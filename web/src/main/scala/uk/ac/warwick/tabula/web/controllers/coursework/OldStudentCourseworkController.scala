package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.commands.coursework.assignments.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseYearDetails}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.{Appliable, MemberOrUser}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.coursework.assignments.{StudentCourseworkFullScreenCommand, StudentCourseworkGadgetCommand}

abstract class OldStudentCourseworkController extends OldCourseworkController {
	def getMav(member: Member, info: StudentAssignments): Mav =
		Mav(s"$urlPrefix/home/_student",
			"student" -> member,
			"enrolledAssignments" -> info.enrolledAssignments,
			"historicAssignments" -> info.historicAssignments,
			"isSelf" -> (member.universityId == user.universityId),
			"ajax" -> ajax
		).noLayoutIf(ajax)
}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/student/{member}"))
class OldStudentCourseworkFullScreenController extends OldStudentCourseworkController {

	@ModelAttribute("command") def command(@PathVariable member: Member) =
		StudentCourseworkFullScreenCommand(MemberOrUser(member))

	@RequestMapping
	def listAssignments(@ModelAttribute("command") command: Appliable[StudentAssignments], @PathVariable member: Member, user: CurrentUser): Mav =
		getMav(member, command.apply())

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/student/bycourseandyear/{studentCourseYearDetails}"))
class OldStudentCourseworkGadgetController extends OldStudentCourseworkController {

	@ModelAttribute("command") def command(@PathVariable studentCourseYearDetails: StudentCourseYearDetails) =
		StudentCourseworkGadgetCommand(mandatory(studentCourseYearDetails))

	@RequestMapping
	def listAssignments(
		@ModelAttribute("command") command: Appliable[StudentAssignments],
		@PathVariable studentCourseYearDetails: StudentCourseYearDetails,
		user: CurrentUser
	): Mav =
		getMav(studentCourseYearDetails.studentCourseDetails.student, command.apply())

}