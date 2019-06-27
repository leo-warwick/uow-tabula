package uk.ac.warwick.tabula.services.attendancemonitoring

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.services._

trait AttendanceMonitoringMeetingRecordServiceComponent {
  def attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService
}

trait AutowiringAttendanceMonitoringMeetingRecordServiceComponent extends AttendanceMonitoringMeetingRecordServiceComponent {
  val attendanceMonitoringMeetingRecordService: AttendanceMonitoringMeetingRecordService = Wire[AttendanceMonitoringMeetingRecordService]
}

trait AttendanceMonitoringMeetingRecordService {
  def getCheckpoints(meeting: MeetingRecord, onlyRecordable: Boolean = true): Seq[AttendanceMonitoringCheckpoint]

  def getCheckpointsWhenApproved(meeting: MeetingRecord, onlyRecordable: Boolean = true): Seq[AttendanceMonitoringCheckpoint]

  def updateCheckpoints(meeting: MeetingRecord): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])
}

abstract class AbstractAttendanceMonitoringMeetingRecordService extends AttendanceMonitoringMeetingRecordService {

  self: AttendanceMonitoringServiceComponent with RelationshipServiceComponent with MeetingRecordDaoComponent =>

  def getCheckpoints(meeting: MeetingRecord, onlyRecordable: Boolean = true): Seq[AttendanceMonitoringCheckpoint] = {
    if (!meeting.isAttendanceApproved) {
      Seq()
    } else {
      getCheckpointsWhenApproved(meeting, onlyRecordable)
    }
  }

  def getCheckpointsWhenApproved(meeting: MeetingRecord, onlyRecordable: Boolean = true): Seq[AttendanceMonitoringCheckpoint] = {
    meeting.relationships.flatMap(_.studentMember).flatMap {
      case studentMember: StudentMember =>
        val relevantPoints = getRelevantPoints(
          attendanceMonitoringService.listStudentsPointsForDate(studentMember, None, meeting.meetingDate),
          meeting,
          studentMember,
          onlyRecordable
        )

        relevantPoints.filter(point => checkQuantity(point, meeting, studentMember)).map(point => {
          val checkpoint = new AttendanceMonitoringCheckpoint
          checkpoint.autoCreated = true
          checkpoint.point = point
          checkpoint.attendanceMonitoringService = attendanceMonitoringService
          checkpoint.student = studentMember
          checkpoint.updatedBy = meeting.relationships.head.agentMember match {
            case Some(agent: Member) => agent.userId
            case _ => meeting.relationships.head.agent
          }
          checkpoint.updatedDate = DateTime.now
          checkpoint.state = AttendanceState.Attended
          checkpoint
        })
      case _ => Nil
    }
  }

  def updateCheckpoints(meeting: MeetingRecord): (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]) = {
    getCheckpoints(meeting).map(checkpoint => {
      attendanceMonitoringService.setAttendance(checkpoint.student, Map(checkpoint.point -> checkpoint.state), checkpoint.updatedBy, autocreated = true)
    }).foldLeft(
      (Seq[AttendanceMonitoringCheckpoint](), Seq[AttendanceMonitoringCheckpointTotal]())
    ) {
      case ((leftCheckpoints, leftTotals), (rightCheckpoints, rightTotals)) => (leftCheckpoints ++ rightCheckpoints, leftTotals ++ rightTotals)
    }
  }

  private def getRelevantPoints(points: Seq[AttendanceMonitoringPoint], meeting: MeetingRecord, student: StudentMember, onlyRecordable: Boolean): Seq[AttendanceMonitoringPoint] = {
    points.filter(point =>
      // Is it the correct type
      point.pointType == AttendanceMonitoringPointType.Meeting
        // Is the meeting's date inside the point's weeks
        && point.containsDate(meeting.meetingDate.toLocalDate)
        // Is the meeting's relationship valid
        && meeting.relationships.map(_.relationshipType).exists(point.meetingRelationships.contains)
        // Is the meeting's format valid
        && point.meetingFormats.contains(meeting.format)
        && (!onlyRecordable || (
        // Is there no existing checkpoint
        attendanceMonitoringService.getCheckpoints(Seq(point), Seq(student)).isEmpty
          // The student hasn't been sent to SITS for this point
          && !attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point))
        )
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
          && point.containsDate(m.meetingDate.toLocalDate)
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
    with AutowiringRelationshipServiceComponent
    with AutowiringMeetingRecordDaoComponent
