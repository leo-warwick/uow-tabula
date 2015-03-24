package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.coursework.commands.assignments.AssignMarkersCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.web.{Routes => ExamRoutes}
import uk.ac.warwick.tabula.coursework.web.{Routes => CourseworkRoutes}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, UserLookupService}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.AcademicYear

object AssignMarkersController {

	case class Marker(fullName: String, userCode: String, students: Seq[Student])
	case class Student(user: User, module: Module) {
		def userCode: String = user.getUserId
		def displayValue: String = module.adminDepartment.showStudentName match {
			case true => user.getFullName
			case false => user.getWarwickId
		}
		def sortValue = module.adminDepartment.showStudentName match {
			case true => (user.getLastName, user.getFirstName)
			// returning a pair here removes the need to define a custom Ordering implementation
			case false => (user.getWarwickId, user.getWarwickId)
		}
	}

	def retrieveMarkers(
		module: Module,
		assessment: Assessment,
		markers: Seq[String],
		markerMap: Map[String, UserGroup],
		userLookup: UserLookupService
	): Seq[Marker] = {
		markers.map { markerId =>
			val assignedStudents: Seq[Student] = markerMap.get(markerId) match {
				case Some(usergroup: UserGroup) => usergroup.users.map {
					Student(_, module)
				}
				case None => Seq()
			}

			val user = Option(userLookup.getUserByUserId(markerId))
			val fullName = user match {
				case Some(u) => u.getFullName
				case None => ""
			}

			Marker(fullName, markerId, assignedStudents.sortBy(_.sortValue))
		}
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/assign-markers"))
class AssignmentAssignMarkersController extends CourseworkController {

	import AssignMarkersController._

	@Autowired var userLookup: UserLookupService = _
	@Autowired var assessmentMembershipService: AssessmentMembershipService = _

	@ModelAttribute("command")
	def getCommand(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		AssignMarkersCommand(module, assignment)

	@ModelAttribute("firstMarkerRoleName")
	def firstMarkerRoleName(@PathVariable assignment: Assignment): String = assignment.markingWorkflow.firstMarkerRoleName

	@ModelAttribute("secondMarkerRoleName")
	def secondMarkerRoleName(@PathVariable assignment: Assignment): Option[String] = assignment.markingWorkflow.secondMarkerRoleName

	@ModelAttribute("firstMarkers")
	def firstMarkers(@PathVariable module: Module, @PathVariable assignment: Assignment): Seq[Marker] =
		retrieveMarkers(module, assignment, assignment.markingWorkflow.firstMarkers.knownType.members, assignment.firstMarkerMap, userLookup)

	@ModelAttribute("secondMarkers")
	def secondMarkers(@PathVariable module: Module, @PathVariable assignment: Assignment): Seq[Marker] =
		assignment.markingWorkflow.hasSecondMarker match {
			case true => retrieveMarkers(module, assignment, assignment.markingWorkflow.secondMarkers.knownType.members, assignment.secondMarkerMap, userLookup)
			case false => Seq()
		}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Assignment],
		@ModelAttribute("firstMarkers") firstMarkers: Seq[Marker],
		@ModelAttribute("secondMarkers") secondMarkers: Seq[Marker],
		@PathVariable module: Module,
		@PathVariable assignment: Assignment
	) = {
		val members = assessmentMembershipService.determineMembershipUsers(assignment).map{
			new Student(_, module)
		}

		val firstMarkerUnassignedStudents = members.toList.filterNot(firstMarkers.map(_.students).flatten.contains).sortBy(_.sortValue)
		val secondMarkerUnassignedStudents = members.toList.filterNot(secondMarkers.map(_.students).flatten.contains).sortBy(_.sortValue)

		Mav("admin/assignments/assignmarkers/form",
			"assessment" -> assignment,
			"assignMarkersURL" -> CourseworkRoutes.admin.assignment.assignMarkers(assignment),
			"hasSecondMarker" -> assignment.markingWorkflow.hasSecondMarker,
			"firstMarkerUnassignedStudents" -> firstMarkerUnassignedStudents,
			"secondMarkerUnassignedStudents" -> secondMarkerUnassignedStudents,
			"cancelUrl" -> CourseworkRoutes.admin.module(module)
		).crumbs(Breadcrumbs.Department(module.adminDepartment), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(POST), params = Array("!uploadSpreadsheet"))
	def submitChanges(
		@PathVariable module: Module,
		@PathVariable(value = "assignment") assignment: Assignment,
		@ModelAttribute("command") cmd: Appliable[Assignment]
	) = {
		cmd.apply()
		Redirect(CourseworkRoutes.admin.module(module))
	}

	@RequestMapping(method = Array(POST), params = Array("uploadSpreadsheet"))
	def doUpload(@PathVariable module: Module,
							 @PathVariable(value = "assignment") assignment: Assignment,
							 @ModelAttribute("command") cmd: Appliable[Assignment],
							 errors: Errors) = {
			Mav("admin/assignments/assignmarkers/upload-preview",
				"firstMarkerRole" -> assignment.markingWorkflow.firstMarkerRoleName,
				"secondMarkerRole" -> assignment.markingWorkflow.secondMarkerRoleName.getOrElse("Second marker")
			)
	}

}

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/{exam}/assign-markers"))
class ExamAssignMarkersController extends ExamsController {

	import AssignMarkersController._

	@Autowired var userLookup: UserLookupService = _
	@Autowired var assessmentMembershipService: AssessmentMembershipService = _

	@ModelAttribute("command")
	def getCommand(@PathVariable module: Module, @PathVariable exam: Exam) =
		AssignMarkersCommand(module, exam)

	@ModelAttribute("firstMarkerRoleName")
	def firstMarkerRoleName(@PathVariable exam: Exam): String = exam.markingWorkflow.firstMarkerRoleName

	@ModelAttribute("secondMarkerRoleName")
	def secondMarkerRoleName(@PathVariable exam: Exam): Option[String] = exam.markingWorkflow.secondMarkerRoleName

	@ModelAttribute("firstMarkers")
	def firstMarkers(@PathVariable module: Module, @PathVariable exam: Exam): Seq[Marker] =
		retrieveMarkers(module, exam, exam.markingWorkflow.firstMarkers.knownType.members, exam.firstMarkerMap, userLookup)

	@ModelAttribute("secondMarkers")
	def secondMarkers(@PathVariable module: Module, @PathVariable exam: Exam): Seq[Marker] =
		exam.markingWorkflow.hasSecondMarker match {
			case true => retrieveMarkers(module, exam, exam.markingWorkflow.secondMarkers.knownType.members, exam.secondMarkerMap, userLookup)
			case false => Seq()
		}



	@RequestMapping(method = Array(GET))
	def form(
						@ModelAttribute("command") cmd: Appliable[Exam],
						@ModelAttribute("firstMarkers") firstMarkers: Seq[Marker],
						@ModelAttribute("secondMarkers") secondMarkers: Seq[Marker],
						@PathVariable module: Module,
						@PathVariable exam: Exam,
						@PathVariable academicYear: AcademicYear
						) = {
		val members = assessmentMembershipService.determineMembershipUsers(exam).map{
			new Student(_, module)
		}

		val firstMarkerUnassignedStudents = members.toList.filterNot(firstMarkers.map(_.students).flatten.contains).sortBy(_.sortValue)
		val secondMarkerUnassignedStudents = members.toList.filterNot(secondMarkers.map(_.students).flatten.contains).sortBy(_.sortValue)

		Mav("admin/assignments/assignmarkers/form",
			"assessment" -> exam,
			"assignMarkersURL" -> ExamRoutes.admin.exam.assignMarkers(exam),
			"hasSecondMarker" -> exam.markingWorkflow.hasSecondMarker,
			"firstMarkerUnassignedStudents" -> firstMarkerUnassignedStudents,
			"secondMarkerUnassignedStudents" -> secondMarkerUnassignedStudents,
			"cancelUrl" -> ExamRoutes.admin.module(module, academicYear)
		).crumbs(Breadcrumbs.Department(module.adminDepartment, exam.academicYear), Breadcrumbs.Module(module, exam.academicYear))
	}

	@RequestMapping(method = Array(POST), params = Array("!uploadSpreadsheet"))
	def submitChanges(
										 @PathVariable module: Module,
										 @PathVariable(value = "exam") exam: Exam,
										 @ModelAttribute("command") cmd: Appliable[Exam]
										 ) = {
		cmd.apply()
		Redirect(ExamRoutes.admin.module(module, exam.academicYear))
	}

	@RequestMapping(method = Array(POST), params = Array("uploadSpreadsheet"))
	def doUpload(@PathVariable module: Module,
							 @PathVariable(value = "exam") exam: Exam,
							 @ModelAttribute("command") cmd: Appliable[Exam],
							 errors: Errors) = {
		Mav("admin/assignments/assignmarkers/upload-preview",
			"firstMarkerRole" -> exam.markingWorkflow.firstMarkerRoleName,
			"secondMarkerRole" -> exam.markingWorkflow.secondMarkerRoleName.getOrElse("Second marker")
		)
	}

}