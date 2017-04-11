package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.data.{AssessmentDaoComponent, AutowiringAssessmentDaoComponent}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.userlookup.User

trait AssessmentServiceComponent {
	def assessmentService: AssessmentService
}

trait AutowiringAssessmentServiceComponent extends AssessmentServiceComponent {
	var assessmentService: AssessmentService = Wire[AssessmentService]
}

/**
 * Service providing access to Assignments and related objects.
 */
trait AssessmentService {
	def getAssignmentById(id: String): Option[Assignment]
	def getExamById(id: String): Option[Exam]

	def save(assignment: Assignment): Unit
	def save(exam: Exam): Unit

	def deleteFormField(field: FormField) : Unit

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment]
	def getExamByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Exam]

	def getAssignmentsWithFeedback(usercode: String): Seq[Assignment]
	def getAssignmentsWithFeedback(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment]
	def getAssignmentsWithFeedback(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment]

	def getAssignmentsWithSubmission(usercode: String): Seq[Assignment]
	def getAssignmentsWithSubmission(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment]
	def getAssignmentsWithSubmission(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment]

	def getSubmissionsForAssignmentsBetweenDates(usercode: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission]

	def getAssignmentWhereMarker(user: User): Seq[Assignment]
	def getAssignmentsByDepartmentAndMarker(department: Department, user: CurrentUser): Seq[Assignment]
	def getAssignmentsByModuleAndMarker(module: Module, user: CurrentUser): Seq[Assignment]

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment]

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment]

	def filterAssignmentsByCourseAndYear(assignments: Seq[Assignment], studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment]

	def getAssignmentsClosingBetween(startInclusive: DateTime, endExclusive: DateTime): Seq[Assignment]

	def getExamsByModules(modules: Seq[Module], academicYear: AcademicYear): Map[Module, Seq[Exam]]

	def getExamsWhereMarker(user: User): Seq[Exam]

}

abstract class AbstractAssessmentService extends AssessmentService {
	self: AssessmentDaoComponent with AssessmentServiceUserGroupHelpers with MarkingWorkflowServiceComponent =>

	def getAssignmentById(id: String): Option[Assignment] = assessmentDao.getAssignmentById(id)
	def getExamById(id: String): Option[Exam] = assessmentDao.getExamById(id)

	def save(assignment: Assignment): Unit = assessmentDao.save(assignment)
	def save(exam: Exam): Unit = assessmentDao.save(exam)

	def deleteFormField(field: FormField) : Unit = assessmentDao.deleteFormField(field)

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment] =
		assessmentDao.getAssignmentByNameYearModule(name, year, module)

	def getExamByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Exam] =
		assessmentDao.getExamByNameYearModule(name, year, module)

	def getAssignmentsWithFeedback(usercode: String): Seq[Assignment] = assessmentDao.getAssignmentsWithFeedback(usercode).filter { _.isVisibleToStudentsHistoric }

	def getAssignmentsWithFeedback(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment] = {
		val allAssignments = getAssignmentsWithFeedback(studentCourseYearDetails.studentCourseDetails.student.userId)
		filterAssignmentsByCourseAndYear(allAssignments, studentCourseYearDetails)
	}

	def getAssignmentsWithFeedback(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment] =
		assessmentDao.getAssignmentsWithFeedback(usercode, academicYearOption)

	def getAssignmentsWithSubmission(usercode: String): Seq[Assignment] = assessmentDao.getAssignmentsWithSubmission(usercode).filter { _.isVisibleToStudentsHistoric }

	def getAssignmentsWithSubmission(studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment] = {
		val allAssignments = getAssignmentsWithSubmission(studentCourseYearDetails.studentCourseDetails.student.userId)
		filterAssignmentsByCourseAndYear(allAssignments, studentCourseYearDetails)
	}

	def getAssignmentsWithSubmission(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment] =
		assessmentDao.getAssignmentsWithSubmission(usercode, academicYearOption)

	def getSubmissionsForAssignmentsBetweenDates(usercode: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission] =
		assessmentDao.getSubmissionsForAssignmentsBetweenDates(usercode, startInclusive, endExclusive)

	def getAssignmentWhereMarker(user: User): Seq[Assignment] = {
		(firstMarkerHelper.findBy(user) ++ secondMarkerHelper.findBy(user))
			.distinct
			.flatMap(markingWorkflowService.getAssignmentsUsingMarkingWorkflow)
			.filter { _.isAlive }
	}

	def getAssignmentsByDepartmentAndMarker(department: Department, user: CurrentUser): Seq[Assignment] =
		getAssignmentWhereMarker(user.apparentUser).filter { _.module.adminDepartment == department }

	def getAssignmentsByModuleAndMarker(module: Module, user: CurrentUser): Seq[Assignment] =
		getAssignmentWhereMarker(user.apparentUser).filter { _.module == module }

	/**
	 * Find a recent assignment within this module or possible department.
	 */
	def recentAssignment(department: Department): Option[Assignment] = assessmentDao.recentAssignment(department)

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment] = assessmentDao.getAssignmentsByName(partialName, department)

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment] = assessmentDao.findAssignmentsByNameOrModule(query)

	def filterAssignmentsByCourseAndYear(assignments: Seq[Assignment], studentCourseYearDetails: StudentCourseYearDetails): Seq[Assignment] = {
		assignments
			.filter(_.academicYear == studentCourseYearDetails.academicYear)
			.filter(assignment => {
				val allStudentsModulesForYear = studentCourseYearDetails.studentCourseDetails.student.registeredModulesByYear(Some(assignment.academicYear))

				// include the assignment only for the course with the relevant module registration -
				// unless the student isn't registered on the module at all, in which case include this assignment under all the student's course tabs
				studentCourseYearDetails.registeredModules.contains(assignment.module) || !allStudentsModulesForYear.contains(assignment.module)
			}
		)
	}

	def getAssignmentsClosingBetween(start: DateTime, end: DateTime): Seq[Assignment] = assessmentDao.getAssignmentsClosingBetween(start, end)

	def getExamsByModules(modules: Seq[Module], academicYear: AcademicYear): Map[Module, Seq[Exam]] =
		assessmentDao.getExamsByModules(modules, academicYear)

	def getExamsWhereMarker(user: User): Seq[Exam] = {
		(firstMarkerHelper.findBy(user) ++ secondMarkerHelper.findBy(user))
			.distinct
			.flatMap(markingWorkflowService.getExamsUsingMarkingWorkflow)
			.filterNot { e => e.deleted}
	}
}

trait AssessmentServiceUserGroupHelpers {
	val firstMarkerHelper: UserGroupMembershipHelper[MarkingWorkflow]
	val secondMarkerHelper: UserGroupMembershipHelper[MarkingWorkflow]
}

trait AssessmentServiceUserGroupHelpersImpl extends AssessmentServiceUserGroupHelpers {
	val firstMarkerHelper = new UserGroupMembershipHelper[MarkingWorkflow]("_firstMarkers")
	val secondMarkerHelper = new UserGroupMembershipHelper[MarkingWorkflow]("_secondMarkers")
}

@Service(value = "assignmentService")
class AssessmentServiceImpl
	extends AbstractAssessmentService
	with AutowiringAssessmentDaoComponent
	with AutowiringMarkingWorkflowServiceComponent
	with AssessmentServiceUserGroupHelpersImpl

