package uk.ac.warwick.tabula.commands.marks

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.apache.poi.xssf.usermodel.XSSFComment
import org.springframework.validation.{BindingResult, Errors}
import org.xml.sax.InputSource
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.RecordAssessmentComponentMarksCommand._
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.marks.{AssessmentComponentValidGradesForMarkRequest, ValidGradesForMark}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.coursework.docconversion.AbstractXslxSheetHandler
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._
import scala.util.{Try, Using}

object RecordAssessmentComponentMarksCommand {
  type UniversityID = String
  class StudentMarksItem {
    def this(universityID: UniversityID) = {
      this()
      this.universityID = universityID
    }

    var universityID: UniversityID = _
    var resitSequence: String = _
    var mark: String = _ // Easier as a String to treat empty strings correctly
    var grade: String = _
    var validGrades: (Seq[GradeBoundary], Option[GradeBoundary]) = _
    var comments: String = _
  }

  type Result = Seq[RecordedAssessmentComponentStudent]
  type Command = Appliable[Result]
    with RecordAssessmentComponentMarksRequest
    with SelfValidating
    with BindListener
    with PopulateOnForm

  val AdminPermission: Permission = Permissions.Feedback.Publish
  val OverwriteAgreedMarksPermission: Permission = Permissions.Marks.OverwriteAgreedMarks

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new RecordAssessmentComponentMarksCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with RecordAssessmentComponentMarksRequest
      with RecordAssessmentComponentMarksValidation
      with RecordAssessmentComponentMarksPermissions
      with RecordAssessmentComponentMarksDescription
      with RecordAssessmentComponentMarksBindListener
      with RecordAssessmentComponentMarksValidGradesBindListener
      with RecordAssessmentComponentMarksSpreadsheetBindListener
      with RecordAssessmentComponentMarksPopulateOnForm
      with ClearRecordedModuleMarks
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringTransactionalComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringSecurityServiceComponent
}

abstract class RecordAssessmentComponentMarksCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState
    with ClearRecordedModuleMarksState {
  self: RecordAssessmentComponentMarksRequest
    with AssessmentComponentMarksServiceComponent
    with TransactionalComponent
    with ClearRecordedModuleMarks =>

  override def applyInternal(): Result = transactional() {
    students.asScala.values.toSeq
      .map { item =>
        val upstreamAssessmentGroupMember =
          upstreamAssessmentGroup.members.asScala
            .find { m =>
              m.universityId == item.universityID && (
                (item.resitSequence.maybeText.isEmpty && m.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment) ||
                (m.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.Reassessment && m.resitSequence.contains(item.resitSequence))
              )
            }.get // We validate that this exists

        val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent =
          assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember)

        recordedAssessmentComponentStudent.addMark(
          uploader = currentUser.apparentUser,
          mark = item.mark.maybeText.map(_.toInt),
          grade = item.grade.maybeText,
          source = RecordedAssessmentComponentStudentMarkSource.MarkEntry,
          markState = recordedAssessmentComponentStudent.latestState.getOrElse(UnconfirmedActual),
          comments = item.comments,
        )

        assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

        clearRecordedModuleMarksFor(recordedAssessmentComponentStudent)

        recordedAssessmentComponentStudent
      }
  }
}

trait RecordAssessmentComponentMarksState {
  def assessmentComponent: AssessmentComponent
  def upstreamAssessmentGroup: UpstreamAssessmentGroup
}

trait RecordAssessmentComponentMarksRequest {
  var students: JMap[String, StudentMarksItem] =
    LazyMaps.create { id: String =>
      id.split("_", 2) match {
        case Array(universityID, resitSequence) =>
          val item = new StudentMarksItem(universityID)
          item.resitSequence = resitSequence
          item

        case Array(universityID) => new StudentMarksItem(universityID)
      }
    }.asJava

  // For uploading a spreadsheet
  var file: UploadedFile = new UploadedFile
}

trait RecordAssessmentComponentMarksBindListener extends BindListener {
  self: RecordAssessmentComponentMarksValidGradesBindListener
    with RecordAssessmentComponentMarksSpreadsheetBindListener
    with RecordAssessmentComponentMarksRequest
    with TransactionalComponent =>

  override def onBind(result: BindingResult): Unit = {
    onBindSpreadsheet(result)
    onBindValidGrades(result)
  }
}

trait RecordAssessmentComponentMarksValidGradesBindListener {
  self: RecordAssessmentComponentMarksRequest
    with RecordAssessmentComponentMarksState
    with AssessmentMembershipServiceComponent =>

  def onBindValidGrades(result: BindingResult): Unit = {
    students.asScala.foreach { case (_, item) =>
      upstreamAssessmentGroup.members.asScala
        .find { m =>
          m.universityId == item.universityID && (
            (item.resitSequence.maybeText.isEmpty && m.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment) ||
              (m.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.Reassessment && m.resitSequence.contains(item.resitSequence))
            )
        }
        .foreach { upstreamAssessmentGroupMember =>
          val request = new ValidAssessmentComponentGradesRequest
          request.mark = item.mark
          request.existing = item.grade
          request.resitAttempt = JInteger(upstreamAssessmentGroupMember.currentResitAttempt)
          item.validGrades = ValidGradesForMark.getTuple(
            request,
            assessmentComponent
          )(assessmentMembershipService = assessmentMembershipService)
        }
        // we don't care if it's not a valid upstream group member, it'll be caught in the validation step after the bind listeners have run
    }
  }
}

class ValidAssessmentComponentGradesRequest
  extends AssessmentComponentValidGradesForMarkRequest

trait RecordAssessmentComponentMarksSpreadsheetBindListener {
  self: RecordAssessmentComponentMarksRequest
    with TransactionalComponent =>

  final val MAX_MARKS_ROWS: Int = 5000
  final val VALID_FILE_TYPES: Seq[String] = Seq(".xlsx")

  def onBindSpreadsheet(result: BindingResult): Unit = {
    val fileNames = file.fileNames map (_.toLowerCase)
    val invalidFiles = fileNames.filter(s => !VALID_FILE_TYPES.exists(s.endsWith))

    if (invalidFiles.nonEmpty) {
      if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), VALID_FILE_TYPES.mkString(", ")), "")
      else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "")
    }

    if (!result.hasErrors) {
      transactional() {
        result.pushNestedPath("file")
        file.onBind(result)
        result.popNestedPath()

        file.attached.asScala.filter(_.hasData).foreach(file => {
          try {
            Using.resource(file.asByteSource.openStream()) { stream =>
              val pkg = OPCPackage.open(stream)
              val sst = new ReadOnlySharedStringsTable(pkg)
              val reader = new XSSFReader(pkg)
              val styles = reader.getStylesTable

              val items: JList[StudentMarksItem] = JArrayList()
              val sheetHandler = new AbstractXslxSheetHandler(styles, sst, items) {
                override def newCurrentItem: StudentMarksItem = new StudentMarksItem()
                override def cell(cellReference: String, formattedValue: String, comment: XSSFComment): Unit = {
                  val col = new CellReference(cellReference).getCol
                  if (isFirstRow) {
                    columnMap(col) = formattedValue
                  } else if (columnMap.asJava.containsKey(col) && formattedValue.hasText) {
                    columnMap(col) match {
                      case "University ID" | "ID" =>
                        currentItem.universityID = formattedValue
                      case "Resit sequence" =>
                        currentItem.resitSequence = formattedValue.maybeText.map(v => Try(v.toInt).toOption.map("%03d".format(_)).getOrElse(v)).getOrElse("")
                      case "Mark" =>
                        currentItem.mark = formattedValue
                      case "Grade" =>
                        currentItem.grade = formattedValue
                      case "Comments" =>
                        currentItem.comments = formattedValue
                      case _ => // ignore anything else
                    }
                  }
                }
              }

              val parser = sheetHandler.fetchSheetParser
              reader.getSheetsData.asScala.foreach { is =>
                Using.resource(is)(sheet => parser.parse(new InputSource(sheet)))
              }

              if (items.size() > MAX_MARKS_ROWS) {
                result.rejectValue("file", "file.tooManyRows", Array(MAX_MARKS_ROWS.toString), "")
                items.clear()
              } else {
                items.asScala.filter(_.universityID.hasText).foreach { item =>
                  students.put(s"${item.universityID}_${item.resitSequence.maybeText.getOrElse("")}", item)
                }
              }
            }
          } catch {
            case _: InvalidFormatException => result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "")
          }
        })
      }
    }
  }
}

trait RecordAssessmentComponentMarksPopulateOnForm extends PopulateOnForm {
  self: RecordAssessmentComponentMarksState
    with RecordAssessmentComponentMarksRequest
    with AssessmentComponentMarksServiceComponent
    with ResitServiceComponent
    with AssessmentMembershipServiceComponent =>

  override def populate(): Unit = {
    val info = UpstreamAssessmentGroupInfo(
      upstreamAssessmentGroup,
      assessmentMembershipService.getCurrentUpstreamAssessmentGroupMembers(upstreamAssessmentGroup.id)
    )

    ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService, resitService, assessmentMembershipService).foreach { student =>
      if (!student.outOfSync && (student.mark.nonEmpty || student.grade.nonEmpty) && !students.asScala.keysIterator.contains(student.universityId)) {
        val s = new StudentMarksItem(student.universityId)
        s.resitSequence = student.resitSequence.getOrElse("")
        student.mark.foreach(m => s.mark = m.toString)
        student.grade.foreach(s.grade = _)

        val request = new ValidAssessmentComponentGradesRequest
        request.mark = student.mark.map(_.toString).getOrElse("")
        request.existing = student.grade.orNull
        request.resitAttempt = JInteger(student.upstreamAssessmentGroupMember.currentResitAttempt)
        s.validGrades = ValidGradesForMark.getTuple(request, assessmentComponent)(assessmentMembershipService = assessmentMembershipService)

        students.put(s"${student.universityId}_${student.resitSequence.getOrElse("")}", s)
      }
    }
  }
}

trait RecordAssessmentComponentMarksValidation extends SelfValidating {
  self: RecordAssessmentComponentMarksState
    with ClearRecordedModuleMarksState
    with RecordAssessmentComponentMarksRequest
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with ResitServiceComponent
    with SecurityServiceComponent =>

  lazy val canEditAgreedMarks: Boolean =
    securityService.can(currentUser, OverwriteAgreedMarksPermission, assessmentComponent.module)

  override def validate(errors: Errors): Unit = {
    val info = UpstreamAssessmentGroupInfo(
      upstreamAssessmentGroup,
      assessmentMembershipService.getCurrentUpstreamAssessmentGroupMembers(upstreamAssessmentGroup.id)
    )

    val studentMarkRecords = ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService, resitService, assessmentMembershipService)

    val doGradeValidation = assessmentComponent.module.adminDepartment.assignmentGradeValidation

    val modRegsMap = moduleRegistrationService.getByModuleOccurrence(upstreamAssessmentGroup.moduleCode, upstreamAssessmentGroup.academicYear, upstreamAssessmentGroup.occurrence)
      .map(modReg => modReg.studentCourseDetails.student.universityId -> modReg).toMap

    students.asScala.foreach { case (id, item) =>
      errors.pushNestedPath(s"students[$id]")

      val upstreamAssessmentGroupMember = upstreamAssessmentGroup.members.asScala.find { m =>
        m.universityId == item.universityID && (
          (item.resitSequence.maybeText.isEmpty && m.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment) ||
          (m.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.Reassessment && m.resitSequence.contains(item.resitSequence))
        )
      }

      // We allow returning marks for PWD students so we don't need to filter by "current" members here
      if (upstreamAssessmentGroupMember.isEmpty) {
        errors.reject("uniNumber.notOnAssessment", Array(item.universityID), "")
      }

      // Don't worry about validating marks/grades if there's been no change
      val isUnchanged = upstreamAssessmentGroupMember.exists { uagm =>
        val studentMarkRecord = studentMarkRecords.find(_.upstreamAssessmentGroupMember == uagm).get

        !studentMarkRecord.outOfSync &&
        !item.comments.hasText &&
        ((!item.mark.hasText && studentMarkRecord.mark.isEmpty) || studentMarkRecord.mark.map(_.toString).contains(item.mark)) &&
        ((!item.grade.hasText && studentMarkRecord.grade.isEmpty) || studentMarkRecord.grade.contains(item.grade))
      }

      if (!isUnchanged) {
        if (item.mark.hasText) {
          if (item.grade.maybeText.contains(GradeBoundary.ForceMajeureMissingComponentGrade)) {
            errors.rejectValue("mark", "actualMark.notEmpty.forceMajeure")
          }

          try {
            val asInt = item.mark.toInt
            if (asInt < 0 || asInt > 100) {
              errors.rejectValue("mark", "actualMark.range")
            } else if (doGradeValidation) {
              val validGrades = assessmentMembershipService.gradesForMark(assessmentComponent, Some(asInt), upstreamAssessmentGroupMember.flatMap(_.currentResitAttempt))
              if (item.grade.hasText) {
                if (!validGrades.exists(_.grade == item.grade)) {
                  errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
                }
              } else if (asInt != 0 || assessmentComponent.module.adminDepartment.assignmentGradeValidationUseDefaultForZero) {
                // This is a bit naughty, validation shouldn't modify state, but it's clearer in the preview if we show what the grade will be
                validGrades.find(_.isDefault).foreach(gb => item.grade = gb.grade)
              }

              if (!item.grade.hasText) {
                errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
              }
            }
          } catch {
            case _@(_: NumberFormatException | _: IllegalArgumentException) =>
              errors.rejectValue("mark", "actualMark.format")
          }
        } else if (doGradeValidation && item.grade.hasText) {
          val validGrades = assessmentMembershipService.gradesForMark(assessmentComponent, None, upstreamAssessmentGroupMember.flatMap(_.currentResitAttempt))
          if (!validGrades.exists(_.grade == item.grade)) {
            errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
          }
        }

        if (item.grade.safeLength > 2) {
          errors.rejectValue("grade", "actualGrade.tooLong")
        }
      }

      upstreamAssessmentGroupMember.foreach { uagm =>
        val studentMarkRecord = studentMarkRecords.find(_.upstreamAssessmentGroupMember == uagm).get

        val isAgreed = studentMarkRecord.agreed || studentMarkRecord.markState.contains(MarkState.Agreed)

        val resultsReleasedToStudents = modRegsMap.get(uagm.universityId).exists(MarkState.resultsReleasedToStudents(_, MarkState.MarkUploadTime))

        if (isAgreed && !isUnchanged && resultsReleasedToStudents && !canEditAgreedMarks) {
          errors.rejectValue("mark", "actualMark.agreed")
        }
      }

      errors.popNestedPath()
    }
  }
}

trait RecordAssessmentComponentMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RecordAssessmentComponentMarksState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(upstreamAssessmentGroup, assessmentComponent)
    p.PermissionCheck(AdminPermission, mandatory(assessmentComponent.module))
  }
}

trait RecordAssessmentComponentMarksDescription extends Describable[Result] {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "RecordAssessmentComponentMarks"

  override def describe(d: Description): Unit =
    d.assessmentComponent(assessmentComponent)
     .upstreamAssessmentGroup(upstreamAssessmentGroup)

  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "marks" -> result.filter(_.latestMark.nonEmpty).map { student =>
        student.universityId -> student.latestMark.get
      }.toMap,
      "grades" -> result.filter(_.latestGrade.nonEmpty).map { student =>
        student.universityId -> student.latestGrade.get
      }.toMap,
      "state" -> result.filter(_.latestState.nonEmpty).map { student =>
        student.universityId -> student.latestState.get.entryName
      }.toMap
    )
}
