package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, RequestMapping}
import uk.ac.warwick.tabula.web.Mav
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.web.views.JSONView
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.{AssessmentMembershipInfo, AssessmentService}
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(Array("/sysadmin/event-calendar"))
class SystemEventCalendarController extends BaseSysadminController {

	var assignmentService: AssessmentService = Wire[AssessmentService]

	@RequestMapping def view = Mav("sysadmin/event-calendar")

	@RequestMapping(value = Array("/events"))
	def getEvents(@RequestParam from: LocalDate, @RequestParam to: LocalDate): Mav = {
		val start = from.toDateTimeAtStartOfDay
		val end = to.toDateTimeAtStartOfDay

		val assignments =
			assignmentService.getAssignmentsClosingBetween(start, end)
				.map { assignment => (assignment, assignment.membershipInfo) }
				.filter { case (_, membershipInfo) => membershipInfo.totalCount > 0 }

		val calendarEvents =
			assignments.map { case (assignment, membershipInfo) => FullCalendarEvent(assignment, membershipInfo) }

		val allDayEvents =
			assignments.groupBy { case (assignment, _) => assignment.closeDate.toLocalDate }
				.toSeq
				.map { case (date, assignments) =>
					def count(s: Seq[(Assignment, AssessmentMembershipInfo)]) = s.foldLeft(0) { case (acc, (_, membershipInfo)) => acc + membershipInfo.totalCount }
					val totalCount = count(assignments)

					val countsByDepartment =
						assignments.groupBy { case (assignment, _) => assignment.module.adminDepartment.rootDepartment }
							.map { case (department, assignments) =>
								(department, assignments.length, count(assignments))
							}

					val shortTimeFormat = DateTimeFormat.shortTime()
					FullCalendarEvent(
						title = s"${assignments.length} assignments, ${totalCount} students",
						allDay = true,
						start = date.toDateTimeAtStartOfDay.getMillis / 1000,
						end = date.toDateTimeAtStartOfDay.getMillis / 1000,
						formattedCloseDate = shortTimeFormat.print(date),
						description =
							countsByDepartment.map { case (department, assignmentCount, studentCount) =>
								s"${department.name}: ${assignmentCount} assignments, ${studentCount} students"
							}.mkString("\n\n"),
						context = ""
					)
				}

		Mav(new JSONView(colourEvents(allDayEvents ++ calendarEvents)))
	}

	def colourEvents(uncoloured: Seq[FullCalendarEvent]):Seq[FullCalendarEvent] = {
		val colours = Seq(
			"#239b92", "#a3b139", "#ec8d22", "#ef3e36", "#df4094", "#4daacc", "#167ec2", "#f1592a", "#818285",
			"#bacecc", "#f5c6df", "#dae0b0", "#f9ddbd", "#fffa99", "#cae6f0", "#d8d8d8", "#ecf7f7",
			"#8bada9", "#ef9fc9", "#d1d89c", "#f5c690", "#fff87f", "#a6d4e5", "#c4c4c5", "#d2e7e3"
		)
		// an infinitely repeating stream of colours
		val colourStream = Stream.continually(colours.toStream).flatten
		val contexts = uncoloured.map(_.context).distinct
		val contextsWithColours = contexts.zip(colourStream)
		uncoloured.map { event =>
			if (event.title == "Busy") { // FIXME hack
				event.copy(backgroundColor = "#bbb", borderColor = "#bbb")
			} else {
				val colour = contextsWithColours.find(_._1 == event.context).get._2
				event.copy(backgroundColor = colour, borderColor = colour)
			}
		}
	}

}

/**
 * serialises to the JSON which FullCalendar likes.
 *
 * Note: start and end are *Seconds* since the epoch, not milliseconds!
 */
case class FullCalendarEvent(
	title: String,
	allDay: Boolean,
	start: Long,
	end: Long,
	backgroundColor: String="#4daacc", // tabulaBlueLight.
	borderColor: String="#4daacc",
	textColor: String="#000",
	// fields below here are not used by FullCalendar itself, they're custom fields
	// for use in the renderEvent callback
	formattedCloseDate: String,
	description: String = "",
	context: String = ""
)

object FullCalendarEvent {
	def apply(assignment: Assignment, membershipInfo: AssessmentMembershipInfo): FullCalendarEvent = {
		val shortTimeFormat = DateTimeFormat.shortTime()
		FullCalendarEvent(
			title = s"[${membershipInfo.totalCount}] ${assignment.module.code.toUpperCase} ${assignment.name}",
			allDay = false,
			start = assignment.closeDate.getMillis / 1000,
			end = assignment.closeDate.getMillis / 1000,
			formattedCloseDate = shortTimeFormat.print(assignment.closeDate),
			description = s"${membershipInfo.totalCount} enrolled students (${membershipInfo.sitsCount} from SITS${if (membershipInfo.usedExcludeCount > 0) s"after ${membershipInfo.usedExcludeCount} removed manually" else "" }${if (membershipInfo.usedIncludeCount > 0) s", plus ${membershipInfo.usedIncludeCount} added manually" else ""})",
			context = assignment.module.adminDepartment.rootDepartment.name
		)
	}
}