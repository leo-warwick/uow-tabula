package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._

trait AttendanceMonitoringMeetingRecordServiceComponent {
	def attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService
}

trait AutowiringAttendanceMonitoringMeetingRecordServiceComponent extends AttendanceMonitoringMeetingRecordServiceComponent {
	val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = Wire[AttendanceMonitoringMeetingRecordService]
}

trait AttendanceMonitoringMeetingRecordService {
	def getCheckpoints(meeting: MeetingRecord): Seq[AttendanceMonitoringCheckpoint]
	def updateCheckpoints(meeting: MeetingRecord): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
}

abstract class AbstractAttendanceMonitoringMeetingRecordService extends AttendanceMonitoringMeetingRecordService {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent with RelationshipServiceComponent with MeetingRecordDaoComponent =>

	def getCheckpoints(meeting: MeetingRecord): Seq[AttendanceMonitoringCheckpoint] = {
		if (!meeting.isAttendanceApproved) {
			Seq()
		} else {
			meeting.relationship.studentMember.flatMap{
				case studentMember: StudentMember =>
					val academicYear = AcademicYear.findAcademicYearContainingDate(meeting.meetingDate)
					val relevantPoints = getRelevantPoints(
						attendanceMonitoringService.listStudentsPoints(studentMember, None, academicYear),
						meeting,
						studentMember
					)
					val checkpoints = relevantPoints.filter(point => checkQuantity(point, meeting, studentMember)).map(point => {
						val checkpoint = new AttendanceMonitoringCheckpoint
						checkpoint.autoCreated = true
						checkpoint.point = point
						checkpoint.attendanceMonitoringService = attendanceMonitoringService
						checkpoint.student = studentMember
						checkpoint.updatedBy = meeting.relationship.agentMember match {
							case Some(agent: Member) => agent.userId
							case _ => meeting.relationship.agent
						}
						checkpoint.updatedDate = DateTime.now
						checkpoint.state = AttendanceState.Attended
						checkpoint
					})
					Option(checkpoints)
				case _ => None
			}.getOrElse(Seq())
		}
	}

	def updateCheckpoints(meeting: MeetingRecord): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
		getCheckpoints(meeting).map(checkpoint => {
			attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
		}).foldLeft(
			(Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())
		){
			case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
		}
	}

	private def getRelevantPoints(points: Seq[AttendanceMonitoringPoint], meeting: MeetingRecord, student: StudentMember): Seq[AttendanceMonitoringPoint] = {
		points.filter(point =>
			// Is it the correct type
			point.pointType == AttendanceMonitoringPointType.Meeting
				// Is the meeting's date inside the point's weeks
				&& point.isDateValidForPoint(meeting.meetingDate.toLocalDate)
				// Is the meeting's relationship valid
				&& point.meetingRelationships.contains(meeting.relationship.relationshipType)
				// Is the meeting's format valid
				&& point.meetingFormats.contains(meeting.format)
				// Is there no existing checkpoint
				&& attendanceMonitoringService.getCheckpoints(Seq(point), Seq(student)).isEmpty
				// The student hasn't been sent to SITS for this point
				&& !attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point)
		)
	}

	private def checkQuantity(point: AttendanceMonitoringPoint, meeting: MeetingRecord, student: StudentMember): Boolean = {
		if (point.meetingQuantity == 1) {
			true
		} else {
			val meetings = point.meetingRelationships.flatMap(relationshipType =>
				relationshipService.getRelationships(relationshipType, student).flatMap(meetingRecordDao.list)
			).filterNot(m => m.id == meeting.id).filter(m =>
				m.isAttendanceApproved
				&& point.isDateValidForPoint(m.meetingDate.toLocalDate)
				&& point.meetingFormats.contains(m.format)
			) ++ Seq(meeting)

			meetings.size >= point.meetingQuantity
		}
	}
}

@Service("attendanceMonitoringMeetingRecordService")
class AttendanceMonitoringMeetingRecordServiceImpl
	extends AbstractAttendanceMonitoringMeetingRecordService
	with AutowiringAttendanceMonitoringServiceComponent
	with AutowiringTermServiceComponent
	with AutowiringRelationshipServiceComponent
	with AutowiringMeetingRecordDaoComponent
