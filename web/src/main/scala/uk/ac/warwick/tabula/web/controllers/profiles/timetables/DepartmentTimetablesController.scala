package uk.ac.warwick.tabula.web.controllers.profiles.timetables

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.CurrentAcademicYear
import uk.ac.warwick.tabula.commands.timetables.{DepartmentEventsCommand, ViewModuleTimetableCommandFactoryImpl, ViewStaffMemberEventsCommandFactoryImpl, ViewStudentMemberEventsCommandFactoryImpl}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.helpers.SystemClockComponent
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{FullCalendarEvent, JSONView}

@Controller
@RequestMapping(Array("/profiles/department/{department}/timetables"))
class DepartmentTimetablesController extends ProfilesController
  with CurrentAcademicYear with AutowiringModuleTimetableEventSourceComponent
  with AutowiringUserLookupComponent with AutowiringScientiaConfigurationComponent
  with SystemClockComponent {

  @ModelAttribute("activeDepartment")
  def activeDepartment(@PathVariable department: Department): Department = department

  @ModelAttribute("command")
  def command(@PathVariable department: Department): DepartmentEventsCommand.CommandType = {
    DepartmentEventsCommand(
      mandatory(department),
      academicYear,
      user,
      new ViewModuleTimetableCommandFactoryImpl(moduleTimetableEventSource),
      new ViewStudentMemberEventsCommandFactoryImpl(user),
      new ViewStaffMemberEventsCommandFactoryImpl(user)
    )
  }

  @RequestMapping(method = Array(GET))
  def form(@ModelAttribute("command") cmd: DepartmentEventsCommand.CommandType, @PathVariable department: Department): Mav = {
    Mav("profiles/timetables/department",
      "academicYears" -> scientiaConfiguration.academicYears,
      "canFilterStudents" -> securityService.can(user, DepartmentEventsCommand.FilterStudentPermission, mandatory(department)),
      "canFilterStaff" -> securityService.can(user, DepartmentEventsCommand.FilterStaffPermission, mandatory(department)),
      "canFilterRoute" -> true,
      "canFilterYearOfStudy" -> true
    )
  }

  @RequestMapping(method = Array(POST))
  def post(
    @ModelAttribute("command") cmd: DepartmentEventsCommand.CommandType,
    @PathVariable department: Department
  ): Mav = {
    val result = cmd.apply()
    val calendarEvents = FullCalendarEvent.colourEvents(result._1.events.map(FullCalendarEvent(_, userLookup)))
    Mav(new JSONView(Map("events" -> calendarEvents, "lastUpdated" -> result._1.lastUpdated, "errors" -> result._2)))
  }

}
