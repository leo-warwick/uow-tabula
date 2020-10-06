package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.timetables.ViewMemberTimetableCommand
import uk.ac.warwick.tabula.commands.timetables.ViewMemberTimetableCommand.TimetableCommand
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.PDFView
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, RequestFailedException}

import scala.util.{Failure, Success}

@Controller
@RequestMapping(Array("/profiles/view/{member}/timetable/download/{academicYear}"))
class DownloadTimetableController extends ProfilesController with TaskBenchmarking with DownloadsTimetable {

  @ModelAttribute("timetableCommand")
  def timetableCommand(@PathVariable member: Member, currentUser: CurrentUser) =
    ViewMemberTimetableCommand(mandatory(member), currentUser)

  @RequestMapping
  def render(
    @ModelAttribute("timetableCommand") cmd: TimetableCommand,
    @PathVariable member: Member,
    @PathVariable academicYear: AcademicYear
  ): PDFView = {
    cmd.academicYear = academicYear

    cmd.apply() match {
      case Success(result) =>
        getTimetable(
          events = result.events,
          academicYear = academicYear,
          fileNameSuffix = member.universityId,
          title = s"${member.fullName.getOrElse("")} (${member.universityId}) for ${academicYear.toString}"
        )
      case Failure(t) =>
        logger.error("Couldn't generate member timetable for PDF", t)
        throw new RequestFailedException("The timetabling service could not be reached", t)
    }
  }

}
