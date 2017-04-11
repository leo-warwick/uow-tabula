package uk.ac.warwick.tabula.web.controllers.attendance.check

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.check.{CheckEventAttendanceCheckpointsCommand, CheckpointResult}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/attendance/check/smallgroup", "/attendance/smallgroupcheckpoints"))
class CheckEventAttendanceCheckpointsController extends AttendanceController {

	@ModelAttribute("command")
	def command(@RequestParam occurrence: SmallGroupEventOccurrence) =
		CheckEventAttendanceCheckpointsCommand(mandatory(occurrence))

	@RequestMapping(method = Array(POST))
	def home(@ModelAttribute("command") cmd: Appliable[CheckpointResult]): Mav = {
		val CheckpointResult(attended, missedUnauthorised, missedAuthorised) = cmd.apply()

		def transform(students: Seq[StudentMember]): Seq[Map[String, String]] =
			students.sortBy(s => (s.lastName, s.firstName, s.universityId)).map(student => Map(
				"name" -> student.fullName.getOrElse(student.universityId),
				"universityId" -> student.universityId
			))

		Mav(new JSONView(Map(
			"attended" -> transform(attended),
			"missedUnauthorised" -> transform(missedUnauthorised),
			"missedAuthorised" -> transform(missedAuthorised)
		)))
	}
}
