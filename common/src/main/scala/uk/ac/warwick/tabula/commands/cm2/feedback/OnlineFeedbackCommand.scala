package uk.ac.warwick.tabula.commands.cm2.feedback

import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports.{JList, JMap, _}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{Extension, FormValue, SavedFormValue, StringFormValue}
import uk.ac.warwick.tabula.data.{AutowiringSavedFormValueDaoComponent, SavedFormValueDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._
import scala.util.Try

object OnlineFeedbackCommand {
  def apply(assignment: Assignment, student: User, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks) =
    new OnlineFeedbackCommandInternal(assignment, student, submitter, gradeGenerator)
      with ComposableCommand[Feedback]
      with OnlineFeedbackPermissions
      with OnlineFeedbackDescription[Feedback]
      with OnlineFeedbackValidation
      with OnlineFeedbackBindListener
      with AutowiringProfileServiceComponent
      with AutowiringFileAttachmentServiceComponent
      with AutowiringSavedFormValueDaoComponent
      with AutowiringFeedbackServiceComponent
      with AutowiringZipServiceComponent {
      override lazy val eventName = "OnlineFeedback"
    }
}

class OnlineFeedbackCommandInternal(val assignment: Assignment, val student: User, val submitter: CurrentUser, val gradeGenerator: GeneratesGradesFromMarks)
  extends CommandInternal[Feedback] with OnlineFeedbackState with CopyFromFormFields with WriteToFormFields {

  self: ProfileServiceComponent with FileAttachmentServiceComponent with SavedFormValueDaoComponent with FeedbackServiceComponent with ZipServiceComponent =>

  feedback match {
    case Some(f) => copyFrom(f)
    case None =>
      fields = {
        val pairs = assignment.feedbackFields.map(field => field.id -> field.blankFormValue)
        Map(pairs: _*).asJava
      }
  }

  def applyInternal(): Feedback = {

    val updatedFeedback: Feedback = feedback.getOrElse({
      val newFeedback = new Feedback
      newFeedback.assignment = assignment
      newFeedback.uploaderId = submitter.apparentUser.getUserId
      newFeedback.usercode = student.getUserId
      newFeedback._universityId = student.getWarwickId
      newFeedback.released = false
      newFeedback.createdDate = DateTime.now
      newFeedback
    })

    copyTo(updatedFeedback)
    updatedFeedback.updatedDate = DateTime.now
    feedbackService.saveOrUpdate(updatedFeedback)

    // if we are updating existing feedback then invalidate any cached feedback zips
    if (updatedFeedback.id != null) {
      zipService.invalidateIndividualFeedbackZip(updatedFeedback)
    }

    updatedFeedback
  }

  private def copyFrom(feedback: Feedback): Unit = {

    copyFormFields(feedback)

    // mark and grade
    if (assignment.collectMarks) {
      mark = feedback.actualMark.map(_.toString).getOrElse("")
      grade = feedback.actualGrade.getOrElse("")
    }

    // get attachments
    attachedFiles = JArrayList[FileAttachment](feedback.attachments.asScala)
  }

  private def copyTo(feedback: Feedback): Unit = {

    saveFormFields(feedback)

    // save mark and grade
    if (assignment.collectMarks) {
      feedback.actualMark = mark.maybeText.map(_.toInt)
      feedback.actualGrade = grade.maybeText
    }

    // save attachments
    if (feedback.attachments != null) {
      val filesToKeep = Option(attachedFiles).getOrElse(JList()).asScala.toSeq
      val existingFiles = Option(feedback.attachments).getOrElse(JHashSet()).asScala.toSeq
      val filesToRemove = existingFiles diff filesToKeep
      val filesToReplicate = filesToKeep diff existingFiles
      fileAttachmentService.deleteAttachments(filesToRemove)
      feedback.attachments = JHashSet[FileAttachment](filesToKeep: _*)
      val replicatedFiles = filesToReplicate.map(_.duplicate())
      replicatedFiles.foreach(feedback.addAttachment)
    }
    feedback.addAttachments(file.attached.asScala.toSeq)
  }
}

trait OnlineFeedbackBindListener extends BindListener {
  self: OnlineFeedbackState =>

  override def onBind(result: BindingResult): Unit = {
    if (fields != null) {
      for ((key, field) <- fields.asScala) {
        result.pushNestedPath(s"fields[$key]")
        field.onBind(result)
        result.popNestedPath()
      }
    }

    result.pushNestedPath("file")
    file.onBind(result)
    result.popNestedPath()
  }
}

trait OnlineFeedbackValidation extends SelfValidating {
  self: OnlineFeedbackState =>

  override def validate(errors: Errors): Unit = {
    fieldValidation(errors)
  }

  private def fieldValidation(errors: Errors): Unit = {
    // Individually validate all the custom fields
    if (fields != null) {
      assignment.feedbackFields.foreach { field =>
        errors.pushNestedPath("fields[%s]".format(field.id))
        fields.asScala.get(field.id).foreach(field.validate(_, errors))
        errors.popNestedPath()
      }
    }

    if (mark.hasText) {
      try {
        val asInt = mark.toInt
        if (asInt < 0 || asInt > 100) {
          errors.rejectValue("mark", "actualMark.range")
        }
        if (assignment.useMarkPoints && MarkPoint.forMark(asInt).isEmpty) {
          errors.rejectValue("mark", "actualMark.markPoint")
        }
      } catch {
        case _@(_: NumberFormatException | _: IllegalArgumentException) =>
          errors.rejectValue("mark", "actualMark.format")
      }
    }

    // validate grade is department setting is true
    if (!errors.hasErrors && grade.hasText && assignment.module.adminDepartment.assignmentGradeValidation) {
      val validGrades = Try(mark.toInt).toOption.toSeq.flatMap { m => gradeGenerator.applyForMarks(Map(student.getWarwickId -> m))(student.getWarwickId) }
      if (validGrades.nonEmpty && !validGrades.exists(_.grade == grade)) {
        errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
      }
    }
  }
}

trait CopyFromFormFields {

  self: OnlineFeedbackState with SavedFormValueDaoComponent =>

  def copyFormFields(feedback: Feedback): Unit = {
    // get custom field values
    fields = {
      val pairs = assignment.feedbackFields.map { field =>
        val currentValue = feedback.customFormValues.asScala.find(_.name == field.name)
        val formValue = currentValue match {
          case Some(initialValue) => field.populatedFormValue(initialValue)
          case None => field.blankFormValue
        }
        field.id -> formValue
      }
      Map(pairs: _*).asJava
    }
  }

}

trait WriteToFormFields {

  self: OnlineFeedbackState with SavedFormValueDaoComponent =>

  def saveFormFields(feedback: Feedback): Unit = {
    // save custom fields
    feedback.clearCustomFormValues()
    feedback.customFormValues.addAll(
      fields.asScala.map { case (_, formValue) =>
        def newValue = {
          val newValue = new SavedFormValue()
          newValue.name = formValue.field.name
          newValue.feedback = feedback
          newValue
        }

        // Don't send brand new feedback to the DAO or we'll get a TransientObjectException
        val savedFormValue = if (feedback.id == null) {
          newValue
        } else {
          savedFormValueDao.get(formValue.field, feedback).getOrElse(newValue)
        }

        formValue.persist(savedFormValue)
        savedFormValue
      }.toSet[SavedFormValue].asJava
    )
  }

}

trait OnlineFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: OnlineFeedbackState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Feedback.Manage, assignment)
  }
}

trait OnlineFeedbackDescription[A] extends Describable[A] {
  self: OnlineFeedbackState =>

  def describe(d: Description): Unit = {
    d.studentIds(Option(student.getWarwickId).toSeq)
    d.studentUsercodes(student.getUserId)
    d.assignment(assignment)
  }

  override def describeResult(d: Description): Unit = {
    d.fileAttachments(file.attached.asScala.toSeq)
  }
}

trait OnlineFeedbackState extends SubmissionState with ExtensionState {

  self: ProfileServiceComponent =>

  def assignment: Assignment

  def student: User

  def gradeGenerator: GeneratesGradesFromMarks

  def submitter: CurrentUser

  val feedback: Option[Feedback] = assignment.allFeedback.find(_.usercode == student.getUserId)
  val submission: Option[Submission] = assignment.submissions.asScala.find(_.usercode == student.getUserId)
  val extension: Option[Extension] = assignment.requestedOrApprovedExtensions.get(student.getUserId)

  var mark: String = _
  var grade: String = _
  var fields: JMap[String, FormValue] = _
  var file: UploadedFile = new UploadedFile
  var attachedFiles: JList[FileAttachment] = _

  var approved: Boolean = false

  private def fieldHasValue = Try(fields.asScala.exists { case (_, value: StringFormValue) => value.value.hasText }).toOption.getOrElse(false)

  private def hasFile = Option(attachedFiles).exists(!_.isEmpty) || Option(file).exists(!_.attachedOrEmpty.isEmpty)

  def hasContent: Boolean = mark.hasText || grade.hasText || hasFile || fieldHasValue
}

trait SubmissionState {

  self: ProfileServiceComponent =>

  def assignment: Assignment

  def submission: Option[Submission]

  def student: User

  def submissionState: String = {
    submission match {
      case Some(s) if s.isAuthorisedLate => "workflow.Submission.authorisedLate"
      case Some(s) if s.isLate => "workflow.Submission.late"
      case Some(_) => "workflow.Submission.onTime"
      case None if !assignment.isClosed => "workflow.Submission.unsubmitted.withinDeadline"
      case None if assignment.approvedExtensions.get(student.getUserId).exists(_.expiryDate.exists(_.isBeforeNow)) =>
        "workflow.Submission.unsubmitted.withinExtension"
      case None => "workflow.Submission.unsubmitted.late"
    }
  }

  def disability: Option[Disability] = submission.filter(_.useDisability).flatMap(_ =>
    profileService
      .getMemberByUniversityId(student.getWarwickId)
      .collect { case s: StudentMember => s }
      .flatMap(_.disability)
  )

  def reasonableAdjustmentsDeclared: Option[Boolean] = submission.flatMap(_.reasonableAdjustmentsDeclared)
}

trait ExtensionState {
  def assignment: Assignment

  def extension: Option[Extension]

  def extensionState: String = extension match {
    case Some(e) if e.rejected || e.revoked => "workflow.Extension.requestDenied"
    case Some(e) if e.approved => "workflow.Extension.granted"
    case Some(e) if !e.isManual => "workflow.Extension.requested"
    case _ => "workflow.Extension.none"
  }

  def extensionDate: Option[DateTime] = extension.flatMap(e => e.expiryDate.orElse(e.requestedExpiryDate))
}
