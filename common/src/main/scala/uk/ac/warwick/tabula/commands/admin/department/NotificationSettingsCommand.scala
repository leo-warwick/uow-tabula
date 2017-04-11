package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{Description, Describable, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.notifications.coursework.FinaliseFeedbackNotificationSettings
import uk.ac.warwick.tabula.data.model.notifications.groups.reminders.SmallGroupEventAttendanceReminderNotificationSettings
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent, ModuleAndDepartmentServiceComponent, AutowiringModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}

object NotificationSettingsCommand {
	def apply(department: Department) =
		new NotificationSettingsCommandInternal(department)
			with ComposableCommand[Department]
			with PopulateNotificationSettingsCommandState
			with NotificationSettingsDescription
			with NotificationSettingsPermissions
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringUserLookupComponent
}

trait NotificationSettingsCommandState {
	def department: Department

	lazy val smallGroupEventAttendanceReminderSettings = new SmallGroupEventAttendanceReminderNotificationSettings(department.notificationSettings("SmallGroupEventAttendanceReminder"))

	var smallGroupEventAttendanceReminderEnabled: Boolean = _

	var smallGroupEventAttendanceReminderNotifyTutors: Boolean = _
	var smallGroupEventAttendanceReminderNotifyModuleAssistants: Boolean = _
	var smallGroupEventAttendanceReminderNotifyModuleManagers: Boolean = _
	var smallGroupEventAttendanceReminderNotifyDepartmentAdministrators: Boolean = _

	// Not currently in use
//	var smallGroupEventAttendanceReminderNotifyNamedUsers: Boolean = _
//	var smallGroupEventAttendanceReminderNotifyNamedUsersFirst: Boolean = _
//	var smallGroupEventAttendanceReminderNamedUsers: JList[String] = JArrayList()

	var smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly: Boolean = _

	lazy val finaliseFeedbackNotificationSettings = new FinaliseFeedbackNotificationSettings(department.notificationSettings("FinaliseFeedback"))

	var finaliseFeedbackNotificationEnabled: Boolean = _

	var finaliseFeedbackNotificationNotifyModuleManagers: Boolean = _
	var finaliseFeedbackNotificationNotifyDepartmentAdministrators: Boolean = _

	// Not currently in use
	//	var finaliseFeedbackNotificationNotifyNamedUsers: Boolean = _
	//	var finaliseFeedbackNotificationrNotifyNamedUsersFirst: Boolean = _
	//	var finaliseFeedbackNotificationNamedUsers: JList[String] = JArrayList()

	var finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly: Boolean = _
}

trait PopulateNotificationSettingsCommandState {
	self: NotificationSettingsCommandState =>

	smallGroupEventAttendanceReminderEnabled = smallGroupEventAttendanceReminderSettings.enabled.value

	smallGroupEventAttendanceReminderNotifyTutors = smallGroupEventAttendanceReminderSettings.notifyTutors.value
	smallGroupEventAttendanceReminderNotifyModuleAssistants = smallGroupEventAttendanceReminderSettings.notifyModuleAssistants.value
	smallGroupEventAttendanceReminderNotifyModuleManagers = smallGroupEventAttendanceReminderSettings.notifyModuleManagers.value
	smallGroupEventAttendanceReminderNotifyDepartmentAdministrators = smallGroupEventAttendanceReminderSettings.notifyDepartmentAdministrators.value

	// Not currently in use
//	smallGroupEventAttendanceReminderNotifyNamedUsers = smallGroupEventAttendanceReminderSettings.notifyNamedUsers.value
//	smallGroupEventAttendanceReminderNotifyNamedUsersFirst = smallGroupEventAttendanceReminderSettings.notifyNamedUsersFirst.value
//	smallGroupEventAttendanceReminderNamedUsers.addAll(
//		smallGroupEventAttendanceReminderSettings.namedUsers.value.map { _.getUserId }.filter { _.hasText }.asJavaCollection
//	)

	smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly = smallGroupEventAttendanceReminderSettings.notifyFirstNonEmptyGroupOnly.value

	finaliseFeedbackNotificationEnabled = finaliseFeedbackNotificationSettings.enabled.value

	finaliseFeedbackNotificationNotifyModuleManagers = finaliseFeedbackNotificationSettings.notifyModuleManagers.value
	finaliseFeedbackNotificationNotifyDepartmentAdministrators = finaliseFeedbackNotificationSettings.notifyDepartmentAdministrators.value

	// Not currently in use
	//	finaliseFeedbackNotificationNotifyNamedUsers = finaliseFeedbackNotificationSettings.notifyNamedUsers.value
	//	finaliseFeedbackNotificationNotifyNamedUsersFirst = finaliseFeedbackNotificationSettings.notifyNamedUsersFirst.value
	//	finaliseFeedbackNotificationNamedUsers.addAll(
	//		finaliseFeedbackNotificationSettings.namedUsers.value.map { _.getUserId }.filter { _.hasText }.asJavaCollection
	//	)

	finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly = finaliseFeedbackNotificationSettings.notifyFirstNonEmptyGroupOnly.value
}

class NotificationSettingsCommandInternal(val department: Department) extends CommandInternal[Department] with NotificationSettingsCommandState {
	self: ModuleAndDepartmentServiceComponent with UserLookupComponent =>

	override def applyInternal(): Department = transactional() {
		smallGroupEventAttendanceReminderSettings.enabled.value = smallGroupEventAttendanceReminderEnabled

		smallGroupEventAttendanceReminderSettings.notifyTutors.value = smallGroupEventAttendanceReminderNotifyTutors
		smallGroupEventAttendanceReminderSettings.notifyModuleAssistants.value = smallGroupEventAttendanceReminderNotifyModuleAssistants
		smallGroupEventAttendanceReminderSettings.notifyModuleManagers.value = smallGroupEventAttendanceReminderNotifyModuleManagers
		smallGroupEventAttendanceReminderSettings.notifyDepartmentAdministrators.value = smallGroupEventAttendanceReminderNotifyDepartmentAdministrators

		// Not currently in use
//		smallGroupEventAttendanceReminderSettings.notifyNamedUsers.value = smallGroupEventAttendanceReminderNotifyNamedUsers
//		smallGroupEventAttendanceReminderSettings.notifyNamedUsersFirst.value = smallGroupEventAttendanceReminderNotifyNamedUsersFirst
//		smallGroupEventAttendanceReminderSettings.namedUsers.value = smallGroupEventAttendanceReminderNamedUsers.asScala.map(userLookup.getUserByUserId).filter(_.isFoundUser).toList

		smallGroupEventAttendanceReminderSettings.notifyFirstNonEmptyGroupOnly.value = smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly

		finaliseFeedbackNotificationSettings.enabled.value = finaliseFeedbackNotificationEnabled

		finaliseFeedbackNotificationSettings.notifyModuleManagers.value = finaliseFeedbackNotificationNotifyModuleManagers
		finaliseFeedbackNotificationSettings.notifyDepartmentAdministrators.value = finaliseFeedbackNotificationNotifyDepartmentAdministrators

		// Not currently in use
		//		finaliseFeedbackNotificationSettings.notifyNamedUsers.value = finaliseFeedbackNotificationNotifyNamedUsers
		//		finaliseFeedbackNotificationSettings.notifyNamedUsersFirst.value = finaliseFeedbackNotificationNotifyNamedUsersFirst
		//		finaliseFeedbackNotificationSettings.namedUsers.value = finaliseFeedbackNotificationNamedUsers.asScala.map(userLookup.getUserByUserId).filter(_.isFoundUser).toList

		finaliseFeedbackNotificationSettings.notifyFirstNonEmptyGroupOnly.value = finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly

		moduleAndDepartmentService.saveOrUpdate(department)
		department
	}
}

trait NotificationSettingsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: NotificationSettingsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ManageNotificationSettings, mandatory(department))
	}
}

trait NotificationSettingsDescription extends Describable[Department] {
	self: NotificationSettingsCommandState =>

	override def describe(d: Description): Unit =
		d.department(department)

	override def describeResult(d: Description, result: Department): Unit =
		d.department(department)
		 .property("SmallGroupEventAttendanceReminder", department.notificationSettings("SmallGroupEventAttendanceReminder"))
		 .property("FinaliseFeedback", department.notificationSettings("FinaliseFeedback"))
}