package uk.ac.warwick.tabula.commands.attendance.profile

import org.joda.time.{DateTime, Interval}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.profile.ViewSmallGroupsForPointCommandResult.GroupData
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.SmallGroupAttendanceState.{Late, MissedAuthorised, MissedUnauthorised, NotRecorded}
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand._
import uk.ac.warwick.tabula.commands.groups.{SmallGroupAttendanceState, StudentGroupAttendance}
import uk.ac.warwick.tabula.data.model.{AttendanceNote, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringTermServiceComponent, SmallGroupServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.immutable.SortedMap

object ViewSmallGroupsForPointCommandResult {
	case class Course(
		name: String,
		route: String,
		department: String,
		status: String,
		attendance: String,
		courseType: String,
		yearOfStudy: String
	)

	case class Module(
		hasGroups: Boolean,
		code: String,
		title: String,
		department: String,
		cats: String,
		status: String
	)

	case class GroupData(terms: Seq[GroupData.Term])
	object GroupData {
		case class Term(
			name: String,
			weeks: Seq[(Int, Boolean)],
			groups: Seq[Term.Group]
		)
		object Term {
			case class Group(name: String, relevant: Boolean, attendance: Map[Int, Seq[Group.Attendance]])
			object Group {
				case class Attendance(instance: EventInstance, state: SmallGroupAttendanceState, note: Option[AttendanceNote], reason: String, relevant: Boolean)
			}
		}
	}

}

case class ViewSmallGroupsForPointCommandResult(
	course: ViewSmallGroupsForPointCommandResult.Course,
	modules: Seq[ViewSmallGroupsForPointCommandResult.Module],
	groupData: ViewSmallGroupsForPointCommandResult.GroupData
)

object ViewSmallGroupsForPointCommand {
	def apply(student: StudentMember, point: AttendanceMonitoringPoint, attendance: StudentGroupAttendance) =
		new ViewSmallGroupsForPointCommandInternal(student, point, attendance)
			with AutowiringSmallGroupServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[ViewSmallGroupsForPointCommandResult]
			with ViewSmallGroupsForPointPermissions
			with ViewSmallGroupsForPointCommandState
			with ReadOnly with Unaudited
}


class ViewSmallGroupsForPointCommandInternal(val student: StudentMember, val point: AttendanceMonitoringPoint, val attendance: StudentGroupAttendance)
	extends CommandInternal[ViewSmallGroupsForPointCommandResult] {

	self: SmallGroupServiceComponent with TermServiceComponent =>

	type EventInstance = (SmallGroupEvent, SmallGroupEventOccurrence.WeekNumber)

	lazy val weeksForYear: Map[Integer, Interval] = termService.getAcademicWeeksForYear(point.scheme.academicYear.dateInTermOne).toMap

	def weekNumberToDate(weekNumber: Int, dayOfWeek: DayOfWeek): DateTime =
		weeksForYear(weekNumber).getStart.withDayOfWeek(dayOfWeek.jodaDayOfWeek)

	override def applyInternal(): ViewSmallGroupsForPointCommandResult = {
		ViewSmallGroupsForPointCommandResult(
			courseData,
			moduleData,
			groupData
		)
	}

	private def courseData = {
		student.mostSignificantCourseDetails match {
			case None => ViewSmallGroupsForPointCommandResult.Course("","",student.homeDepartment.name,"","","","")
			case Some(scd) =>
				ViewSmallGroupsForPointCommandResult.Course(
					student.mostSignificantCourseDetails.map(scd => scd.course.name).getOrElse(""),
					student.mostSignificantCourseDetails.map(scd => s"${scd.currentRoute.name} (${scd.currentRoute.code.toUpperCase})").getOrElse(""),
					student.homeDepartment.name,
					student.mostSignificantCourseDetails.map(scd => scd.statusOnRoute.fullName.toLowerCase.capitalize).getOrElse(""),
					student.mostSignificantCourseDetails.map(scd => scd.latestStudentCourseYearDetails.modeOfAttendance.fullNameAliased).getOrElse(""),
					student.mostSignificantCourseDetails.map(scd => scd.currentRoute.degreeType.toString).getOrElse(""),
					student.mostSignificantCourseDetails.map(scd => scd.latestStudentCourseYearDetails.yearOfStudy.toString).getOrElse("")
				)
		}
	}

	private def moduleData = {
		student.mostSignificantCourseDetails.flatMap(scd =>
			scd.freshStudentCourseYearDetails.find(_.academicYear == point.scheme.academicYear).map(scyd =>
				scyd.moduleRegistrations.map{moduleRegistration =>
					ViewSmallGroupsForPointCommandResult.Module(
						smallGroupService.hasSmallGroups(moduleRegistration.module, moduleRegistration.academicYear),
						moduleRegistration.module.code.toUpperCase,
						moduleRegistration.module.name,
						moduleRegistration.module.adminDepartment.name,
						moduleRegistration.cats.toString,
						Option(moduleRegistration.selectionStatus).map(_.description).getOrElse("")
					)
				}
			)
		).getOrElse(Seq())
	}

	private def groupData = {
		// Any day in this week is definitely before the start date of the point
		val weekBeforePoint = termService.getAcademicWeekForAcademicYear(point.startDate.toDateTimeAtStartOfDay, point.scheme.academicYear) - 1
		// Any day in this week is definitely after the end date of the point
		val weekAfterPoint = termService.getAcademicWeekForAcademicYear(point.endDate.toDateTimeAtStartOfDay, point.scheme.academicYear) + 1

		ViewSmallGroupsForPointCommandResult.GroupData(
			attendance.attendance.map{case(term, groupAttendance) =>
				val termWeekBoundaries = attendance.termWeeks(term)
				val termWeeks = termWeekBoundaries.minWeek to termWeekBoundaries.maxWeek
				GroupData.Term(
					term.getTermTypeAsString,
					termWeeks.map(week => (week, week > weekBeforePoint && week < weekAfterPoint)),
					groupAttendance.map{case(group, attendanceMap) =>
						GroupData.Term.Group(
							group.groupSet.module.code.toUpperCase,
							point.smallGroupEventModules.isEmpty || point.smallGroupEventModules.contains(group.groupSet.module),
							termWeeks.map{week => week -> (attendanceMap.get(week) match {
								case None => Seq(GroupData.Term.Group.Attendance(null, null, None, "No event in this week", relevant = false))
								case Some(instanceMap) => checkRelevance(instanceMap, group.groupSet.academicYear, attendance.notes)
							})}.toMap
						)
					}.toSeq
				)
			}.toSeq
		)
	}

	private def checkRelevance(
		instanceMap: SortedMap[EventInstance, SmallGroupAttendanceState],
		academicYear: AcademicYear,
		notes: Map[EventInstance, SmallGroupEventAttendanceNote]
	): Seq[GroupData.Term.Group.Attendance] = {
		// Check each possible reason for not counting for each instance
		instanceMap.keys.map{ case(event, week) =>
			val instance = (event, week)
			val state = instanceMap(instance)
			val instanceDate = weekNumberToDate(week, event.day)
			if (instanceDate.isBefore(point.startDate.toDateTimeAtStartOfDay)) {
				GroupData.Term.Group.Attendance(instance, state, notes.get(instance), "This event took place before the monitoring period", relevant = false)
			} else if (instanceDate.isAfter(point.endDate.toDateTimeAtStartOfDay)) {
				GroupData.Term.Group.Attendance(instance, state, notes.get(instance), "This event took place after the monitoring period", relevant = false)
			} else {
				state match {
					case MissedUnauthorised =>
						GroupData.Term.Group.Attendance(instance, state, notes.get(instance), "Marked absent (unauthorised) for this event", relevant = true)
					case MissedAuthorised =>
						GroupData.Term.Group.Attendance(instance, state, notes.get(instance), "Marked absent (authorised) for this event", relevant = true)
					case NotRecorded | Late =>
						GroupData.Term.Group.Attendance(instance, state, notes.get(instance), "Attendance has not been recorded for this event", relevant = true)
					case _ =>
						GroupData.Term.Group.Attendance((event, week), state, notes.get(instance), "", relevant = true)
				}
			}
		}.toSeq
	}
}

trait ViewSmallGroupsForPointPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewSmallGroupsForPointCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}

}

trait ViewSmallGroupsForPointCommandState {
	def student: StudentMember
	def point: AttendanceMonitoringPoint
	def attendance: StudentGroupAttendance
}
