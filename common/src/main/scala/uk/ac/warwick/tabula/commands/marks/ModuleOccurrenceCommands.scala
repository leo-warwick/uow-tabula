package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.{Describable, Description, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.marks.ValidGradesForMark
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, ModuleRegistrationMarksServiceComponent, ResitServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipServiceComponent, ModuleRegistrationServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

import scala.jdk.CollectionConverters._

// Traits for commands that act on a specific run of a module - extend as required

trait ModuleOccurrenceState {
  def sitsModuleCode: String
  def module: Module
  def academicYear: AcademicYear
  def occurrence: String
}


trait ModuleOccurrenceLoadModuleRegistrations extends RetrieveComponentMarks {
  self: ModuleOccurrenceState
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with ResitServiceComponent
    with RetrieveComponentMarks =>

  lazy val assessmentComponents: Seq[AssessmentComponent] =
    assessmentMembershipService.getAssessmentComponents(sitsModuleCode, inUseOnly = false)
      .filter { ac =>
        ac.sequence != AssessmentComponent.NoneAssessmentGroup
      }

  lazy val upstreamAssessmentGroupInfos: Seq[UpstreamAssessmentGroupInfo] =
    assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, academicYear)

  lazy val studentComponentMarkRecords: Seq[(AssessmentComponent, Seq[StudentMarkRecord])] =
    upstreamAssessmentGroupInfos
      .filter { info =>
        info.upstreamAssessmentGroup.occurrence == occurrence &&
        info.allMembers.nonEmpty
      }
      .map { info =>
        info.upstreamAssessmentGroup.assessmentComponent.get ->
          ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService, resitService, assessmentMembershipService)
      }

  lazy val moduleRegistrations: Seq[ModuleRegistration] = moduleRegistrationService.getByModuleOccurrence(sitsModuleCode, academicYear, occurrence)

  lazy val studentModuleMarkRecords: Seq[StudentModuleMarkRecord] =
    MarksDepartmentHomeCommand.studentModuleMarkRecords(sitsModuleCode, academicYear, occurrence, moduleRegistrations, moduleRegistrationMarksService, assessmentMembershipService)
}

trait ModuleOccurrenceMarksRequest[A <: ModuleOccurrenceCommands.StudentModuleMarksItem] {
  var students: JMap[ModuleOccurrenceCommands.SprCode, A]
}

trait ModuleOccurrenceValidGradesBindListener {
  self: ModuleOccurrenceMarksRequest[_ <: ModuleOccurrenceCommands.StudentModuleMarksItem]
    with ModuleOccurrenceLoadModuleRegistrations
    with AssessmentMembershipServiceComponent =>

  def onBindValidGrades(result: BindingResult): Unit = {
    students.asScala.foreach { case (_, item) =>
      moduleRegistrations.find(_.sprCode == item.sprCode).foreach { moduleRegistration =>
        val request = new ValidModuleRegistrationGradesRequest
        request.mark = item.mark
        request.existing = item.grade
        item.validGrades = ValidGradesForMark.getTuple(
          request,
          moduleRegistration
        )(assessmentMembershipService = assessmentMembershipService)
      }
      // we don't care if it's not a valid module reg, it'll be caught in the validation step after the bind listeners have run
    }
  }
}

trait ModuleOccurrenceUpdateMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ModuleOccurrenceState with ModuleOccurrenceLoadModuleRegistrations =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Feedback.Publish, mandatory(module))

    // Make sure sitsModuleCode is for the same module
    if (mandatory(Module.stripCats(mandatory(sitsModuleCode))).toLowerCase != mandatory(module.code)) {
      logger.info("Not displaying module as it doesn't match SITS module code")
      throw new ItemNotFoundException(module, "Not displaying module as it doesn't match SITS module code")
    }

    // Make sure that the module occurrence actually exists
    mandatory(academicYear)
    mandatory(occurrence)
    mandatory(moduleRegistrations.headOption)
  }
}

object ModuleOccurrenceCommands {
  type SprCode = String

  abstract class StudentModuleMarksItem {
    var sprCode: SprCode = _
    var mark: String = _
    var grade: String = _
    var validGrades: (Seq[GradeBoundary], Option[GradeBoundary]) = _
    var result: String = _
    var comments: String = _

    def populate(student: StudentModuleMarkRecord)(assessmentMembershipService: AssessmentMembershipService): Unit = {
      student.mark.foreach(m => mark = m.toString)
      student.grade.foreach(grade = _)
      student.result.foreach(r => result = r.dbValue)

      val request = new ValidModuleRegistrationGradesRequest
      request.mark = student.mark.map(_.toString).getOrElse("")
      request.existing = student.grade.orNull
      validGrades = ValidGradesForMark.getTuple(request, student.moduleRegistration)(assessmentMembershipService = assessmentMembershipService)
    }
  }

  trait OptionalMarksItem {
    self: StudentModuleMarksItem =>

    var process: Boolean = true

    def shouldProcess(student: StudentModuleMarkRecord): Unit = {
      if (student.grade.isEmpty || student.result.isEmpty || student.agreed || student.markState.contains(MarkState.Agreed)) {
        process = false
      }
    }
  }

}


trait ValidatesModuleMark {

  self: AssessmentMembershipServiceComponent with SecurityServiceComponent =>

  def validateModuleMark(errors: Errors)(
    item: ModuleOccurrenceCommands.StudentModuleMarksItem,
    moduleRegistration: Option[ModuleRegistration],
    studentModuleMarkRecord: StudentModuleMarkRecord,
    department: Department,
    canEditAgreedMarks: Boolean,
    doGradeValidation: Boolean,
  ): Unit = {

    val sprCode = item.sprCode

    // Don't worry about validating marks/grades if there's been no change
    val isUnchanged = moduleRegistration.exists { modReg =>

      !studentModuleMarkRecord.outOfSync &&
        !item.comments.hasText &&
        ((!item.mark.hasText && studentModuleMarkRecord.mark.isEmpty) || studentModuleMarkRecord.mark.map(_.toString).contains(item.mark)) &&
        ((!item.grade.hasText && studentModuleMarkRecord.grade.isEmpty) || studentModuleMarkRecord.grade.contains(item.grade)) &&
        ((!item.result.hasText && studentModuleMarkRecord.result.isEmpty) || studentModuleMarkRecord.result.map(_.dbValue).contains(item.result))
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
            val validGrades = moduleRegistration.map(modReg => assessmentMembershipService.gradesForMark(modReg, Some(asInt))).getOrElse(Seq.empty)
            if (item.grade.hasText) {
              if (!validGrades.exists(_.grade == item.grade)) {
                errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
              } else {
                validGrades.find(_.grade == item.grade).foreach { gb =>
                  if (!item.result.hasText) {
                    item.result = gb.result.map(_.dbValue).orNull
                  } else if (gb.result.exists(_.dbValue != item.result)) {
                    errors.rejectValue("result", "result.invalidSITS", Array(gb.result.get.description), "")
                  }
                }
              }
            } else if (asInt != 0 || department.assignmentGradeValidationUseDefaultForZero) {
              // This is a bit naughty, validation shouldn't modify state, but it's clearer in the preview if we show what the grade will be
              validGrades.find(_.isDefault).foreach { gb =>
                item.grade = gb.grade

                if (!item.result.hasText) {
                  item.result = gb.result.map(_.dbValue).orNull
                } else if (gb.result.exists(_.dbValue != item.result)) {
                  errors.rejectValue("result", "result.invalidSITS", Array(gb.result.get.description), "")
                }
              }
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
        val validGrades = moduleRegistration.map(modReg => assessmentMembershipService.gradesForMark(modReg, None)).getOrElse(Seq.empty)
        if (!validGrades.exists(_.grade == item.grade)) {
          errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
        } else {
          validGrades.find(_.grade == item.grade).foreach { gb =>
            if (!item.result.hasText) {
              item.result = gb.result.map(_.dbValue).orNull
            } else if (gb.result.exists(_.dbValue != item.result)) {
              errors.rejectValue("result", "result.invalidSITS", Array(gb.result.get.description), "")
            }
          }
        }
      }

      if (item.grade.safeLength > 2) {
        errors.rejectValue("grade", "actualGrade.tooLong")
      }
    }

    moduleRegistration.foreach { modReg =>
      val isAgreed = studentModuleMarkRecord.agreed || studentModuleMarkRecord.markState.contains(MarkState.Agreed)
      val resultsReleasedToStudents = MarkState.resultsReleasedToStudents(modReg, MarkState.MarkUploadTime)
      if (isAgreed && !isUnchanged && resultsReleasedToStudents && !canEditAgreedMarks) {
        errors.rejectValue("mark", "actualMark.module.agreed")
      }
    }
  }
}

trait ModuleOccurrenceValidation extends ValidatesModuleMark {
  self: SelfValidating
    with ModuleOccurrenceState
    with ClearRecordedModuleMarksState
    with ModuleOccurrenceLoadModuleRegistrations
    with AssessmentMembershipServiceComponent
    with SecurityServiceComponent =>

  lazy val canEditAgreedMarks: Boolean =
    securityService.can(currentUser, Permissions.Marks.OverwriteAgreedMarks, module)

  def validateMarkEntry(errors: Errors)(item: ModuleOccurrenceCommands.StudentModuleMarksItem, doGradeValidation: Boolean): Unit = {
    val sprCode = item.sprCode

    // Check that there's a module registration for the student
    val moduleRegistration = moduleRegistrations.find(_.sprCode == sprCode)

    // We allow returning marks for PWD students so we don't need to filter by "current" members here
    if (moduleRegistration.isEmpty) {
      errors.reject("uniNumber.notOnModule", Array(sprCode), "")
    }
    val studentModuleMarkRecord = moduleRegistration.flatMap(mr => studentModuleMarkRecords.find(_.sprCode == mr.sprCode)).get
    validateModuleMark(errors)(item, moduleRegistration, studentModuleMarkRecord, module.adminDepartment, canEditAgreedMarks, doGradeValidation)
  }
}

trait ModuleOccurrenceDescription[A] extends Describable[A] {
  self: ModuleOccurrenceState =>

  def mandatoryEventName: String

  override lazy val eventName: String = mandatoryEventName

  override def describe(d: Description): Unit =
    d.module(module)
      .properties(
        "sitsModuleCode" -> sitsModuleCode,
        "academicYear" -> academicYear.toString,
        "occurrence" -> occurrence,
      )
}

trait RecordedModuleRegistrationsDescription extends ModuleOccurrenceDescription[Seq[RecordedModuleRegistration]] {
  self: ModuleOccurrenceState =>

  override def describeResult(d: Description, result: Seq[RecordedModuleRegistration]): Unit =
    d.properties(
      "marks" -> result.filter(_.latestMark.nonEmpty).map { student =>
        student.sprCode -> student.latestMark.get
      }.toMap,
      "grades" -> result.filter(_.latestGrade.nonEmpty).map { student =>
        student.sprCode -> student.latestGrade.get
      }.toMap,
      "results" -> result.filter(_.latestResult.nonEmpty).map { student =>
        student.sprCode -> student.latestResult.get.entryName
      }.toMap,
      "state" -> result.filter(_.latestState.nonEmpty).map { student =>
        student.sprCode -> student.latestState.get.entryName
      }.toMap
    )
}
