package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands.{Appliable, Description}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.notifications.coursework.FinaliseFeedbackNotificationSettings
import uk.ac.warwick.tabula.data.model.notifications.groups.reminders.SmallGroupEventAttendanceReminderNotificationSettings
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}

class NotificationSettingsCommandTest extends TestBase with Mockito {

	private trait Fixture {
		val testDepartment = new Department

		val commandInternal = new NotificationSettingsCommandInternal(testDepartment) with ModuleAndDepartmentServiceComponent with UserLookupComponent with PopulateNotificationSettingsCommandState {
			var moduleAndDepartmentService: ModuleAndDepartmentService = mock[ModuleAndDepartmentService]
			var userLookup = new MockUserLookup
		}
	}

	@Test
	def objectApplyCreatesCommand() {
		new Fixture {
			val command = NotificationSettingsCommand(testDepartment)

			command.isInstanceOf[Appliable[Department]] should be(true)
			command.isInstanceOf[NotificationSettingsCommandState] should be(true)
			command.asInstanceOf[NotificationSettingsCommandState].department should be(testDepartment)
		}
	}

	@Test
	def commandSetsStateFromDepartmentWhenConstructing() {
		new Fixture {
			commandInternal.smallGroupEventAttendanceReminderEnabled should be (true)
			commandInternal.smallGroupEventAttendanceReminderNotifyTutors should be (true)
			commandInternal.smallGroupEventAttendanceReminderNotifyModuleAssistants should be (false)
			commandInternal.smallGroupEventAttendanceReminderNotifyModuleManagers should be (true)
			commandInternal.smallGroupEventAttendanceReminderNotifyDepartmentAdministrators should be (false)
			commandInternal.smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly should be (true)

			commandInternal.finaliseFeedbackNotificationEnabled should be (true)
			commandInternal.finaliseFeedbackNotificationNotifyModuleManagers should be (false)
			commandInternal.finaliseFeedbackNotificationNotifyDepartmentAdministrators should be (false)
			commandInternal.finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly should be (true)
		}
	}

	@Test
	def commandUpdatesDepartmentWhenApplied() {
		new Fixture {
			commandInternal.smallGroupEventAttendanceReminderEnabled = true
			commandInternal.smallGroupEventAttendanceReminderNotifyTutors = false
			commandInternal.smallGroupEventAttendanceReminderNotifyModuleAssistants = false
			commandInternal.smallGroupEventAttendanceReminderNotifyModuleManagers = false
			commandInternal.smallGroupEventAttendanceReminderNotifyDepartmentAdministrators = true
			commandInternal.smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly = true

			commandInternal.finaliseFeedbackNotificationEnabled = true
			commandInternal.finaliseFeedbackNotificationNotifyModuleManagers = false
			commandInternal.finaliseFeedbackNotificationNotifyDepartmentAdministrators = true
			commandInternal.finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly = true

			commandInternal.applyInternal()

			val smallGroupEventAttendanceReminderSettings = new SmallGroupEventAttendanceReminderNotificationSettings(testDepartment.notificationSettings("SmallGroupEventAttendanceReminder"))

			smallGroupEventAttendanceReminderSettings.enabled.value should be (true)
			smallGroupEventAttendanceReminderSettings.notifyTutors.value should be (false)
			smallGroupEventAttendanceReminderSettings.notifyModuleAssistants.value should be (false)
			smallGroupEventAttendanceReminderSettings.notifyModuleManagers.value should be (false)
			smallGroupEventAttendanceReminderSettings.notifyDepartmentAdministrators.value should be (true)
			smallGroupEventAttendanceReminderSettings.notifyFirstNonEmptyGroupOnly.value should be (true)

			val finaliseFeedbackNotificationSettings = new FinaliseFeedbackNotificationSettings(testDepartment.notificationSettings("FinaliseFeedback"))

			finaliseFeedbackNotificationSettings.enabled.value should be (true)
			finaliseFeedbackNotificationSettings.notifyModuleManagers.value should be (false)
			finaliseFeedbackNotificationSettings.notifyDepartmentAdministrators.value should be (true)
			finaliseFeedbackNotificationSettings.notifyFirstNonEmptyGroupOnly.value should be (true)
		}
	}

	@Test
	def commandApplyInvokesSaveOnDepartmentService() {
		new Fixture {
			commandInternal.applyInternal()
			verify(commandInternal.moduleAndDepartmentService, times(1)).saveOrUpdate(testDepartment)
		}
	}

	@Test
	def commandDescriptionDescribedDepartment() {
		new Fixture {
			val describable = new DisplaySettingsCommandDescription with DisplaySettingsCommandState {
				val eventName: String = "test"
				val department: Department = testDepartment
			}

			val description: Description = mock[Description]
			describable.describe(description)
			verify(description, times(1)).department(testDepartment)
		}
	}

	@Test
	def permissionsRequireManageDisplaySettingsOnDepartment {
		new Fixture {
			val perms = new NotificationSettingsPermissions() with NotificationSettingsCommandState {
				val department: Department = testDepartment
			}
			val checking: PermissionsChecking = mock[PermissionsChecking]
			perms.permissionsCheck(checking)
			verify(checking, times(1)).PermissionCheck(Permissions.Department.ManageNotificationSettings, testDepartment)
		}
	}

}
