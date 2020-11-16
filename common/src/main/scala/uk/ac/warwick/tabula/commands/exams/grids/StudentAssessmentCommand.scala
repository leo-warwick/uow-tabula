package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.StudentAssessmentCommand.Command
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions.Profiles
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{AutowiringNormalCATSLoadServiceComponent, AutowiringUpstreamRouteRuleServiceComponent, NormalCATSLoadServiceComponent, UpstreamRouteRuleServiceComponent}
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.services.mitcircs.AutowiringMitCircsSubmissionServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, FeaturesComponent, ItemNotFoundException}

import scala.collection.mutable

object StudentAssessmentCommand {
  type Command = Appliable[StudentMarksBreakdown] with StudentAssessmentCommandState with PermissionsChecking

  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear): Command =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with AutowiringMitCircsSubmissionServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringStudentAwardServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringFeaturesComponent
      with ComposableCommand[StudentMarksBreakdown]
      with StudentAssessmentPermissions
      with StudentAssessmentCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited {
      override val includeActualMarks: Boolean = true

      override def mitCircsSubmissions: Option[Seq[MitigatingCircumstancesSubmission]] =
        Some(mitCircsSubmissionService.submissionsWithOutcomes(studentCourseDetails.student))
    }
}

object StudentAssessmentProfileCommand {
  def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear): Command =
    new StudentAssessmentCommandInternal(studentCourseDetails, academicYear)
      with ComposableCommand[StudentMarksBreakdown]
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringNormalCATSLoadServiceComponent
      with AutowiringUpstreamRouteRuleServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringStudentAwardServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringFeaturesComponent
      with StudentAssessmentProfilePermissions
      with StudentAssessmentCommandState
      with StudentModuleRegistrationAndComponents
      with ReadOnly with Unaudited {
      override val includeActualMarks: Boolean = false
    }
}

case class StudentMarksBreakdown(
  yearMark: Option[BigDecimal],
  weightedMeanYearMark: Option[BigDecimal],
  yearWeighting: Option[CourseYearWeighting],
  modules: Seq[ModuleRegistrationAndComponents],
  mitigatingCircumstances: Option[Seq[MitigatingCircumstancesSubmission]],
  progressionDecisions: Seq[ProgressionDecision],
  studentAwards: Seq[StudentAward],
)

case class ModuleRegistrationAndComponents(
  moduleRegistration: ModuleRegistration,
  markState: Option[MarkState],
  markRecord: StudentModuleMarkRecord,
  components: Seq[Component],
  releasedToStudents: Boolean,
)

case class Component(
  name: String,
  assessmentType: AssessmentType,
  upstreamGroup: UpstreamGroup,
  member: UpstreamAssessmentGroupMember,
  weighting: Option[BigDecimal],
  markState: Option[MarkState],
  markRecord: StudentMarkRecord,
)

class StudentAssessmentCommandInternal(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
  extends CommandInternal[StudentMarksBreakdown] with TaskBenchmarking {
  self: StudentAssessmentCommandState
    with StudentModuleRegistrationAndComponents
    with ModuleRegistrationServiceComponent
    with CourseAndRouteServiceComponent
    with NormalCATSLoadServiceComponent
    with StudentAwardServiceComponent
    with FeaturesComponent
    with UpstreamRouteRuleServiceComponent =>

  def mitCircsSubmissions: Option[Seq[MitigatingCircumstancesSubmission]] = None

  override def applyInternal(): StudentMarksBreakdown = {
    val modules = generateModuleRegistrationAndComponents(Seq(studentCourseYearDetails))

    // We explicitly allow years in the past for year marks
    val yearMarkReleasedToStudents: Boolean = academicYear < AcademicYear.now() || MarkState.resultsReleasedToStudents(academicYear, Option(studentCourseDetails), MarkState.DecisionReleaseTime)

    val weightedMeanYearMark: Option[BigDecimal] =
      moduleRegistrationService.agreedWeightedMeanYearMark(studentCourseYearDetails.moduleRegistrations, Map(), allowEmpty = false)
        .toOption.filterNot(_ == null) // Should never happen, but just being defensive
        .filter(_ => yearMarkReleasedToStudents)

    val yearMark: Option[BigDecimal] = Option(studentCourseYearDetails.agreedMark).map(BigDecimal.apply).orElse {
      // overcatted marks are returned even if no agreed marks exist so map on weightedMeanYearMark to ensure that we are only showing "agreed" overcatt marks
      weightedMeanYearMark.flatMap(meanMark => {
        val normalLoad: BigDecimal =
          normalCATSLoadService.find(studentCourseYearDetails.route, academicYear, studentCourseYearDetails.yearOfStudy).map(_.normalLoad)
            .orElse(Option(studentCourseYearDetails.route).flatMap { r => Option(r.degreeType) }.map(_.normalCATSLoad))
            .getOrElse(DegreeType.Undergraduate.normalCATSLoad)

        val routeRules: Seq[UpstreamRouteRule] =
          studentCourseYearDetails.level.map { l =>
            upstreamRouteRuleService.list(studentCourseYearDetails.route, academicYear, l)
          }.getOrElse(Nil)

        // Sometimes this will return null for the mark, e.g. if it contains pass/fail modules, so we guard that below
        val overcatSubsets: Seq[(BigDecimal, Seq[ModuleRegistration])] =
          moduleRegistrationService.overcattedModuleSubsets(studentCourseYearDetails.moduleRegistrations, Map(), normalLoad, routeRules)

        if (overcatSubsets.nonEmpty) {
          // If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
          // find the subset that matches those modules, and show that mark if found
          studentCourseYearDetails.overcattingModules.flatMap(overcattingModules => {
            overcatSubsets
              .find { case (_, subset) => subset.size == overcattingModules.size && subset.map(_.module).forall(overcattingModules.contains) }
              .map { case (overcatMark, _) => Seq(Option(meanMark), Option(overcatMark)).flatten.max }
          }).orElse {
            if (overcatSubsets.size == 1) Some(Seq(Option(meanMark), Option(overcatSubsets.head._1)).flatten.max)
            else None
          }
        } else {
          Option(meanMark)
        }
      })
    }.filter(_ => yearMarkReleasedToStudents)

    val yearWeighting = courseAndRouteService.getCourseYearWeighting(
      studentCourseYearDetails.studentCourseDetails.course.code,
      studentCourseYearDetails.studentCourseDetails.sprStartAcademicYear,
      studentCourseYearDetails.yearOfStudy
    )

    // We only show progression decisions that are visible to the student, to prevent tutors accidentally leaking this
    val progressionDecisions =
      studentCourseDetails.progressionDecisionsByYear(academicYear)
        .filter(_.isVisibleToStudent)

    // Are there any UA* type progression decisions
    val uaProgressionDecisions = progressionDecisions.flatMap(_.outcome).exists(_.hasAward)

    lazy val pgtAwardDisplay = studentCourseDetails.courseType.contains(CourseType.PGT) &&
      (features.ignoreResultRelease || MarkState.resultsReleasedToStudents(academicYear, Some(studentCourseDetails), MarkState.DecisionReleaseTime))
    val studentAwards =  if (uaProgressionDecisions || pgtAwardDisplay) {
      studentAwardService.getBySprCodeAndAcademicYear(studentCourseDetails.sprCode,academicYear)
    } else Seq()

    StudentMarksBreakdown(yearMark, weightedMeanYearMark, yearWeighting, modules, mitCircsSubmissions, progressionDecisions, studentAwards)
  }
}

trait StudentModuleRegistrationAndComponents extends Logging {
  self: AssessmentMembershipServiceComponent with ModuleRegistrationMarksServiceComponent with AssessmentComponentMarksServiceComponent with ResitServiceComponent =>

  /**
   * Whether to consider actual marks, e.g. for calculating VAW weightings. This should be false when it will be displayed to students,
   * as they will be able to infer component marks before they are agreed from the variable weightings.
   */
  def includeActualMarks: Boolean

  def generateModuleRegistrationAndComponents(scyds: Seq[StudentCourseYearDetails]): Seq[ModuleRegistrationAndComponents] = {

    val studentsResits: Seq[RecordedResit] = resitService.findResits(scyds.map(_.studentCourseDetails.sprCode))

    lazy val gradeBoundaries: Seq[GradeBoundary] = {
      val mr = scyds.flatMap(_.moduleRegistrations)
      val ac = mr.flatMap(_.upstreamAssessmentGroups).flatMap(_.assessmentComponent)
      val marksCodes = (mr.map(_.marksCode) ++ ac.map(_.marksCode)).distinct
      marksCodes.flatMap(assessmentMembershipService.markScheme)
    }

    scyds.flatMap { scyd =>
      scyd.moduleRegistrations.map { mr =>
        def extractMarks(components: Seq[UpstreamAssessmentGroupMember]): Seq[(AssessmentType, String, Option[Int])] = components.flatMap { uagm =>
          uagm.upstreamAssessmentGroup.assessmentComponent.map { ac =>
            (ac.assessmentType, ac.sequence, if (includeActualMarks) uagm.firstDefinedMark else uagm.agreedMark)
          }
        }

        val seen: mutable.Set[UpstreamAssessmentGroupMember] = mutable.Set()
        val components: Seq[(UpstreamGroup, UpstreamAssessmentGroupMember, Option[BigDecimal])] =
          for {
            attempt <- mr.upstreamAssessmentGroupMembersAllAttempts(extractMarks)
            (uagm, weighting) <- attempt.sortBy(_._1.upstreamAssessmentGroup.sequence) if seen.add(uagm)
            aComponent <- uagm.upstreamAssessmentGroup.assessmentComponent
          } yield (new UpstreamGroup(aComponent, uagm.upstreamAssessmentGroup, attempt.map(_._1)), uagm, weighting)

        val recordedModuleRegistration: Option[RecordedModuleRegistration] = moduleRegistrationMarksService.getRecordedModuleRegistration(mr)
        val process = if (mr.currentResitAttempt.nonEmpty) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
        val grade = recordedModuleRegistration.flatMap(_.latestGrade)
        val gradeBoundary = grade.flatMap(g => gradeBoundaries.find(gb => gb.grade == g && gb.process == process && mr.marksCode == gb.marksCode))

        ModuleRegistrationAndComponents(
          moduleRegistration = mr,
          markState = recordedModuleRegistration.flatMap(_.latestState),
          markRecord = StudentModuleMarkRecord(mr, recordedModuleRegistration, gradeBoundary),
          components = components.map { case (ug, uagm, weighting) =>
            val recordedAssessmentComponentStudent: Option[RecordedAssessmentComponentStudent] = assessmentComponentMarksService.getRecordedStudent(uagm)
            val resit: Option[RecordedResit] = studentsResits.filter(r => r.sprCode == mr.sprCode && r.sequence == ug.sequence)
              .sortBy(_.resitSequence)
              .headOption
              .filterNot(r => uagm.resitSequence.exists(r.resitSequence.contains))
            val gradeBoundary = {
              val process = if (uagm.isReassessment) GradeBoundaryProcess.Reassessment else GradeBoundaryProcess.StudentAssessment
              val grade = recordedAssessmentComponentStudent.flatMap(_.latestGrade)
              grade.flatMap(g => gradeBoundaries.find(gb => gb.grade == g && gb.process == process))
            }

            Component(
              name = uagm.resitAssessmentName.getOrElse(ug.name),
              assessmentType = uagm.resitAssessmentType.getOrElse(ug.assessmentComponent.assessmentType),
              upstreamGroup = ug,
              member = uagm,
              weighting = weighting,
              markState = recordedAssessmentComponentStudent.flatMap(_.latestState),
              markRecord = StudentMarkRecord(
                UpstreamAssessmentGroupInfo(
                  uagm.upstreamAssessmentGroup,
                  Seq(uagm).filterNot(_ => scyd.studentCourseDetails.permanentlyWithdrawn)
                ),
                uagm,
                recordedAssessmentComponentStudent,
                resit,
                gradeBoundary
              ),
            )
          },
          releasedToStudents = MarkState.resultsReleasedToStudents(mr, MarkState.DecisionReleaseTime),
        )
      }
    }
  }
}

trait StudentAssessmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheckAny(
      Seq(CheckablePermission(Permissions.Department.ExamGrids, studentCourseYearDetails.enrolmentDepartment),
        CheckablePermission(Permissions.Department.ExamGrids, Option(studentCourseYearDetails.route).getOrElse(studentCourseYearDetails.studentCourseDetails.currentRoute)))
    )
  }

}

trait StudentAssessmentProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: StudentAssessmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentCourseDetails)
  }
}


trait StudentAssessmentCommandState {
  def academicYear: AcademicYear

  def studentCourseDetails: StudentCourseDetails

  lazy val studentCourseYearDetails: StudentCourseYearDetails = studentCourseDetails.freshStudentCourseYearDetailsForYear(academicYear) // fresh scyd for this year
    .orElse(studentCourseDetails.freshOrStaleStudentCourseYearDetailsForYear(academicYear)) // or stale scyd for this year
    .getOrElse(throw new ItemNotFoundException())
}
