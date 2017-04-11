package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringCheckpointTotal}
import uk.ac.warwick.tabula.data.model.notifications.attendance.{MissedAttendanceMonitoringCheckpointsHighNotification, MissedAttendanceMonitoringCheckpointsLowNotification, MissedAttendanceMonitoringCheckpointsMediumNotification}
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent

trait MissedAttendanceMonitoringCheckpointsNotifications
	extends Notifies[(Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal]), AttendanceMonitoringCheckpointTotal] {

	self: AttendanceMonitoringServiceComponent =>

	override def emit(result: (Seq[AttendanceMonitoringCheckpoint], Seq[AttendanceMonitoringCheckpointTotal])): Seq[Notification[AttendanceMonitoringCheckpointTotal, Unit]] = {
		result match {
			case (_, totals) => totals.flatMap { total =>
				val levels = total.department.missedMonitoringPointsNotificationLevels
				if (total.unauthorised >= levels.high && total.unauthorisedHighLevelNotified == null) {
					attendanceMonitoringService.updateCheckpointTotalHigh(total)
					Some(Notification.init(new MissedAttendanceMonitoringCheckpointsHighNotification, null, total))
				} else if (total.unauthorised >= levels.medium && total.unauthorisedMediumLevelNotified == null) {
					attendanceMonitoringService.updateCheckpointTotalMedium(total)
					Some(Notification.init(new MissedAttendanceMonitoringCheckpointsMediumNotification, null, total))
				} else if (total.unauthorised >= levels.low && total.unauthorisedLowLevelNotified == null) {
					attendanceMonitoringService.updateCheckpointTotalLow(total)
					Some(Notification.init(new MissedAttendanceMonitoringCheckpointsLowNotification, null, total))
				} else {
					None
				}
			}
			case _ => Seq()
		}
	}

}
