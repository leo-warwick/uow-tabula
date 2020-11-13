package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{StudentMember, _}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}

import scala.jdk.CollectionConverters._


object ExamGridStudentCheckCommand {
  def apply(department: Department, academicYear: AcademicYear) =
    new ExamGridStudentCheckCommandInternal(department, academicYear)
      with ComposableCommand[StudentCheckInfo]
      with ExamGridStudentCheckValidation
      with ExamGridStudentCheckPermissions
      with Unaudited with ReadOnly
      with ExamGridStudentCheckState
      with GenerateExamGridSelectCourseCommandRequest
      with AutowiringAssessmentMembershipServiceComponent
}

case class StudentCheckInfo(
  hasEnrolmentForYear: Boolean,
  courseMatches: Boolean,
  courseOccurrencesMatches: Boolean,
  isTemporarilyWithdrawn: Boolean,
  routeMatches: Boolean,
  isResitStudent: Boolean,
  yearOrLevelMatches: Boolean,
  confirmedModuleRegs: Boolean,
  lastImportDate: DateTime
)

class ExamGridStudentCheckCommandInternal(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[StudentCheckInfo] {

  self: ExamGridStudentCheckState with GenerateExamGridSelectCourseCommandRequest with AssessmentMembershipServiceComponent =>

  def applyInternal(): StudentCheckInfo = {
    val scyds = student.allFreshStudentCourseYearDetailsForYear(academicYear)

    StudentCheckInfo(
      scyds.exists(_.enrolledOrCompleted),
      scyds.exists(scyd => courses.asScala.contains(scyd.studentCourseDetails.course)),
      scyds.exists(scyd => courseOccurrences.asScala.contains(scyd.blockOccurrence)),
      scyds.exists(_.enrolmentStatus.code.startsWith("T")),
      scyds.exists(scyd => routes.contains(scyd.studentCourseDetails.currentRoute)),
      assessmentMembershipService.getUpstreamAssessmentGroups(student, academicYear, resitOnly = true).nonEmpty,
      if (isLevelGrid) scyds.exists(_.studyLevel == levelCode) else scyds.exists(_.yearOfStudy == yearOfStudy),
      scyds.forall(_.moduleRegistrationStatus == ModuleRegistrationStatus.Confirmed),
      Option(student.lastImportDate).getOrElse(DateTime.now)
    )
  }

}

trait ExamGridStudentCheckValidation extends SelfValidating {

  self: ExamGridStudentCheckState =>

  override def validate(errors: Errors): Unit = {
    if (member == null) {
      errors.reject("examGrid.studentcheck.missingstudentmember")
    } else if (!member.isInstanceOf[StudentMember]) {
      errors.reject("examGrid.studentcheck.notAStudent", Array(member.fullName.getOrElse(""), member.universityId), "")
    }
  }

}

trait ExamGridStudentCheckPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ExamGridStudentCheckState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Department.ExamGrids, department)
  }
}

trait ExamGridStudentCheckState {
  var member: Member = _
  lazy val student: StudentMember = member.asInstanceOf[StudentMember]

  def academicYear: AcademicYear

  def department: Department
}
