package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupEventAttendance, SmallGroupEventAttendanceNote, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.data.model.{AbsenceType, Module, StudentMember}
import uk.ac.warwick.tabula.services._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

trait AttendanceMonitoringEventAttendanceServiceComponent {
	def attendanceMonitoringEventAttendanceService: AttendanceMonitoringEventAttendanceService
}

trait AutowiringAttendanceMonitoringEventAttendanceServiceComponent extends AttendanceMonitoringEventAttendanceServiceComponent {
	val attendanceMonitoringEventAttendanceService: AttendanceMonitoringEventAttendanceService = Wire[AttendanceMonitoringEventAttendanceService]
}

trait AttendanceMonitoringEventAttendanceService {
	def getCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[AttendanceMonitoringCheckpoint]
	def updateCheckpoints(attendances: Seq[SmallGroupEventAttendance]): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
	def getMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[(AttendanceMonitoringCheckpoint, Seq[SmallGroupEventAttendanceNote])]
	def updateMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance], user: CurrentUser): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
}

abstract class AbstractAttendanceMonitoringEventAttendanceService extends AttendanceMonitoringEventAttendanceService {

	self: ProfileServiceComponent with AttendanceMonitoringServiceComponent with SmallGroupServiceComponent with TermServiceComponent =>

	def getCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[AttendanceMonitoringCheckpoint] = {
		attendances.filter(a => a.state == AttendanceState.Attended && a.occurrence.event.day != null).flatMap(attendance => {
			profileService.getMemberByUniversityId(attendance.universityId).flatMap{
				case studentMember: StudentMember =>
					val relevantPoints = getRelevantPoints(
						attendanceMonitoringService.listStudentsPoints(studentMember, None, attendance.occurrence.event.group.groupSet.academicYear),
						attendance,
						studentMember
					)
					val checkpoints = relevantPoints.filter(point => checkQuantity(point, attendance, studentMember)).map(point => {
						val checkpoint = new AttendanceMonitoringCheckpoint
						checkpoint.autoCreated = true
						checkpoint.point = point
						checkpoint.attendanceMonitoringService = attendanceMonitoringService
						checkpoint.student = studentMember
						checkpoint.updatedBy = attendance.updatedBy
						checkpoint.updatedDate = DateTime.now
						checkpoint.state = AttendanceState.Attended
						checkpoint
					})
					Option(checkpoints)
				case _ => None
			}
		}).flatten
	}

	def updateCheckpoints(attendances: Seq[SmallGroupEventAttendance]): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		getCheckpoints(attendances).map(checkpoint => {
			attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
		})
	}.foldLeft(
		(Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())
	){
		case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
	}

	def getMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance]): Seq[(AttendanceMonitoringCheckpoint, Seq[SmallGroupEventAttendanceNote])] = {
		attendances.filter(a => (a.state == AttendanceState.MissedAuthorised || a.state == AttendanceState.MissedUnauthorised) && a.occurrence.event.day != null).flatMap(attendance => {
			profileService.getMemberByUniversityId(attendance.universityId).flatMap {
				case studentMember: StudentMember =>
					// all AttendanceMonitoringpoints applicable for this student
					val relevantPoints = getRelevantPoints(
						attendanceMonitoringService.listStudentsPoints(studentMember, None, attendance.occurrence.event.group.groupSet.academicYear),
						attendance,
						studentMember
					)
					// check among applicable AttendanceMonitoringpoints, which ones this student can't meet if we have marked this event as missed
					val missedPoints =  relevantPoints.filter(point => checkMonitoringPointRequirementsMissed(point, attendance, studentMember))
					val checkpoints = missedPoints.map(point => {
						val checkpoint = new AttendanceMonitoringCheckpoint
						checkpoint.autoCreated = true
						checkpoint.point = point
						checkpoint.attendanceMonitoringService = attendanceMonitoringService
						checkpoint.student = studentMember
						checkpoint.updatedBy = attendance.updatedBy
						checkpoint.updatedDate = DateTime.now
						checkpoint.state = attendance.state
						val occurrences = findEventOccurrencesForDates(
							point.startDate,
							point.endDate,
							attendance.occurrence.event.group.groupSet.academicYear,
							studentMember, point.smallGroupEventModules
						)
						val eventNotes = smallGroupService.findAttendanceNotes(Seq(checkpoint.student.universityId), occurrences)
						(checkpoint,eventNotes)
					})
					Option(checkpoints)
				case _ => None
			}
		}).flatten
	}


	def updateMissedCheckpoints(attendances: Seq[SmallGroupEventAttendance], user: CurrentUser): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		getMissedCheckpoints(attendances).map { case (checkpoint, eventNotes) =>
			val eventNoteContents = eventNotes.map(note => s"[Reference Id- ${note.occurrence.id}, Event - ${note.occurrence.event.title}, Details: ${note.note}] ")

			if (eventNoteContents.nonEmpty) {
				val monitoringNote =  attendanceMonitoringService.getAttendanceNote(checkpoint.student, checkpoint.point)
				val attendanceNote = monitoringNote.getOrElse({
					val newNote = new AttendanceMonitoringNote
					newNote.student = checkpoint.student
					newNote.point = checkpoint.point
					newNote.absenceType = AbsenceType.Other
					newNote
				})
				val newNoteSummary = s"\nSummary Of Small Group Event Note Details:  ${eventNoteContents.mkString(",")}"
				if (attendanceNote.note.hasText)
					attendanceNote.note = s" ${attendanceNote.note} $newNoteSummary"
				else
					attendanceNote.note = s" $newNoteSummary"

				attendanceNote.updatedBy = user.userId
				attendanceNote.updatedDate = DateTime.now
				attendanceMonitoringService.saveOrUpdate(attendanceNote)
			}
			attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
		}.foldLeft(
			(Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())
		){
			case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
		}
	}

	private def getRelevantPoints(points: Seq[AttendanceMonitoringPoint], attendance: SmallGroupEventAttendance, studentMember: StudentMember): Seq[AttendanceMonitoringPoint] = {
		val eventDateOption = attendance.occurrence.date
		eventDateOption.map(eventDate =>
			points.filter(point =>
				// Is it the correct type
				point.pointType == AttendanceMonitoringPointType.SmallGroup
					// Is the attendance inside the point's weeks
					&& point.isDateValidForPoint(eventDate)
					// Is the group's module valid
					&& (point.smallGroupEventModules.isEmpty || point.smallGroupEventModules.contains(attendance.occurrence.event.group.groupSet.module))
					// Is there no existing checkpoint
					&& noExistingCheckpoint(point, attendance, studentMember)
					// The student hasn't been sent to SITS for this point
					&& !attendanceMonitoringService.studentAlreadyReportedThisTerm(studentMember, point)
		)).getOrElse(Seq())
	}

	// false if there is an exisiting manual checkpoint or automatic checkpoint with the same state
	private def noExistingCheckpoint(point: AttendanceMonitoringPoint, attendance: SmallGroupEventAttendance, studentMember: StudentMember): Boolean = {
		val checkpoint = attendanceMonitoringService.getCheckpoints(Seq(point), Seq(studentMember)).values.headOption.flatMap(_.values.headOption)
		!checkpoint.exists(c => !c.autoCreated || c.state == attendance.state)
	}

	private def checkQuantity(point: AttendanceMonitoringPoint, attendance: SmallGroupEventAttendance, student: StudentMember): Boolean = {
		if (point.smallGroupEventQuantity == 1) {
			true
		}	else {
			val attendances = findAttendanceForDates(point.startDate, point.endDate, attendance.occurrence.event.group.groupSet.academicYear, student, point.smallGroupEventModules)
				.filterNot(a => a.occurrence == attendance.occurrence && a.universityId == attendance.universityId)
			point.smallGroupEventQuantity <= attendances.size + 1
		}
	}

	private def checkMonitoringPointRequirementsMissed(point: AttendanceMonitoringPoint, attendance: SmallGroupEventAttendance, student: StudentMember): Boolean = {

		// find possible events that this student can go to that have not been already marked
		// find SmallGroupEventAttendance marked attended already for AttendanceMonitoringpoint
		val attendedEvents = findAttendanceForDates(
			point.startDate,
			point.endDate,
			attendance.occurrence.event.group.groupSet.academicYear,
			student, point.smallGroupEventModules
		).filter(a =>  a.state == AttendanceState.Attended)
		val unmarkedEventOccurrences = findEventOccurrencesForDates(
			point.startDate, point.endDate,
			attendance.occurrence.event.group.groupSet.academicYear,
			student, point.smallGroupEventModules
		).filter(occurence =>  occurence != attendance.occurrence && checkOccurrenceNotMarked(occurence, student))
		(attendedEvents.size + unmarkedEventOccurrences.size) < point.smallGroupEventQuantity
	}

	private def checkOccurrenceNotMarked(occurence: SmallGroupEventOccurrence, student: StudentMember): Boolean = {
		occurence.attendance.asScala.count(attendance => attendance.universityId == student.universityId) == 0
	}

	private def findAttendanceForDates(
		startDate: LocalDate,
		endDate: LocalDate,
		academicYear: AcademicYear,
		student: StudentMember,
		modules: Seq[Module]
	): Seq[SmallGroupEventAttendance] = {
		val startWeek = termService.getAcademicWeekForAcademicYear(startDate.toDateTimeAtStartOfDay, academicYear)
		val endWeek = termService.getAcademicWeekForAcademicYear(endDate.toDateTimeAtStartOfDay, academicYear)
		val weekAttendances = smallGroupService.findAttendanceForStudentInModulesInWeeks(student, startWeek, endWeek, modules)
		// weekAttendances may contain attendance before the startDate and after the endDate, so filter those out
		// don't need to filter if the startDate is a MOnday and the endDate is a Sunday, as that's the whole week
		if (startDate.getDayOfWeek == DayOfWeek.Monday.jodaDayOfWeek && endDate.getDayOfWeek == DayOfWeek.Sunday.jodaDayOfWeek) {
			weekAttendances
		} else {
			weekAttendances.filter(a =>
				a.occurrence.week > startWeek && a.occurrence.week < endWeek
				|| a.occurrence.week == startWeek && a.occurrence.event.day.jodaDayOfWeek >= startDate.getDayOfWeek
				|| a.occurrence.week == endWeek && a.occurrence.event.day.jodaDayOfWeek <= endDate.getDayOfWeek
			)
		}
	}

	// used same logic that we have used in findAttendanceForDates
	private def findEventOccurrencesForDates(
		startDate: LocalDate,
		endDate: LocalDate,
		academicYear: AcademicYear,
		student: StudentMember,
		modules: Seq[Module]
	): Seq[SmallGroupEventOccurrence] = {
		val startWeek = termService.getAcademicWeekForAcademicYear(startDate.toDateTimeAtStartOfDay, academicYear)
		val endWeek = termService.getAcademicWeekForAcademicYear(endDate.toDateTimeAtStartOfDay, academicYear)
		val weekOccurrences = (modules match {
			case Nil => smallGroupService.findOccurrencesInWeeks(startWeek, endWeek, academicYear)
			case _ => smallGroupService.findOccurrencesInModulesInWeeks(startWeek, endWeek, modules, academicYear)

		}).filter(_.event.group.groupSet.showAttendanceReports).filter(_.event.group.students.includesUser(MemberOrUser(student).asUser))

		// logic similar to findAttendanceForDates
		// weekAttendances may contain attendance before the startDate and after the endDate, so filter those out
		// don't need to filter if the startDate is a Monday and the endDate is a Sunday, as that's the whole week
		if (startDate.getDayOfWeek == DayOfWeek.Monday.jodaDayOfWeek && endDate.getDayOfWeek == DayOfWeek.Sunday.jodaDayOfWeek) {
			weekOccurrences
		} else {
			weekOccurrences.filter(a =>
				a.week > startWeek && a.week < endWeek
					|| a.week == startWeek && a.event.day.jodaDayOfWeek >= startDate.getDayOfWeek
					|| a.week == endWeek && a.event.day.jodaDayOfWeek <= endDate.getDayOfWeek
			)
		}
	}

}

@Service("attendanceMonitoringEventAttendanceService")
class AttendanceMonitoringEventAttendanceServiceImpl
	extends AbstractAttendanceMonitoringEventAttendanceService
	with AutowiringAttendanceMonitoringServiceComponent
	with AutowiringProfileServiceComponent
	with AutowiringTermServiceComponent
	with AutowiringSmallGroupServiceComponent
