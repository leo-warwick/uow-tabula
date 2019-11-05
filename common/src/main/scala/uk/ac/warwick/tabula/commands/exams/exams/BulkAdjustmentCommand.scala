package uk.ac.warwick.tabula.commands.exams.exams

import org.springframework.validation.{BindException, BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.feedback.AssignmentFeedbackAdjustmentCommand
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkType.{Adjustment, PrivateAdjustment}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.cm2.{Cm2FeedbackAdjustmentNotification, Cm2StudentFeedbackAdjustmentNotification}
import uk.ac.warwick.tabula.helpers.SpreadsheetHelpers
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringFeedbackServiceComponent, AutowiringUserLookupComponent, FeedbackServiceComponent, GeneratesGradesFromMarks}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object BulkAdjustmentCommand {
  val StudentIdHeader = "Student ID"
  val MarkHeader = "Adjusted mark"
  val GradeHeader = "Adjusted grade"
  val ReasonHeader = "Reason"
  val CommentsHeader = "Comments"

  def apply(assessment: Assessment, gradeGenerator: GeneratesGradesFromMarks, spreadsheetHelper: SpreadsheetHelpers, user: CurrentUser) =
    new BulkAdjustmentCommandInternal(assessment, gradeGenerator, spreadsheetHelper, user)
      with AutowiringFeedbackServiceComponent
      with ComposableCommand[Seq[Feedback]]
      with BulkAdjustmentCommandBindListener
      with BulkAdjustmentValidation
      with BulkAdjustmentDescription
      with BulkAdjustmentPermissions
      with BulkAdjustmentCommandState
      with BulkAdjustmentNotifier
}


class BulkAdjustmentCommandInternal(val assessment: Assessment, val gradeGenerator: GeneratesGradesFromMarks, val spreadsheetHelper: SpreadsheetHelpers, val user: CurrentUser)
  extends CommandInternal[Seq[Feedback]] {

  self: BulkAdjustmentCommandState with FeedbackServiceComponent with BulkAdjustmentValidation =>

  override def applyInternal(): Seq[Feedback] = {
    val errors = new BindException(this, "command")
    validate(errors)

    students.asScala.toSeq
      .filter(usercode =>
        !errors.hasFieldErrors(s"marks[$usercode]") &&
          !errors.hasFieldErrors(s"grades[$usercode]") &&
          !errors.hasFieldErrors(s"reasons[$usercode]")
      )
      .map(usercode => {
        val feedback = feedbackMap(usercode)
        val mark = feedback.addMark(
          user.apparentUser.getUserId,
          privateAdjustment match {
            case true => PrivateAdjustment
            case false => Adjustment
          },
          marks.asScala(usercode).toInt,
          grades.asScala.get(usercode),
          reasons.asScala.get(usercode) match {
            case Some(reason) if reason.hasText => reason
            case _ => defaultReason
          },
          comments.asScala.get(usercode) match {
            case Some(comment) if comment.hasText => comment
            case _ => defaultComment
          }
        )
        feedbackService.saveOrUpdate(mark)
        feedbackService.saveOrUpdate(feedback)
        feedback
      })
  }

}

trait BulkAdjustmentCommandBindListener extends BindListener {

  self: BulkAdjustmentCommandState =>

  override def onBind(result: BindingResult): Unit = {
    // parse file
    validateUploadedFile(result)
    if (!result.hasErrors) {
      transactional() {
        result.pushNestedPath("file")
        file.onBind(result)
        result.popNestedPath()

        if (!file.attached.isEmpty) {
          extractDataFromFile(file.attached.asScala.head, result)
        }
      }
    }
  }

  private def validateUploadedFile(result: BindingResult) {
    val fileNames = file.fileNames.map(_.toLowerCase)
    val invalidFiles = fileNames.filter(s => !s.endsWith(".xlsx"))

    if (invalidFiles.nonEmpty) {
      if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), ".xlsx"), "")
      else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", "), ".xlsx"), "")
    }
  }

  private def extractDataFromFile(file: FileAttachment, result: BindingResult) = {
    val rowData = spreadsheetHelper.parseXSSFExcelFile(file.asByteSource.openStream())

    val (rowsToValidate, badRows) = rowData.partition(row => {
      row.data.get(BulkAdjustmentCommand.StudentIdHeader.toLowerCase) match {
        case Some(studentId) if feedbackMap.get(studentId).isDefined => true
        case _ => false
      }
    })

    ignoredRows = badRows.map(_.data)

    rowsToValidate.foreach(row => {
      val data = row.data
      val studentId = data(BulkAdjustmentCommand.StudentIdHeader.toLowerCase)
      students.add(studentId)
      marks.put(studentId, data.get(BulkAdjustmentCommand.MarkHeader.toLowerCase).orNull)
      grades.put(studentId, data.get(BulkAdjustmentCommand.GradeHeader.toLowerCase).orNull)
      reasons.put(studentId, data.get(BulkAdjustmentCommand.ReasonHeader.toLowerCase).orNull)
      comments.put(studentId, data.get(BulkAdjustmentCommand.CommentsHeader.toLowerCase).orNull)
    })
  }

}

trait BulkAdjustmentValidation extends SelfValidating {

  self: BulkAdjustmentCommandState =>

  override def validate(errors: Errors) {
    val doGradeValidation = assessment.module.adminDepartment.assignmentGradeValidation
    students.asScala.foreach(id => {
      marks.asScala.get(id) match {
        case Some(mark) if mark.hasText =>
          try {
            val asInt = mark.toInt
            if (asInt < 0 || asInt > 100) {
              errors.rejectValue(s"marks[$id]", "actualMark.range")
            } else if (doGradeValidation && grades.asScala.getOrElse(id, null).hasText) {
              val validGrades = gradeGenerator.applyForMarks(Map(id -> asInt))(id)
              if (validGrades.nonEmpty && !validGrades.exists(_.grade == grades.asScala(id))) {
                errors.rejectValue(s"grades[$id]", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
              }
            }
          } catch {
            case _@(_: NumberFormatException | _: IllegalArgumentException) =>
              errors.rejectValue(s"marks[$id]", "actualMark.format")
          }
        case _ =>
          errors.rejectValue(s"marks[$id]", "actualMark.range")
      }
      reasons.asScala.get(id) match {
        case Some(reason) if reason.hasText && reason.length > AssignmentFeedbackAdjustmentCommand.REASON_SIZE_LIMIT =>
          errors.rejectValue(s"reasons[$id]", "feedback.adjustment.reason.tooBig")
        case _ =>
      }
    })

    if (confirmStep) {
      if (requiresDefaultReason) {
        if (defaultReason.hasText && defaultReason.length > AssignmentFeedbackAdjustmentCommand.REASON_SIZE_LIMIT) {
          errors.rejectValue("defaultReason", "feedback.adjustment.reason.tooBig")
        }
        if (!defaultReason.hasText) {
          errors.rejectValue("defaultReason", "feedback.adjustment.reason.empty.bulk")
        }
      }
      if (requiresDefaultComments && !defaultComment.hasText) {
        errors.rejectValue("defaultComment", "feedback.adjustment.comments.empty.bulk")
      }
    }
  }


}

trait BulkAdjustmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: BulkAdjustmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    HibernateHelpers.initialiseAndUnproxy(mandatory(assessment)) match {
      case assignment: Assignment =>
        p.PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
      case exam: Exam =>
        p.PermissionCheck(Permissions.ExamFeedback.Manage, exam)
    }
  }

}

trait BulkAdjustmentDescription extends Describable[Seq[Feedback]] {

  self: BulkAdjustmentCommandState =>

  override lazy val eventName = "BulkAdjustment"

  override def describe(d: Description) {
    d.assessment(assessment)
    d.property("marks" -> marks.asScala.filter { case (_, mark) => mark.hasText })
  }
}

trait BulkAdjustmentCommandState {

  def assessment: Assessment

  def gradeGenerator: GeneratesGradesFromMarks

  def spreadsheetHelper: SpreadsheetHelpers

  def user: CurrentUser

  lazy val feedbackMap: Map[String, Feedback] = assessment.allFeedback.groupBy(_.studentIdentifier).view.mapValues(_.head).toMap

  // Bind variables
  var file: UploadedFile = new UploadedFile

  var students: JList[String] = JArrayList()
  var marks: JMap[String, String] = JHashMap()
  var grades: JMap[String, String] = JHashMap()
  var reasons: JMap[String, String] = JHashMap()
  var comments: JMap[String, String] = JHashMap()

  var privateAdjustment = true
  var defaultReason: String = _
  var defaultComment: String = _

  var ignoredRows: Seq[Map[String, String]] = Seq()

  var confirmStep = false

  lazy val requiresDefaultReason: Boolean = !students.asScala.forall(id => reasons.asScala.getOrElse(id, null).hasText)
  lazy val requiresDefaultComments: Boolean = !students.asScala.forall(id => comments.asScala.getOrElse(id, null).hasText)

}

trait BulkAdjustmentNotifier extends Notifies[Seq[Feedback], Seq[Feedback]] {
  self: BulkAdjustmentCommandState =>

  def emit(feedbacks: Seq[Feedback]): Seq[NotificationWithTarget[AssignmentFeedback, Assignment] with SingleItemNotification[AssignmentFeedback] with AutowiringUserLookupComponent] = {
    if (privateAdjustment) {
      Nil
    } else {
      feedbacks.flatMap { feedback =>
        HibernateHelpers.initialiseAndUnproxy(feedback) match {
          case assignmentFeedback: AssignmentFeedback =>
            val studentsNotifications = if (assignmentFeedback.released) {
              Seq(Notification.init(new Cm2StudentFeedbackAdjustmentNotification, user.apparentUser, assignmentFeedback, assignmentFeedback.assignment))
            } else {
              Nil
            }
            val adminsNotifications = if (assessment.hasWorkflow) {
              Seq(Notification.init(new Cm2FeedbackAdjustmentNotification, user.apparentUser, assignmentFeedback, assignmentFeedback.assignment))
            } else {
              Nil
            }
            studentsNotifications ++ adminsNotifications
          case x => Nil
        }
      }
    }
  }
}
