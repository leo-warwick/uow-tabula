package uk.ac.warwick.tabula.data.model.notifications.attendance

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringCheckpointTotal
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, RelationshipService}
import uk.ac.warwick.userlookup.User

abstract class MissedAttendanceMonitoringCheckpointsNotification
	extends Notification[AttendanceMonitoringCheckpointTotal, Unit]
	with SingleItemNotification[AttendanceMonitoringCheckpointTotal] {

	@transient
	def level: Int

	@transient
	lazy val department: Department = item.entity.department
	@transient
	lazy val student: StudentMember = item.entity.student
	@transient
	lazy val academicYear: AcademicYear = item.entity.academicYear

	@transient
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	@transient
	var relationshipService: RelationshipService = Wire[RelationshipService]

	@transient
	override def verb: String = "view"

	@transient
	override def urlTitle: String = s"view ${Option(student.firstName).getOrElse("the student")}'s monitoring points"

	@transient
	override def url: String = Routes.View.student(department, academicYear, student)

	@transient
	override def title: String = s"${student.fullName.getOrElse("A student")} has missed $level monitoring points"

	@transient
	override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/attendancemonitoring/attendance_monitoring_missed_checkpoints_notification.ftl", Map(
		"level" -> level,
		"academicYear" -> academicYear,
		"student" -> student,
		"relationships" -> relationshipService.getAllCurrentRelationships(student).groupBy(_.relationshipType)
	))

	@transient
	override def recipients: Seq[User] =
		// department.owners is not populated correctly if department not fetched directly
		moduleAndDepartmentService.getDepartmentById(department.id).get.owners.users
}

@Entity
@DiscriminatorValue(value="MissedAttendanceMonitoringCheckpointsLow")
class MissedAttendanceMonitoringCheckpointsLowNotification extends MissedAttendanceMonitoringCheckpointsNotification {

	@transient
	override lazy val level: Int = department.missedMonitoringPointsNotificationLevels.low

}

@Entity
@DiscriminatorValue(value="MissedAttendanceMonitoringCheckpointsMedium")
class MissedAttendanceMonitoringCheckpointsMediumNotification extends MissedAttendanceMonitoringCheckpointsNotification {

	@transient
	override lazy val level: Int = department.missedMonitoringPointsNotificationLevels.medium

}

@Entity
@DiscriminatorValue(value="MissedAttendanceMonitoringCheckpointsHigh")
class MissedAttendanceMonitoringCheckpointsHighNotification extends MissedAttendanceMonitoringCheckpointsNotification {

	@transient
	override lazy val level: Int = department.missedMonitoringPointsNotificationLevels.high

}