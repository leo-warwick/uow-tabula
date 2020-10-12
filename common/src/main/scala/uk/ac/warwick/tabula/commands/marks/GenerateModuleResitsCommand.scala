package uk.ac.warwick.tabula.commands.marks

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.GenerateModuleResitsCommand.{ResitItem, Result, Sequence, SprCode}
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkState.Agreed
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, SprCode}

import scala.jdk.CollectionConverters._

case class StudentMarks (
  module: StudentModuleMarkRecord,
  requiresResit: Boolean,
  incrementsAttempt: Boolean,
  components: Map[AssessmentComponent, StudentMarkRecord],
  assessmentTypes: Map[AssessmentComponent, Seq[AssessmentType]],
  sitsResits: Map[AssessmentComponent, Option[UpstreamAssessmentGroupMember]]
) {
  def resitOutOfSync(ac: AssessmentComponent): Boolean = {
    val existingResit = components.get(ac).flatMap(_.existingResit)
    val sitsResit = sitsResits.get(ac).flatten

    (existingResit, sitsResit) match {
      case (Some(er), Some(sr)) =>
        er.resitSequence != sr.resitSequence ||
        er.assessmentType != sr.resitAssessmentType.orNull ||
        er.weighting != sr.resitAssessmentWeighting.orNull ||
        er.currentResitAttempt != sr.currentResitAttempt.orNull
      case _ => false
    }
  }
}

object GenerateModuleResitsCommand {

  type Result = Seq[RecordedResit]
  type Command = Appliable[Result]
    with GenerateModuleResitsState
    with GenerateModuleResitsRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with GenerateModuleResitsValidation
    with SelfValidating
    with PopulateOnForm
  type SprCode = String
  type Sequence = String


  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String, currentUser: CurrentUser) =
    new GenerateModuleResitsCommandInternal(sitsModuleCode, module, academicYear, occurrence, currentUser)
      with GenerateModuleResitsRequest
      with GenerateModuleResitsValidation
      with ModuleOccurrenceUpdateMarksPermissions
      with ModuleOccurrenceLoadModuleRegistrations
      with AutowiringResitServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[Result] // late-init due to ModuleOccurrenceUpdateMarksPermissions being called from permissions
      with ModuleOccurrenceDescription[Result]
      with GenerateModuleResitsPopulateOnForm

  class ResitItem {
    def this(sprCode: String, sequence: String, weighting: String, attempt: String) = {
      this()
      this.sprCode = sprCode
      this.sequence = sequence
      this.weighting = weighting
      this.attempt = attempt
    }

    var sprCode: SprCode = _
    var sequence: String = _
    var create: Boolean = _
    var assessmentType: String = AssessmentType.SeptemberExam.astCode // defaults to september exam
    var weighting: String = _
    var attempt: String = _
  }
}


class GenerateModuleResitsCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, val currentUser: CurrentUser)
  extends CommandInternal[Result] with GenerateModuleResitsState with GenerateModuleResitsValidation with AutowiringSecurityServiceComponent {

  self: GenerateModuleResitsRequest with ModuleOccurrenceLoadModuleRegistrations with ModuleRegistrationMarksServiceComponent
    with AssessmentComponentMarksServiceComponent with AssessmentMembershipServiceComponent with ResitServiceComponent =>

  val mandatoryEventName: String = "GenerateModuleResits"

  def applyInternal(): Result = transactional() {
    resitsToCreate.flatMap { case (sprCode, resits) =>

      val studentMarks: Option[StudentMarks] = requiresResits.find(_.module.sprCode == sprCode)

      val components: Iterable[(AssessmentComponent, StudentMarkRecord)] = studentMarks.map(_.components).getOrElse(Nil)

      resits.flatMap { case (sequence, resitItem) =>
        val componentMarks = components.find(_._1.sequence == sequence).map(_._2)
        componentMarks.map { cm =>
          val recordedResit = cm.existingResit.getOrElse(new RecordedResit(cm, sprCode))
          recordedResit.assessmentType = resitItem.assessmentType
          recordedResit.weighting = resitItem.weighting.toInt
          recordedResit.currentResitAttempt = resitItem.attempt.toInt
          recordedResit.needsWritingToSitsSince = Some(DateTime.now)
          recordedResit.updatedBy = currentUser.apparentUser
          recordedResit.updatedDate = DateTime.now
          resitService.saveOrUpdate(recordedResit)
        }
      }
    }.toSeq
  }
}

trait GenerateModuleResitsPopulateOnForm extends PopulateOnForm {
  self: ModuleOccurrenceState with GenerateModuleResitsRequest with GenerateModuleResitsState =>

  override def populate(): Unit = {
    requiresResits.flatMap(_.components.values).flatMap(_.existingResit).foreach { er =>
      val item = new ResitItem(er.sprCode, er.sequence, er.weighting.toString, er.currentResitAttempt.toString)
      item.assessmentType = er.assessmentType.astCode
      item.create = true
      resits.get(er.sprCode).put(er.sequence, item)
    }
  }
}

trait GenerateModuleResitsValidation extends SelfValidating {
  self: GenerateModuleResitsState with GenerateModuleResitsRequest with SecurityServiceComponent =>

  lazy val canUpdateResits: Boolean =
    securityService.can(currentUser, Permissions.Marks.ModifyResits, module)

  def validate(errors: Errors): Unit = {

    if (resitsToCreate.values.flatten.isEmpty) errors.reject("moduleMarks.resit.noneSelected")

    for (
      (sprCode, resits) <- resitsToCreate;
      (sequence, resit) <- resits
    ) {

      val existingResit = requiresResits.find(_.module.sprCode == sprCode)
        .flatMap(_.components.find(_._1.sequence == sequence).flatMap(_._2.existingResit))

      if (resit.create && existingResit.isDefined && !canUpdateResits) {
        errors.rejectValue(s"resits[$sprCode][$sequence].weighting", "moduleMarks.resit.noEdit")
      }

      if (!resit.weighting.hasText) {
        errors.rejectValue(s"resits[$sprCode][$sequence].weighting", "moduleMarks.resit.weighting.missing")
      } else if (resit.weighting.toIntOption.isEmpty) {
        errors.rejectValue(s"resits[$sprCode][$sequence].weighting", "moduleMarks.resit.weighting.nonInt")
      }

      if (!resit.attempt.hasText) {
        errors.rejectValue(s"resits[$sprCode][$sequence].attempt", "moduleMarks.resit.attempt.missing")
      } else if (resit.attempt.toIntOption.isEmpty) {
        errors.rejectValue(s"resits[$sprCode][$sequence].attempt", "moduleMarks.resit.attempt.nonInt")
      } else if (resit.attempt.toInt < 1 || resit.attempt.toInt > 3 ) {
        errors.rejectValue(s"resits[$sprCode][$sequence].attempt", "moduleMarks.resit.attempt.outOfRange")
      }
    }
  }
}

trait GenerateModuleResitsState extends ModuleOccurrenceState {

  self: ModuleOccurrenceLoadModuleRegistrations with AssessmentMembershipServiceComponent with ResitServiceComponent =>

  def currentUser: CurrentUser

  private lazy val gradeBoundaries: Seq[GradeBoundary] = (moduleRegistrations.map(_.marksCode) ++ studentComponentMarkRecords.map(_._1.marksCode))
    .distinct.flatMap(assessmentMembershipService.markScheme)

  def getGradeBoundary(marksCode: String, process: GradeBoundaryProcess, grade: Option[String]): Option[GradeBoundary] =
    gradeBoundaries.find { gb => gb.marksCode == marksCode && gb.process == process && grade.contains(gb.grade) }

  lazy val existingResits: Seq[UpstreamAssessmentGroupMember] = upstreamAssessmentGroupInfos.flatMap(_.resitMembers)

  lazy val requiresResits: Seq[StudentMarks] = studentModuleMarkRecords.filter(_.markState.contains(Agreed)).flatMap { student =>
    val moduleRegistration = moduleRegistrations.find(_.sprCode == student.sprCode)

    val process = if (moduleRegistration.exists(_.currentResitAttempt.nonEmpty)) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
    val gradeBoundary = moduleRegistrations.find(_.sprCode == student.sprCode).flatMap { mr => getGradeBoundary(mr.marksCode, process, student.grade) }

    if (gradeBoundary.exists(_.generatesResit)) {

      val components = moduleRegistration.map(mr => componentMarks(mr).view.mapValues(_._1).toMap)
        .getOrElse(Map.empty[AssessmentComponent, StudentMarkRecord])

      // for each component check to see if a resit already exists in SITS - ignores resits that are the current sit (has just been marked this cycle)
      val sitsResits = components.map { case (ac, mr) => ac -> existingResits.find { uagm =>
        uagm.universityId == SprCode.getUniversityId(student.sprCode) &&
          uagm.upstreamAssessmentGroup.sequence == ac.sequence &&
          !mr.resitSequence.exists(uagm.resitSequence.contains)
      }}

      val assessmentTypes = components.map { case (ac, mr) =>
        val examTypes = if(mr.isReassessment) {
          ac.assessmentType match {
            case e: ExamType if e.onlineEquivalent.isDefined => Seq(e.onlineEquivalent.get, AssessmentType.Essay)
            case o: OnlineExamType => Seq(o, AssessmentType.Essay)
            case _ => Seq(AssessmentType.Essay, AssessmentType.OnlineSeptemberExam)
          }
        } else {
          Seq(AssessmentType.Essay, AssessmentType.OnlineSeptemberExam)
        }
        ac -> examTypes
      }

      Some(StudentMarks(student, gradeBoundary.exists(_.generatesResit), gradeBoundary.exists(_.incrementsAttempt), components, assessmentTypes, sitsResits))
    } else {
      None
    }
  }

}

trait GenerateModuleResitsRequest {
  self: GenerateModuleResitsState with ModuleOccurrenceLoadModuleRegistrations =>

  var resits: JMap[SprCode, JMap[Sequence, ResitItem]] = LazyMaps.create { sprcode: SprCode =>
    LazyMaps.create { sequence: Sequence =>
      val weighting: Int = assessmentComponents.find(_.sequence == sequence).map(_.rawWeighting.toInt).getOrElse(0)
      val attempt: Int = {
        val studentMarks: Option[StudentMarks] = requiresResits.find(_.module.sprCode == sprcode)
        val markRecord = requiresResits.find(_.module.sprCode == sprcode)
          .map(_.components)
          .getOrElse(Nil)
          .find(_._1.sequence == sequence).map(_._2)

        val currentAttempt = markRecord.flatMap(_.currentResitAttempt).getOrElse(1)
        // increment the attempt but cap at three
        if (studentMarks.exists(_.incrementsAttempt)) (currentAttempt + 1).min(3) else currentAttempt
      }
      new ResitItem(sprcode, sequence, weighting.toString, attempt.toString)
    }.asJava
  }.asJava

  def resitsToCreate: Map[SprCode, Map[Sequence, ResitItem]]  = resits.asScala.view.mapValues(_.asScala.toMap.filter(_._2.create)).toMap

}
