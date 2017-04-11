package uk.ac.warwick.tabula.web.controllers.groups

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.groups._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventAttendance, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.web.Mav

import scala.collection.JavaConverters._

@RequestMapping(value = Array("/groups/event/{event}/register"))
@Controller
class RecordAttendanceController extends GroupsController with AutowiringSmallGroupServiceComponent {

	validatesSelf[SelfValidating]

	type RecordAttendanceCommand = Appliable[(SmallGroupEventOccurrence, Seq[SmallGroupEventAttendance])]
								   with PopulateOnForm with SmallGroupEventInFutureCheck with RecordAttendanceState
									 with AddAdditionalStudent with RemoveAdditionalStudent

	@ModelAttribute
	def command(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, user: CurrentUser)
		: RecordAttendanceCommand
			= RecordAttendanceCommand(mandatory(event), week, user)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		command.populate()
		form(command)
	}

	def form(command: RecordAttendanceCommand): Mav = {
		Mav("groups/attendance/form",
			"command" -> command,
			"allCheckpointStates" -> AttendanceState.values,
			"eventInFuture" -> command.isFutureEvent,
			"returnTo" -> getReturnTo(Routes.tutor.mygroups))
	}

	@RequestMapping(method = Array(POST), params = Array("action=refresh"))
	def refresh(@ModelAttribute command: RecordAttendanceCommand): Mav = {
		command.addAdditionalStudent(command.members)
		command.doRemoveAdditionalStudent(command.members)

		form(command)
	}

	@RequestMapping(method = Array(POST), params = Array("action!=refresh"))
	def submit(@Valid @ModelAttribute command: RecordAttendanceCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(command)
		} else {
			val (occurrence, _) = command.apply()
			Redirect(Routes.tutor.mygroups, "updatedOccurrence" -> occurrence.id)
		}
	}

	@RequestMapping(value = Array("/additional"))
	def addAdditionalForm(@PathVariable event: SmallGroupEvent, @RequestParam week: Int, @RequestParam student: Member): Mav = {
		case class EventOccurrenceAndState(
			event: SmallGroupEvent,
			week: Int,
			occurrence: Option[SmallGroupEventOccurrence],
			attendance: Option[SmallGroupEventAttendance]
		)

		// Find any groups that the student SHOULD be attending in the same module and academic year
		val possibleReplacements: Seq[EventOccurrenceAndState] =
			smallGroupService.findSmallGroupsByStudent(student.asSsoUser)
				.filter { group => group.groupSet.module == event.group.groupSet.module && group.groupSet.academicYear == event.group.groupSet.academicYear }
				.filterNot { _ == event.group } // No self references!
				.flatMap { group =>
					val occurrences = smallGroupService.findAttendanceByGroup(group)

					group.events.filter { !_.isUnscheduled }.flatMap { event =>
						val allWeeks = event.weekRanges.flatMap { _.toWeeks }
						allWeeks.map { week =>
							val occurrence = occurrences.find { o =>
								o.event == event && o.week == week
							}

							EventOccurrenceAndState(
								event,
								week,
								occurrence,
								occurrence.flatMap { _.attendance.asScala.find { _.universityId == student.universityId }}
							)
						}
					}
				}
				.filterNot { // Can't replace an attended instance
					_.attendance.exists { _.state == AttendanceState.Attended }
				}

		Mav("groups/attendance/additional",
			"student" -> student,
			"possibleReplacements" -> possibleReplacements,
			"week" -> week
		).noLayout()
	}

}