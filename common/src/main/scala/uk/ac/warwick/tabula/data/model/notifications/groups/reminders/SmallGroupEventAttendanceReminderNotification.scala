package uk.ac.warwick.tabula.data.model.notifications.groups.reminders

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.{DateTime, Days}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.HasSettings.BooleanSetting
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventOccurrence, WeekRange}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.helpers.WholeWeekFormatter
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, ModuleAndDepartmentService, TermService, UserLookupService}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

@Entity
@DiscriminatorValue(value="SmallGroupEventAttendanceReminder")
class SmallGroupEventAttendanceReminderNotification
	extends Notification[SmallGroupEventOccurrence, Unit]
		with SingleItemNotification[SmallGroupEventOccurrence]
		with ConfigurableNotification
		with AllCompletedActionRequiredNotification {

	override final def verb = "record"

	override def urlTitle = "record attendance for these seminars"

	override def url: String = Routes.tutor.registerForWeek(event, item.entity.week)

	@transient implicit var termService: TermService = Wire[TermService]

	@transient
	final lazy val event: SmallGroupEvent = item.entity.event

	@transient
	final lazy val configuringDepartment: Department = event.group.groupSet.module.adminDepartment

	@transient
	final lazy val referenceDate: DateTime = item.entity.dateTime.getOrElse(throw new IllegalArgumentException("Tried to create notification for occurrence with no date time"))

	override final def onPreSave(newRecord: Boolean) {
		priority = if (Days.daysBetween(created, referenceDate).getDays >= 5) {
			Critical
		} else {
			Warning
		}
	}

	override def title: String = {
		val name = s"${event.group.groupSet.module.code.toUpperCase} ${event.group.groupSet.nameWithoutModulePrefix}"
		val dayOfWeek = event.day.name

		val date = WholeWeekFormatter.format(
			ranges = Seq(WeekRange(item.entity.week)),
			dayOfWeek = event.day,
			year = event.group.groupSet.academicYear,
			numberingSystem = event.group.groupSet.module.adminDepartment.weekNumberingSystem,
			short = false
		).replace("<sup>", "").replace("</sup>", "")

		"%s attendance needs recording for %s, %s".format(name, dayOfWeek, date)
	}

	@transient
	final val FreemarkerTemplate = "/WEB-INF/freemarker/notifications/groups/small_group_event_attendance_reminder_notification.ftl"

	override def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
		"occurrence" -> item.entity,
		"dateTimeFormatter" -> dateTimeFormatter
	))

	override def allRecipients: Seq[User] = {
		val attendanceIds = item.entity.attendance.asScala.map(_.universityId)
		if (!event.group.groupSet.collectAttendance || event.group.groupSet.archived || event.group.students.isEmpty || event.group.students.users.map(_.getWarwickId).forall(attendanceIds.contains)) {
			Seq()
		} else {
			var users: Seq[User] = Seq()

			val settings = new SmallGroupEventAttendanceReminderNotificationSettings(departmentSettings)
			val notifyAllGroups = !settings.notifyFirstNonEmptyGroupOnly.value

			val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
			val module =
				moduleAndDepartmentService.getModuleByCode(event.group.groupSet.module.code)
					.getOrElse(throw new IllegalStateException("No such module"))

			if (settings.notifyNamedUsers.value && settings.notifyNamedUsersFirst.value) {
				users ++= settings.namedUsers.value
			}

			if (event.group.groupSet.releasedToTutors && settings.notifyTutors.value && (users.isEmpty || notifyAllGroups)) {
				users ++= event.tutors.users
			}

			if (settings.notifyModuleAssistants.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.assistants.users
			}

			if (settings.notifyModuleManagers.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.managers.users
			}

			if (settings.notifyDepartmentAdministrators.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.adminDepartment.owners.users
			}

			if (settings.notifyNamedUsers.value && !settings.notifyNamedUsersFirst.value && (users.isEmpty || notifyAllGroups)) {
				users ++= settings.namedUsers.value
			}

			users.distinct
		}
	}
}

class SmallGroupEventAttendanceReminderNotificationSettings(departmentSettings: NotificationSettings) {
	@transient private val userLookup = Wire[UserLookupService]
	// Configuration settings specific to this type of notification
	def enabled: BooleanSetting = departmentSettings.enabled
	def notifyTutors = departmentSettings.BooleanSetting("notifyTutors", default = true)
	def notifyModuleAssistants = departmentSettings.BooleanSetting("notifyModuleAssistants", default = false)
	def notifyModuleManagers = departmentSettings.BooleanSetting("notifyModuleManagers", default = true)
	def notifyDepartmentAdministrators = departmentSettings.BooleanSetting("notifyDepartmentAdministrators", default = false)
	def notifyNamedUsers = departmentSettings.BooleanSetting("notifyNamedUsers", default = false)
	def notifyNamedUsersFirst = departmentSettings.BooleanSetting("notifyNamedUsersFirst", default = false)
	def namedUsers = departmentSettings.UserSeqSetting("namedUsers", default = Seq(), userLookup)
	def notifyFirstNonEmptyGroupOnly = departmentSettings.BooleanSetting("notifyFirstNonEmptyGroupOnly", default = true)
}