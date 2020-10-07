package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, GenerateExamGridSelectCourseCommandRequest}
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.commands.marks.ModuleOccurrenceCommands.OptionalMarksItem
import uk.ac.warwick.tabula.commands.marks.ProcessCohortMarksCommand._
import uk.ac.warwick.tabula.commands.marks.ProcessModuleMarksCommand.SprCode
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.marks.ValidGradesForMark
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.SortedMap
import scala.jdk.CollectionConverters._

object ProcessCohortMarksCommand {
  type Result = Seq[RecordedModuleRegistration]
  type Command = Appliable[Result]
    with ProcessCohortMarksRequest
    with ProcessCohortMarksState
    with SelfValidating
    with ProcessCohortMarksFetchValidGrades
    with PopulateOnForm

  type ModuleCode = String
  type Occurrence = String
  type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest

  class StudentCohortMarksItem extends ModuleOccurrenceCommands.StudentModuleMarksItem with OptionalMarksItem {

    def this(sprCode: SprCode) {
      this()
      this.sprCode = sprCode
    }

    var moduleCode: String = _
    var occurrence: String = _
    var academicYear: AcademicYear = _
  }

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new ProcessCohortMarksCommandInternal(department, academicYear, currentUser)
      with ProcessCohortMarksRequest
      with ProcessCohortMarksValidation
      with ProcessCohortMarksPermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringTransactionalComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[Result]
      with ProcessCohortMarksDescription
      with ProcessCohortMarksFetchValidGrades
      with ProcessCohortMarksPopulateOnForm
      with ConfirmModuleMarkChangedCommandNotification
      with AutowiringProfileServiceComponent
      with RetrieveComponentMarks
}

abstract class ProcessCohortMarksCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ProcessCohortMarksState
    with ClearRecordedModuleMarksState
    with ProcessModuleAndComponentMarks {

  self: ProcessCohortMarksRequest
    with TransactionalComponent
    with ModuleRegistrationMarksServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ResitServiceComponent
    with RetrieveComponentMarks =>

  lazy val allRecordedModuleRegistrations: Map[ModuleCode, Map[SprCode, RecordedModuleRegistration]] = for {
    (moduleCode, s) <- studentModuleMarkRecords
    (occurrence, _) <- s
  } yield {
    moduleCode -> moduleRegistrationMarksService.getAllRecordedModuleRegistrations(moduleCode, academicYear, occurrence)
      .map(rmr => rmr.sprCode -> rmr)
      .toMap
  }

  lazy val allRecordedAssessmentComponentStudents: Map[UpstreamAssessmentGroup, Map[UpstreamAssessmentGroupMember, RecordedAssessmentComponentStudent]] =
    students.asScala.values.flatMap(_.asScala.values).filter(_.process).flatMap { item =>
      val moduleRegistration = moduleRegistrations.find(mr => mr.sprCode == item.sprCode && mr.sitsModuleCode == item.moduleCode).get
      componentMarks(moduleRegistration).map(_._2._1.upstreamAssessmentGroupMember)
    }
      .groupBy(_.upstreamAssessmentGroup)
      .map { case (uag, members) =>
        val recordedStudents = assessmentComponentMarksService.getAllRecordedStudents(uag)
        uag -> members.flatMap { uagm =>
          recordedStudents.find(_.matchesIdentity(uagm)).map(uagm -> _)
        }.toMap
      }

  val existingRecordedModuleRegistration: ModuleRegistration => Option[RecordedModuleRegistration] =
    mr => allRecordedModuleRegistrations.getOrElse(mr.sitsModuleCode, Map()).get(mr.sprCode)

  override def applyInternal(): Result = transactional() {
    students.asScala.values.flatMap(_.asScala.values).filter(_.process).map { item =>
      val moduleRegistration = moduleRegistrations.find(mr => mr.sprCode == item.sprCode && mr.sitsModuleCode == item.moduleCode).get
      process(moduleRegistration, item)
    }.toSeq
  }
}

trait ProcessCohortMarksRequest {
  var students: JMap[SprCode, JMap[ModuleCode, StudentCohortMarksItem]] = LazyMaps.create { sprCode: SprCode =>
    LazyMaps.create { _: ModuleCode =>
      new StudentCohortMarksItem(sprCode)
    }.asJava
  }.asJava
}

trait ProcessCohortMarksPopulateOnForm extends PopulateOnForm {
  self:  ProcessCohortMarksRequest with ProcessCohortMarksState
    with AssessmentMembershipServiceComponent =>

  override def populate(): Unit = {

    for ((sprCode, modules) <- recordsByStudent; (moduleCode, marks) <- modules; markRecord <- marks.values.flatten.headOption) {
      val s = new StudentCohortMarksItem(sprCode)
      s.populate(markRecord)(assessmentMembershipService = assessmentMembershipService)
      s.shouldProcess(markRecord)
      students.get(sprCode).put(moduleCode, s)
    }
  }

}

trait ProcessCohortMarksState {

  self: ModuleRegistrationMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ResitServiceComponent =>

  val currentUser: CurrentUser
  val academicYear: AcademicYear
  val department: Department

  var entities: Seq[ExamGridEntity] = _

  lazy val entitiesBySprCode: SortedMap[SprCode, (ExamGridEntity, Seq[ModuleRegistration])] = SortedMap(entities.flatMap { e =>
    val entityYear = e.validYears.lastOption.map(_._2)
    entityYear.map { ey =>
      ey.studentCourseYearDetails.map(_.studentCourseDetails.sprCode).orNull -> (e, ey.moduleRegistrations)
    }
  }.sortBy(_._1): _*)

  lazy val moduleRegistrations: Seq[ModuleRegistration] = entitiesBySprCode.values.flatMap(_._2).toSeq

  lazy val studentModuleMarkRecords: Map[ModuleCode, Map[Occurrence, Seq[StudentModuleMarkRecord]]] = moduleRegistrations
    .groupBy(_.sitsModuleCode)
    .view.mapValues(_.groupBy(_.occurrence))
    .toMap
    .map { case (moduleCode, occurrences) => moduleCode ->
      occurrences.map { case (occ, mr) => occ ->
        MarksDepartmentHomeCommand.studentModuleMarkRecords(moduleCode, academicYear, occ, mr, moduleRegistrationMarksService, assessmentMembershipService)
      }
    }

  lazy val recordsByStudent: Map[SprCode, Map[ModuleCode, Map[Occurrence, Seq[StudentModuleMarkRecord]]]] =
    studentModuleMarkRecords.values.flatMap(_.values).flatten.toSeq
      .groupBy(_.sprCode)
      .map { case (sprCode, smrs) => sprCode -> smrs.groupBy(_.moduleRegistration.sitsModuleCode)
        .map { case (moduleCode, smrs) => moduleCode -> smrs.groupBy(_.moduleRegistration.occurrence) }
      }

  lazy val assessmentComponents: Seq[AssessmentComponent] =
    assessmentMembershipService.getAssessmentComponents(moduleRegistrations.map(_.sitsModuleCode).toSet, inUseOnly = false)
      .filter(_.sequence != AssessmentComponent.NoneAssessmentGroup)

  lazy val upstreamAssessmentGroupInfos: Seq[UpstreamAssessmentGroupInfo] =
    assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents, academicYear)

  lazy val studentComponentMarkRecords: Seq[(AssessmentComponent, Seq[StudentMarkRecord])] =
    upstreamAssessmentGroupInfos
      .filter { info => info.allMembers.nonEmpty }
      .map { info =>
        info.upstreamAssessmentGroup.assessmentComponent.get ->
          ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService, resitService, assessmentMembershipService)
      }
}

trait ProcessCohortMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ProcessCohortMarksState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Feedback.Publish, mandatory(department))
    mandatory(academicYear)
  }
}

trait ProcessCohortMarksDescription extends Describable[Result] {
  self: ProcessCohortMarksState =>

  override lazy val eventName: String = "ProcessCohortMarks"

  override def describe(d: Description): Unit =
    d.department(department)
      .studentIds(entities.map(_.universityId))
      .properties(
        "academicYear" -> academicYear.toString,
      )
}

trait ProcessCohortMarksFetchValidGrades {

  self: ProcessCohortMarksRequest
    with ProcessCohortMarksState
    with AssessmentMembershipServiceComponent =>

  def fetchValidGrades(): Unit = {

    for ((sprCode, modules) <- students.asScala; (_, item) <- modules.asScala) {
      moduleRegistrations
        .find(mr => mr.sprCode == sprCode && mr.sitsModuleCode == item.moduleCode)
        .foreach { moduleRegistration =>
          val request = new ValidModuleRegistrationGradesRequest
          request.mark = item.mark
          request.existing = item.grade
          item.validGrades = ValidGradesForMark.getTuple(
            request,
            moduleRegistration
          )(assessmentMembershipService = assessmentMembershipService)
        }
    }
  }
}

trait ProcessCohortMarksValidation extends ValidatesModuleMark with SelfValidating {

  self: ClearRecordedModuleMarksState
    with ProcessCohortMarksRequest
    with ProcessCohortMarksState
    with AssessmentMembershipServiceComponent
    with SecurityServiceComponent =>

  override def validate(errors: Errors): Unit = {
    for ((sprCode, modules) <- students.asScala; (moduleCode, item) <- modules.asScala) {
      errors.pushNestedPath(s"students[$sprCode][$moduleCode]")


      lazy val canEditAgreedMarks: Boolean =
        securityService.can(currentUser, Permissions.Marks.OverwriteAgreedMarks, department)

      if (item.process) {

        val moduleRegistration = moduleRegistrations.find(mr => mr.sprCode == item.sprCode && mr.sitsModuleCode == moduleCode)

        val studentMarkRecord = moduleRegistration.flatMap(mr => studentModuleMarkRecords(mr.sitsModuleCode)(mr.occurrence).find(_.sprCode == sprCode)).get

        validateModuleMark(errors)(item, moduleRegistration, studentMarkRecord, department, canEditAgreedMarks, doGradeValidation = true)

        // Validate that every entry has a grade and a result
        if (!item.grade.hasText) {
          errors.rejectValue("grade", "NotEmpty")
        }
        if (!item.result.hasText) {
          errors.rejectValue("result", "NotEmpty")
        }

        // Validate that the result has an agreed status that isn't Held. Make sure there's no existing
        // validation errors for mark and grade so we know it's either empty or a valid int and the grade is non-empty
        if (!errors.hasFieldErrors("mark") && !errors.hasFieldErrors("grade")) {
          val mark = item.mark.maybeText.map(_.toInt)

          val gradeBoundary = moduleRegistration.flatMap { moduleRegistration =>
            assessmentMembershipService.gradesForMark(moduleRegistration, mark).find(_.grade == item.grade)
          }

          gradeBoundary match {
            case None =>
              errors.rejectValue("grade", "actualGrade.noGradeBoundary")

            case Some(gb) if gb.agreedStatus == GradeBoundaryAgreedStatus.Held =>
              errors.rejectValue("grade", "actualGrade.temporary")

            case _ => // This is fine
          }

        }
      }

      errors.popNestedPath()
    }
  }

}
