package uk.ac.warwick.tabula.web.controllers.exams.exams.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.AssignMarkersCommand
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, UserLookupService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/exams/exams/admin/module/{module}/{academicYear}/exams/{exam}/assign-markers"))
class ExamAssignMarkersController extends ExamsController {

	@Autowired var userLookup: UserLookupService = _
	@Autowired var assessmentMembershipService: AssessmentMembershipService = _

	case class ExamStudent(user: User, seatNumber: Option[Int], module: Module) {
		def userCode: String = user.getUserId
		def displayValue: String = module.adminDepartment.showStudentName match {
			case true => user.getFullName
			case false => user.getWarwickId
		}
		def sortValue: (Int, Int) = seatNumber.map(seat => (seat, seat)).getOrElse((100000,100000))
	}
	case class Marker(fullName: String, userCode: String, students: Seq[ExamStudent])

	@ModelAttribute("command")
	def getCommand(@PathVariable module: Module, @PathVariable exam: Exam) =
		AssignMarkersCommand(module, exam)

	@ModelAttribute("firstMarkerRoleName")
	def firstMarkerRoleName(@PathVariable exam: Exam): String = exam.markingWorkflow.firstMarkerRoleName

	@ModelAttribute("firstMarkers")
	def firstMarkers(@PathVariable module: Module, @PathVariable exam: Exam): Seq[Marker] = {
		val allMembersMap = assessmentMembershipService.determineMembershipUsersWithOrder(exam).toMap
		exam.markingWorkflow.firstMarkers.knownType.members.map { markerId =>
			val assignedStudents: Seq[ExamStudent] = exam.firstMarkerMap.get(markerId).map(group =>
				group.users.map(student => ExamStudent(student, allMembersMap.get(student).flatten, module))
			).getOrElse(Seq())
			val user = Option(userLookup.getUserByUserId(markerId))
			val fullName = user match {
				case Some(u) => u.getFullName
				case None => ""
			}
			Marker(fullName, markerId, assignedStudents.sortBy(_.sortValue))
		}
	}

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Exam],
		@ModelAttribute("firstMarkers") firstMarkers: Seq[Marker],
		@PathVariable module: Module,
		@PathVariable exam: Exam,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val members = assessmentMembershipService.determineMembershipUsersWithOrder(exam).map{ case(user, seatOrder) =>
			new ExamStudent(user, seatOrder, module)
		}

		val firstMarkerUnassignedStudents = members.toList.filterNot(firstMarkers.flatMap(_.students).contains)

		Mav("exams/exams/admin/assignmarkers/form",
			"assessment" -> exam,
			"isExam" -> true,
			"assignMarkersURL" -> Routes.Exams.admin.exam.assignMarkers(exam),
			"hasSecondMarker" -> false,
			"firstMarkerUnassignedStudents" -> firstMarkerUnassignedStudents,
			"cancelUrl" -> Routes.Exams.admin.module(module, academicYear)
		).crumbs(
			Breadcrumbs.Exams.Home,
			Breadcrumbs.Exams.Department(module.adminDepartment, exam.academicYear),
			Breadcrumbs.Exams.Module(module, exam.academicYear)
		)
	}

	@RequestMapping(method = Array(POST), params = Array("!uploadSpreadsheet"))
	def submitChanges(
		@PathVariable module: Module,
		@PathVariable(value = "exam") exam: Exam,
		@ModelAttribute("command") cmd: Appliable[Exam]
	): Mav = {
		cmd.apply()
		Redirect(Routes.Exams.admin.module(module, exam.academicYear))
	}

	@RequestMapping(method = Array(POST), params = Array("uploadSpreadsheet"))
	def doUpload(
		@PathVariable module: Module,
		@PathVariable(value = "exam") exam: Exam,
		@PathVariable academicYear: AcademicYear,
		@ModelAttribute("command") cmd: Appliable[Exam],
		errors: Errors
	): Mav = {
		Mav("exams/exams/admin/assignmarkers/upload-preview",
			"assessment" -> exam,
			"isExam" -> true,
			"assignMarkersURL" -> Routes.Exams.admin.exam.assignMarkers(exam),
			"firstMarkerRole" -> exam.markingWorkflow.firstMarkerRoleName,
			"cancelUrl" -> Routes.Exams.admin.module(module, academicYear)
		)
	}

}
