package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.timetables._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.services.timetables.AutowiringModuleTimetableEventSourceComponent
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.PDFView

@Controller
@RequestMapping(Array("/profiles/department/{department}/timetables/download/{academicYear}"))
class DownloadDepartmentTimetableController extends ProfilesController
	with AutowiringModuleTimetableEventSourceComponent with DownloadsTimetable {

	@ModelAttribute("timetableCommand")
	def timetableCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): DepartmentTimetableCommand.CommandType = {
		DepartmentTimetableCommand(
			mandatory(department),
			academicYear,
			user,
			new ViewModuleTimetableCommandFactoryImpl(moduleTimetableEventSource),
			new ViewStudentMemberTimetableCommandFactoryImpl(user),
			new ViewStaffMemberTimetableCommandFactoryImpl(user)
		)
	}

	@RequestMapping
	def render(
		@ModelAttribute("timetableCommand") cmd: DepartmentTimetableCommand.CommandType,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): PDFView = {
		getTimetable(
			events = cmd.apply()._1.events,
			academicYear = academicYear,
			fileNameSuffix = department.code,
			title = s"${department.name} for ${academicYear.toString}"
		)
	}

}
