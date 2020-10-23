package uk.ac.warwick.tabula.commands.marks

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.commands.marks.ExamBoardOutcomesCommand._
import uk.ac.warwick.tabula.commands.marks.ProcessModuleMarksCommand.SprCode
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{ActualProgressionDecision, Department, ProgressionDecision, ProgressionDecisionProcessStatus, RecordedDecision, StudentAward}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringProgressionDecisionServiceComponent, AutowiringStudentAwardServiceComponent, ProgressionDecisionServiceComponent, StudentAwardServiceComponent}
import uk.ac.warwick.tabula.services.marks.{AutowiringDecisionServiceComponent, DecisionServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, SprCode}

import scala.collection.SortedMap
import scala.jdk.CollectionConverters._

object ExamBoardOutcomesCommand {

  case class StudentDecisionRecord (
    sprCode: SprCode,
    firstName: String,
    lastName: String,
    academicYear: AcademicYear,
    isFinalist: Boolean,
    existingRecordedDecision: Option[RecordedDecision],
    progressionDecision: ProgressionDecision,
    studentAwards: Seq[StudentAward]
  ) {
    val universityId: String = SprCode.getUniversityId(sprCode)
    val isResitting: Boolean = progressionDecision.resitPeriod
    val isAgreed: Boolean = progressionDecision.status == ProgressionDecisionProcessStatus.Complete
  }

  type SprCode = String

  class ExamBoardOutcomeItem {
    def this(sprCode: SprCode) {
      this()
      this.sprCode = sprCode
    }

    var sprCode: SprCode = _
    var decision: ActualProgressionDecision = _
    var notes: String = _
    var record: Boolean = true
  }

  type Result = Seq[RecordedDecision]
  type Command = Appliable[Result] with ExamBoardOutcomesState with ExamBoardOutcomesRequest with SelfValidating with PopulateOnForm
  val RequiredPermission: Permission = Permissions.Feedback.Publish

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser) =
    new ExamBoardOutcomesCommandInternal(department, academicYear, currentUser)
      with ComposableCommand[Result]
      with ExamBoardOutcomesRequest
      with ExamBoardOutcomesValidation
      with ExamBoardOutcomesPermissions
      with ExamBoardOutcomesDescription
      with ExamBoardOutcomesCommandPopulateOnForm
      with AutowiringDecisionServiceComponent
      with AutowiringProgressionDecisionServiceComponent
      with AutowiringStudentAwardServiceComponent
}

class ExamBoardOutcomesCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result] with ExamBoardOutcomesState with ExamBoardOutcomesValidation {

  self: ExamBoardOutcomesRequest with DecisionServiceComponent with ProgressionDecisionServiceComponent with StudentAwardServiceComponent =>

  def applyInternal(): Result = transactional() {

    studentsToRecord.map { case (sprCode, item) =>
      val studentDecisionRecord = studentDecisionRecords(sprCode).get
      val recordedDecision: RecordedDecision = studentDecisionRecord.existingRecordedDecision.getOrElse(new RecordedDecision)
      recordedDecision.sprCode = sprCode
      recordedDecision.academicYear = academicYear
      recordedDecision.sequence = studentDecisionRecord.progressionDecision.sequence
      recordedDecision.decision = item.decision
      recordedDecision.notes = item.notes
      recordedDecision.resitPeriod = studentDecisionRecord.isResitting
      recordedDecision.updatedBy = currentUser.apparentUser
      recordedDecision.updatedDate = DateTime.now
      recordedDecision.needsWritingToSitsSince = Some(DateTime.now)
      decisionService.saveOrUpdate(recordedDecision)
    }.toSeq
  }
}

trait ExamBoardOutcomesCommandPopulateOnForm extends PopulateOnForm {
  self: ExamBoardOutcomesRequest with ExamBoardOutcomesState =>

  override def populate(): Unit = {
    for (
      (sprCode, sdr) <- studentDecisionRecords;
      existing <- sdr.flatMap(_.existingRecordedDecision) if !sdr.exists(_.progressionDecision.status == ProgressionDecisionProcessStatus.Complete)
    ){
      val s = new ExamBoardOutcomeItem(sprCode)
      s.decision = existing.decision
      s.notes = existing.notes
      s.record = false
      students.put(sprCode, s)
    }
  }
}


trait ExamBoardOutcomesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ExamBoardOutcomesState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, mandatory(department))
    mandatory(academicYear)
  }
}

trait ExamBoardOutcomesValidation extends SelfValidating {
  self: ExamBoardOutcomesRequest =>

  def validate(errors: Errors) {
    studentsToRecord.foreach { case (sprCode, item) =>
      errors.pushNestedPath(s"students[$sprCode]")
      if (item.decision == null)
        errors.rejectValue("decision", "NotEmpty")
      else if (item.decision.notesRequired && !item.notes.hasText)
        errors.rejectValue("notes", "NotEmpty")
      errors.popNestedPath()
    }
  }
}

trait ExamBoardOutcomesDescription extends Describable[Result] {
  self: ExamBoardOutcomesState with ExamBoardOutcomesRequest =>

  def describe(d: Description) {
    d.department(department)
      .studentIds(studentsToRecord.keys.map(SprCode.getUniversityId).toSeq)
      .properties(
        "academicYear" -> academicYear.toString,
      )
  }
}

trait ExamBoardOutcomesState {

  self: DecisionServiceComponent with ProgressionDecisionServiceComponent with StudentAwardServiceComponent =>

  val currentUser: CurrentUser
  val department: Department
  val academicYear: AcademicYear

  var entities: Seq[ExamGridEntity] = _

  lazy val entitiesBySprCode: SortedMap[SprCode, ExamGridEntity] = SortedMap(entities.flatMap { e =>
    e.validYears.lastOption.map(_._2).flatMap(_.studentCourseYearDetails).map(_.studentCourseDetails.sprCode).map(_ -> e)
  }: _*)

  lazy val existingRecordedDecisions: Map[SprCode, Seq[RecordedDecision]] = decisionService.findDecisions(entitiesBySprCode.keys.toSeq)
    .filter(_.academicYear == academicYear)
    .groupBy(_.sprCode)

  lazy val confirmedAwards: Map[String, Seq[StudentAward]] = studentAwardService.getByUniversityIds(entities.map(_.universityId))
    .filter(_.academicYear == academicYear)
    .groupBy(_.sprCode)

  lazy val allDecisions: Map[SprCode, Seq[ProgressionDecision]] =
    progressionDecisionService.getByUniversityIds(entities.map(_.universityId))
      .filter(_.academicYear == academicYear)
      .groupBy(_.sprCode)


  lazy val studentDecisionRecords: SortedMap[SprCode, Option[StudentDecisionRecord]] = entitiesBySprCode.flatMap { case (sprCode, e) =>
    e.validYears.lastOption.map(_._2).map { ey =>
      sprCode -> allDecisions.getOrElse(sprCode, Nil).sortBy(_.sequence).lastOption.map { decision =>

        val isFinalist = ey.studentCourseYearDetails.exists { scyd =>
          val finalYear: Int = scyd.level.map(_.toYearOfStudy).getOrElse(scyd.studentCourseDetails.courseYearLength)
          scyd.yearOfStudy >= finalYear
        }

        val studentAwards = confirmedAwards.getOrElse(sprCode, Nil)

        val existingRecordedDecision = existingRecordedDecisions.getOrElse(sprCode, Nil).find(_.sequence == decision.sequence)

        StudentDecisionRecord(
          sprCode,
          e.firstName,
          e.lastName,
          academicYear,
          isFinalist,
          existingRecordedDecision,
          decision,
          studentAwards
        )
      }
    }
  }
}

trait ExamBoardOutcomesRequest {
  var students: JMap[SprCode, ExamBoardOutcomeItem] = LazyMaps.create { sprCode: SprCode => new ExamBoardOutcomeItem(sprCode)}.asJava

  def studentsToRecord: Map[SprCode, ExamBoardOutcomeItem] = students.asScala.filter(_._2.record).toMap
}
