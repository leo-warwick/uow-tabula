package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.groups.{ListStudentGroupAttendanceCommand, StudentGroupAttendance}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat.Example
import uk.ac.warwick.tabula.web.Mav

/**
 * Displays a student's attendance at small groups.
 */
@Controller
@RequestMapping(Array("/groups/student/{member}/attendance/{academicYear}"))
class StudentGroupAttendanceController extends GroupsController {

	@ModelAttribute("command") def command(@PathVariable member: Member, @PathVariable academicYear: AcademicYear) =
		ListStudentGroupAttendanceCommand(member, academicYear)

	@RequestMapping
	def showAttendance(@ModelAttribute("command") cmd: Appliable[StudentGroupAttendance], @PathVariable member: Member): Mav = {
		val info = cmd.apply()
		Mav("groups/students_group_attendance",
			"seminarAttendanceCommandResult" -> info,
			"student" -> member,
			"isSelf" -> (user.universityId.maybeText.getOrElse("") == member.universityId),
			"defaultExpand" -> !ajax
		).noLayoutIf(ajax)
	}

}
