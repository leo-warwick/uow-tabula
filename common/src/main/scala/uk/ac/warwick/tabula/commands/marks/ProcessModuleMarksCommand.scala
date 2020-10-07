package uk.ac.warwick.tabula.commands.marks

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.ModuleOccurrenceCommands.OptionalMarksItem
import uk.ac.warwick.tabula.commands.marks.ProcessModuleMarksCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.marks.ValidGradesForMark
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

object ProcessModuleMarksCommand {
  type Result = Seq[RecordedModuleRegistration]
  type Command = Appliable[Result]
    with ProcessModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with SelfValidating
    with BindListener
    with PopulateOnForm

  type SprCode = ModuleOccurrenceCommands.SprCode
  class StudentModuleMarksItem extends ModuleOccurrenceCommands.StudentModuleMarksItem with OptionalMarksItem {
    def this(sprCode: SprCode) {
      this()
      this.sprCode = sprCode
    }
  }

  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String, currentUser: CurrentUser): Command =
    new ProcessModuleMarksCommandInternal(sitsModuleCode, module, academicYear, occurrence, currentUser)
      with ModuleOccurrenceLoadModuleRegistrations
      with ProcessModuleMarksRequest
      with ProcessModuleMarksValidation
      with ModuleOccurrenceUpdateMarksPermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringTransactionalComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[Result] // late-init due to ModuleOccurrenceLoadModuleRegistrations being called from permissions
      with RecordedModuleRegistrationsDescription
      with ModuleOccurrenceValidGradesBindListener
      with ProcessModuleMarksBindListener
      with ProcessModuleMarksPopulateOnForm
      with ConfirmModuleMarkChangedCommandNotification
      with AutowiringProfileServiceComponent
}

abstract class ProcessModuleMarksCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ModuleOccurrenceState
    with ClearRecordedModuleMarksState
    with ProcessModuleAndComponentMarks {

  self: ProcessModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with TransactionalComponent
    with RecordedModuleRegistrationsDescription
    with ModuleRegistrationMarksServiceComponent
    with AssessmentComponentMarksServiceComponent =>

  override val mandatoryEventName: String = "ProcessModuleMarks"

  lazy val allRecordedModuleRegistrations: Map[SprCode, RecordedModuleRegistration] =
    moduleRegistrationMarksService.getAllRecordedModuleRegistrations(sitsModuleCode, academicYear, occurrence)
      .map(rmr => rmr.sprCode -> rmr)
      .toMap

  val allRecordedAssessmentComponentStudents: Map[UpstreamAssessmentGroup, Map[UpstreamAssessmentGroupMember, RecordedAssessmentComponentStudent]] =
    students.asScala.values.filter(_.process).toSeq
      .flatMap { item =>
        val moduleRegistration =
          moduleRegistrations.find(_.sprCode == item.sprCode)
            .get // We validate that this exists

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
    mr => allRecordedModuleRegistrations.get(mr.sprCode)

  override def applyInternal(): Result = transactional() {
    students.asScala.values.filter(_.process).toSeq.map { item =>
      val moduleRegistration = moduleRegistrations.find(_.sprCode == item.sprCode).get // We validate that this exists
      process(moduleRegistration, item)
    }
  }
}

trait ProcessModuleAndComponentMarks {

  self: RetrieveComponentMarks with AssessmentComponentMarksServiceComponent with ModuleRegistrationMarksServiceComponent =>

  val currentUser: CurrentUser
  val existingRecordedModuleRegistration: ModuleRegistration => Option[RecordedModuleRegistration]
  val allRecordedAssessmentComponentStudents: Map[UpstreamAssessmentGroup, Map[UpstreamAssessmentGroupMember, RecordedAssessmentComponentStudent]]

  def process(moduleRegistration: ModuleRegistration, item: ModuleOccurrenceCommands.StudentModuleMarksItem): RecordedModuleRegistration = {

    val components = componentMarks(moduleRegistration)

    require(item.grade.nonEmpty && item.result.nonEmpty)

    val recordedModuleRegistration: RecordedModuleRegistration = existingRecordedModuleRegistration(moduleRegistration)
      .getOrElse(new RecordedModuleRegistration(moduleRegistration))

    recordedModuleRegistration.addMark(
      uploader = currentUser.apparentUser,
      mark = item.mark.maybeText.map(_.toInt),
      grade = item.grade.maybeText,
      result = item.result.maybeText.flatMap(c => Option(ModuleResult.fromCode(c))),
      source = RecordedModuleMarkSource.ProcessModuleMarks,
      markState = MarkState.Agreed,
      comments = item.comments,
    )

    // change the state of all components that are Unconfirmed actual (or that have no state)
    // this includes writing an empty agreed mark/grade if necessary - it stops it being modified later
    components.values.map(_._1).filterNot(c => c.markState.contains(MarkState.Agreed) || c.agreed).foreach { component =>
      val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent = allRecordedAssessmentComponentStudents
        .getOrElse(component.upstreamAssessmentGroupMember.upstreamAssessmentGroup, Map.empty)
        .getOrElse(component.upstreamAssessmentGroupMember, new RecordedAssessmentComponentStudent(component.upstreamAssessmentGroupMember))

      recordedAssessmentComponentStudent.addMark(
        uploader = currentUser.apparentUser,
        mark = component.mark,
        grade = component.grade,
        comments = null,
        source = RecordedAssessmentComponentStudentMarkSource.ProcessModuleMarks,
        markState = MarkState.Agreed
      )

      assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)
    }

    moduleRegistrationMarksService.saveOrUpdate(recordedModuleRegistration)

    recordedModuleRegistration
  }
}

trait ProcessModuleMarksRequest extends ModuleOccurrenceMarksRequest[StudentModuleMarksItem] {
  override var students: JMap[SprCode, StudentModuleMarksItem] =
    LazyMaps.create { sprCode: SprCode => new StudentModuleMarksItem(sprCode) }
      .asJava
}

trait ProcessModuleMarksPopulateOnForm extends PopulateOnForm {
  self: ModuleOccurrenceState
    with ProcessModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with AssessmentMembershipServiceComponent =>

  override def populate(): Unit =
    studentModuleMarkRecords.foreach { student =>
      val s = new StudentModuleMarksItem(student.sprCode)
      s.populate(student)(assessmentMembershipService)
      s.shouldProcess(student)
      students.put(student.sprCode, s)
    }
}

trait ProcessModuleMarksBindListener extends BindListener {
  self: ProcessModuleMarksRequest
    with ModuleOccurrenceValidGradesBindListener
    with TransactionalComponent =>

  override def onBind(result: BindingResult): Unit = onBindValidGrades(result)
}

trait ProcessModuleMarksValidation extends ModuleOccurrenceValidation with SelfValidating {
  self: ModuleOccurrenceState
    with ClearRecordedModuleMarksState
    with ProcessModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with AssessmentMembershipServiceComponent
    with SecurityServiceComponent =>

  override def validate(errors: Errors): Unit = {
    students.asScala.foreach { case (sprCode, item) =>
      errors.pushNestedPath(s"students[$sprCode]")

      if (item.process) {
        // Departments are not allowed to opt out of grade validation for agreed marks
        validateMarkEntry(errors)(item, doGradeValidation = true)

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

          val gradeBoundary =
            moduleRegistrations.find(_.sprCode == sprCode)
              .flatMap { moduleRegistration =>
                assessmentMembershipService.gradesForMark(moduleRegistration, mark)
                  .find(_.grade == item.grade)
              }

          gradeBoundary match {
            case None =>
              errors.rejectValue("grade", "actualGrade.noGradeBoundary")

            case Some(gb) if gb.agreedStatus == GradeBoundaryAgreedStatus.Held =>
              errors.rejectValue("grade", "actualGrade.temporary")

            case _ => // This is fine
          }

          // TODO Do we need to validate that combinations of resit grades are allowed? e.g. can I have S for the module
          // but R for individual components?
        }
      }

      errors.popNestedPath()
    }
  }
}
