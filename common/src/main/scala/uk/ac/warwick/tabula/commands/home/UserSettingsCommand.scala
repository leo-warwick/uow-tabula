package uk.ac.warwick.tabula.commands.home

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description, SelfValidating}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.data.model.notifications.coursework.FinaliseFeedbackNotificationSettings
import uk.ac.warwick.tabula.data.model.notifications.groups.reminders.SmallGroupEventAttendanceReminderNotificationSettings
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringUserSettingsServiceComponent, UserSettingsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object UserSettingsCommand {

	def apply(user: CurrentUser, settings: UserSettings) =
		new UserSettingsCommand(user, settings)
		with ComposableCommand[UserSettings]
		with UserSettingsPermission
		with UserSettingsCommandValidation
		with UserSettingsDescription
		with UserSettingsCommandState
		with AutowiringUserSettingsServiceComponent
}

class UserSettingsCommand(val user: CurrentUser, val settings: UserSettings) extends CommandInternal[UserSettings] {
	self: UserSettingsServiceComponent =>

	var alertsSubmission: String = settings.alertsSubmission
	var weekNumberingSystem: String = settings.weekNumberingSystem
	var bulkEmailSeparator: String = settings.bulkEmailSeparator
	var profilesDefaultView: String = settings.profilesDefaultView

	lazy val smallGroupEventAttendanceReminderSettings = new SmallGroupEventAttendanceReminderNotificationSettings(settings.notificationSettings("SmallGroupEventAttendanceReminder"))
	var smallGroupEventAttendanceReminderEnabled: Boolean = smallGroupEventAttendanceReminderSettings.enabled.value

	lazy val finaliseFeedbackNotificationSettings = new FinaliseFeedbackNotificationSettings(settings.notificationSettings("FinaliseFeedback"))
	var finaliseFeedbackNotificationEnabled: Boolean = finaliseFeedbackNotificationSettings.enabled.value

	override def applyInternal(): UserSettings = transactional() {
		settings.alertsSubmission = alertsSubmission
		settings.weekNumberingSystem = if (weekNumberingSystem.hasText) weekNumberingSystem else null
		settings.bulkEmailSeparator = bulkEmailSeparator
		settings.profilesDefaultView = profilesDefaultView
		smallGroupEventAttendanceReminderSettings.enabled.value = smallGroupEventAttendanceReminderEnabled
		finaliseFeedbackNotificationSettings.enabled.value = finaliseFeedbackNotificationEnabled

		userSettingsService.save(user, settings)
		settings
	}

}

trait UserSettingsCommandValidation extends SelfValidating {

	self: UserSettingsCommandState =>

	override def validate(errors:Errors) {
		if (!user.exists) {
			errors.reject("user.mustBeLoggedIn")
		}
	}

}

trait UserSettingsDescription extends Describable[UserSettings] {

	self: UserSettingsCommandState =>

	override def describe(d: Description) {
		d.properties("user" -> user.apparentId)
	}

	override def describeResult(d: Description, result: UserSettings): Unit = {
		result.activeDepartment.foreach(d.department)
		d.properties(
			"user" -> user.apparentId,
			"alertsSubmission" -> result.alertsSubmission,
			"hiddenIntros" -> result.hiddenIntros,
			"weekNumberingSystem" -> result.weekNumberingSystem,
			"bulkEmailSeparator" -> result.bulkEmailSeparator,
			"profilesDefaultView" -> result.profilesDefaultView,
			"activeAcademicYear" -> result.activeAcademicYear
		)
	}
}

trait UserSettingsPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: UserSettingsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.UserSettings.Update, settings)
	}

}

trait UserSettingsCommandState {

	def user: CurrentUser
	def settings: UserSettings

}