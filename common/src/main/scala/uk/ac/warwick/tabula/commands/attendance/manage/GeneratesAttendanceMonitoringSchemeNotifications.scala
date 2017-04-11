package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.data.model.{Department, ScheduledNotification}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent

import scala.collection.JavaConverters._

trait GeneratesAttendanceMonitoringSchemeNotifications extends Logging {

	self: AttendanceMonitoringServiceComponent with ProfileServiceComponent =>

	var thisScheduledNotificationService: ScheduledNotificationService = Wire.auto[ScheduledNotificationService]

	def generateNotifications(schemes: Seq[AttendanceMonitoringScheme]): Unit = {
		// Custom scheduled notifications
		schemes.groupBy(_.department).foreach{case(department, _) =>

			thisScheduledNotificationService.removeInvalidNotifications(department)

			val schemes = attendanceMonitoringService.listAllSchemes(department)

			val notifications = schemes.flatMap(_.points.asScala.map(_.endDate)).flatMap(date =>
				Seq(
					date.plusDays(3).toDateTimeAtStartOfDay,
					date.plusDays(6).toDateTimeAtStartOfDay
				)
			).distinct.flatMap(notificationDate => {
				Seq(
					new ScheduledNotification[Department]("AttendanceMonitoringUnrecordedPoints", department, notificationDate),
					new ScheduledNotification[Department]("AttendanceMonitoringUnrecordedStudents", department, notificationDate)
				)
			})

			for (scheduledNotification <- notifications) {
				if (scheduledNotification.scheduledDate.isBeforeNow) {
					logger.warn("ScheduledNotification generated in the past, ignoring: " + scheduledNotification)
				} else {
					thisScheduledNotificationService.push(scheduledNotification)
				}
			}
		}
	}

}
