package uk.ac.warwick.tabula.commands.marks

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.JMap
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.CohortCommand._
import uk.ac.warwick.tabula.commands.marks.GenerateCohortResitsCommand._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MarkState.Agreed
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, SprCode}

import scala.jdk.CollectionConverters._

object GenerateCohortResitsCommand {

  val MaxAttemptNumber = 3

  type Result = Seq[RecordedResit]
  type Command = Appliable[Result] with GenerateCohortResitsState with GenerateCohortResitsRequest with SelfValidating with PopulateOnForm
  val RequiredPermission: Permission = Permissions.Feedback.Publish

  type SprCode = String
  type ModuleCode = String
  type Sequence = String

  case class ReassessmentModule (
    moduleCode: ModuleCode,
    occurrence: Occurrence,
    markRecord: StudentModuleMarkRecord,
    reassessmentComponents: Seq[ReassessmentComponent]
  )

  case class ReassessmentComponent (
    component: AssessmentComponent,
    newAttempt: Attempt, // attempt is incremented if the grade assigned specifies that it should be
    assessmentType: AssessmentType,
    existingResit: Option[RecordedResit],
    sitsResit: Option[UpstreamAssessmentGroupMember]
  )

  class ResitItem {
    def this(sprCode: SprCode, moduleCode: ModuleCode, sequence: Sequence, attempt: Attempt) = {
      this()
      this.sprCode = sprCode
      this.sequence = sequence
      this.moduleCode = moduleCode
      this.attempt = attempt
    }

    var sprCode: SprCode = _
    var moduleCode: ModuleCode = _
    var sequence: Sequence = _

    var create: Boolean = _
    var attempt: Attempt = _
  }

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser) =
    new GenerateCohortResitsCommandInternal(department, academicYear, currentUser)
      with ComposableCommand[Result]
      with GenerateCohortResitsCommandPopulateOnForm
      with GenerateCohortResitsRequest
      with GenerateCohortResitsValidation
      with GenerateCohortResitsPermissions
      with GenerateCohortResitsDescription
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringResitServiceComponent
}

class GenerateCohortResitsCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result] with GenerateCohortResitsState with GenerateCohortResitsValidation {

  self: GenerateCohortResitsRequest with ModuleRegistrationMarksServiceComponent with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent with ResitServiceComponent =>

  def applyInternal(): Result = transactional() {
    (for ((sprCode, modules) <- resitsToCreate; (moduleCode, resits) <- modules; (sequence, resit) <- resits) yield {

      val reassessmentModule = requiresResit(sprCode)
        .find(_.moduleCode == moduleCode)
        .get // has to exist if we have a resit being created

      val reassessmentComponent = reassessmentModule.reassessmentComponents
        .find(_.component.sequence == sequence)
        .get // has to exist if we have a resit being created

      val recordedResit = reassessmentComponent.existingResit.getOrElse {
        val r = new RecordedResit()
        r.sprCode = sprCode
        r.sequence = reassessmentComponent.component.sequence
        r.moduleCode = reassessmentModule.moduleCode
        r.academicYear = academicYear
        r.occurrence = reassessmentModule.occurrence
        r.assessmentType = reassessmentComponent.assessmentType
        r.marksCode = reassessmentComponent.component.marksCode
        r.weighting = reassessmentComponent.component.rawWeighting
        r
      }
      recordedResit.currentResitAttempt = resit.attempt
      recordedResit.needsWritingToSitsSince = Some(DateTime.now)
      recordedResit.updatedBy = currentUser.apparentUser
      recordedResit.updatedDate = DateTime.now
      resitService.saveOrUpdate(recordedResit)
    }).toSeq
  }
}

trait GenerateCohortResitsCommandPopulateOnForm extends PopulateOnForm {
  self: GenerateCohortResitsRequest with GenerateCohortResitsState =>

  override def populate(): Unit = {
    for ((sprCode, modules) <- requiresResit;  module <- modules; rc <- module.reassessmentComponents) {
      val item = new ResitItem(sprCode, module.moduleCode, rc.component.sequence, rc.existingResit.map(_.currentResitAttempt).getOrElse(rc.newAttempt))
      item.create = rc.existingResit.isDefined
      resits.get(sprCode).get(module.moduleCode).put(rc.component.sequence, item)
    }
  }
}


trait GenerateCohortResitsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: GenerateCohortResitsState =>

  def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(RequiredPermission, mandatory(department))
    mandatory(academicYear)
  }
}

trait GenerateCohortResitsValidation extends SelfValidating {
  self: GenerateCohortResitsRequest =>

  def validate(errors: Errors) {
    if (resitsToCreate.values.flatMap(_.values.flatten).isEmpty) errors.reject("resit.noneSelected")

    for ((sprCode, modules) <- resitsToCreate; (moduleCode, resits) <- modules; (sequence, resit) <- resits) {
      if (resit.attempt == null) {
        errors.rejectValue(s"resits[$sprCode][$moduleCode][$sequence].attempt", "resit.attempt.missing")
      }
    }
  }
}

trait GenerateCohortResitsDescription extends Describable[Result] {
  self: GenerateCohortResitsState =>

  override lazy val eventName: String = "GenerateCohortResits"

  override def describe(d: Description): Unit =
    d.department(department)
      .studentIds(requiresResit.keys.map(SprCode.getUniversityId).toSeq)
      .properties(
        "academicYear" -> academicYear.toString,
      )

  override def describeResult(d: Description, result: Result): Unit = {
    d.department(department)
      .studentIds(result.map(r => SprCode.getUniversityId(r.sprCode)).distinct)
      .properties("academicYear" -> academicYear.toString)
  }

}

trait GenerateCohortResitsState extends CohortState with CohortModuleMarksRecords {

  self: ModuleRegistrationMarksServiceComponent with AssessmentMembershipServiceComponent with AssessmentComponentMarksServiceComponent
    with ResitServiceComponent =>

  val department: Department
  val academicYear: AcademicYear

  lazy val resitModuleMarkRecords: Map[SprCode,  Seq[StudentModuleMarkRecord]] = studentModuleMarkRecords.values.flatMap(_.values).flatten.toSeq
    .filter(mmr => mmr.markState.contains(Agreed) && mmr.requiresResit)
    .sortBy(_.moduleRegistration.sitsModuleCode)
    .groupBy(_.sprCode)

  lazy val assessmentComponents: Map[ModuleCode, Seq[AssessmentComponent]] =
    assessmentMembershipService.getAssessmentComponents(resitModuleMarkRecords.values.flatten.map(_.moduleRegistration.sitsModuleCode).toSet, inUseOnly = false)
      .filter(_.sequence != AssessmentComponent.NoneAssessmentGroup)
      .groupBy(_.moduleCode)

  lazy val upstreamAssessmentGroupInfos: Seq[UpstreamAssessmentGroupInfo] =
    assessmentMembershipService.getUpstreamAssessmentGroupInfoForComponents(assessmentComponents.values.flatten.toSeq, academicYear)

  lazy val existingSitsResits: Seq[UpstreamAssessmentGroupMember] = upstreamAssessmentGroupInfos.flatMap(_.resitMembers)

  lazy val studentComponentMarkRecords: Map[AssessmentComponent, Seq[StudentMarkRecord]] = upstreamAssessmentGroupInfos
    .filter(_.allMembers.nonEmpty)
    .map { info =>
      info.upstreamAssessmentGroup.assessmentComponent.get ->
        ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService, resitService, assessmentMembershipService)
    }.toMap

  private def getComponentMarkRecord(ac: AssessmentComponent, universityId: String, agreedOnly: Boolean): Option[StudentMarkRecord] = {
    studentComponentMarkRecords.getOrElse(ac, Nil)
      .filter(mr => mr.universityId == universityId && (!agreedOnly || mr.agreed))
      .sortBy(_.resitSequence.getOrElse("000"))
      .lastOption
  }

  lazy val requiresResit: Map[SprCode, Seq[ReassessmentModule]] = resitModuleMarkRecords.view.mapValues { moduleMarksRecords =>
    moduleMarksRecords.map { mmr =>

      val latestComponents = mmr.moduleRegistration.upstreamAssessmentGroupMembers.map { uagm =>
        val component = uagm.upstreamAssessmentGroup.assessmentComponent.get
        (component, uagm, getComponentMarkRecord(component, uagm.universityId, agreedOnly = true))
      }

      val reassessmentComponents = {
        val acs = latestComponents.map(_._1)
        val resitComponents = if (acs.isEmpty) {
          Nil
        } else if (acs.map(_.reassessmentGroup).forall(rg => rg.isDefined && rg == acs.head.reassessmentGroup)) {
           acs.head.reassessmentComponents
        } else {
          latestComponents.filter(_._3.exists(_.requiresResit)).map(lc => lc._1.replacedBy.getOrElse(lc._1))
        }

        resitComponents.map { rc =>

          val relatedMarkRecord = latestComponents.find(_._1 == rc).flatMap(_._3)

          val newAttempt = {
            val existingAttemptNumber = relatedMarkRecord.flatMap(_.currentResitAttempt)
              .orElse(latestComponents.headOption.flatMap(_._3).flatMap(_.currentResitAttempt))
              .getOrElse(1)

            // if there is an agreed result for this component see if the grade increments the attempt - if not use the module grade
            val newAttemptNumber = if (relatedMarkRecord.map(_.incrementsAttempt).getOrElse(mmr.incrementsAttempt)) {
              Math.min(MaxAttemptNumber, existingAttemptNumber + 1)
            } else {
              existingAttemptNumber
            }

            Attempt.withValue(newAttemptNumber)
          }

          // if related mark record is empty this is the first the reassessment component has been used so this must be the first reassessment
          val isReassessment = relatedMarkRecord.exists(_.isReassessment)

          val assessmentType = rc.assessmentType match {
            case _ @ (_:ExamType | _:OnlineExamType) if !isReassessment => AssessmentType.OnlineSeptemberExam
            case e: ExamType if e.onlineEquivalent.isDefined => e.onlineEquivalent.get
            case o: OnlineExamType => o
            case _ => rc.assessmentType
          }

          val scmr = getComponentMarkRecord(rc, SprCode.getUniversityId(mmr.sprCode), agreedOnly = false)

          val existingSitsResit = existingSitsResits.find { uagm =>
            uagm.universityId == SprCode.getUniversityId(mmr.sprCode) &&
              uagm.upstreamAssessmentGroup.sequence == rc.sequence &&
              uagm.resitSequence.exists(_ > scmr.flatMap(_.resitSequence).getOrElse("000"))
          }

          ReassessmentComponent(rc, newAttempt, assessmentType, scmr.flatMap(_.existingResit), existingSitsResit)
        }
      }

      ReassessmentModule(mmr.moduleRegistration.sitsModuleCode, mmr.moduleRegistration.occurrence, mmr, reassessmentComponents)
    }
  }.toMap


}

trait GenerateCohortResitsRequest {

  self: GenerateCohortResitsState =>

  var resits: JMap[SprCode, JMap[ModuleCode, JMap[Sequence, ResitItem]]] = LazyMaps.create { sprCode: SprCode =>
    LazyMaps.create { moduleCode: ModuleCode =>
      LazyMaps.create { sequence: Sequence =>
        new ResitItem(sprCode, moduleCode, sequence, null)
      }.asJava
    }.asJava
  }.asJava

  def resitsToCreate: Map[SprCode, Map[ModuleCode, Map[Sequence, ResitItem]] ] =
    resits.asScala.view.mapValues(_.asScala.view.mapValues(_.asScala.toMap.filter(_._2.create)).toMap).toMap
}
