package uk.ac.warwick.tabula.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.ViewExamTimetableCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.timetables.ExamTimetableFetchingService
import uk.ac.warwick.tabula.web.Mav

import scala.util.{Failure, Success, Try}

@Controller
@RequestMapping(Array("/profiles/view/{member}/exams"))
class ViewExamTimetableController extends ProfilesController {

	@ModelAttribute("command")
	def command(@PathVariable member: Member) = ViewExamTimetableCommand(mandatory(member), user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Try[ExamTimetableFetchingService.ExamTimetable]]): Mav = {
		cmd.apply() match {
			case Success(examTimetable) => Mav("profiles/timetables/exams",
				"timetable" -> examTimetable
			)
			case Failure(t) => Mav("profiles/timetables/exams",
				"error" -> t.getMessage
			)
		}

	}

}
