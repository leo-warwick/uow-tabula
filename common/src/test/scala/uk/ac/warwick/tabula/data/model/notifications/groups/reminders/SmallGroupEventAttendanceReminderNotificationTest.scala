package uk.ac.warwick.tabula.data.model.notifications.groups.reminders

import javax.sql.DataSource

import org.hamcrest.Matchers._
import org.hibernate.{Session, SessionFactory}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, SmallGroupEventOccurrence, SmallGroupFormat, WeekRange}
import uk.ac.warwick.tabula.data.model.notifications.groups.reminders.SmallGroupEventAttendanceReminderNotificationTest._
import uk.ac.warwick.tabula.data.model.{Notification, UserGroup}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.userlookup.AnonymousUser

class SmallGroupEventAttendanceReminderNotificationTest extends TestBase with FunctionalContextTesting {

	@Test def title() { inContext[MinimalContext] {
		val department = Fixtures.department("cs")
		department.weekNumberingSystem = WeekRange.NumberingSystem.Default

		val module = Fixtures.module("cs118")
		module.adminDepartment = department

		val set = Fixtures.smallGroupSet("CS118 seminars")
		set.module = module
		set.format = SmallGroupFormat.Seminar
		set.academicYear = AcademicYear(2014)

		val group = Fixtures.smallGroup("group")
		group.groupSet = set

		val event = Fixtures.smallGroupEvent("event")
		event.group = group
		event.day = DayOfWeek.Tuesday

		val occurrence = new SmallGroupEventOccurrence
		occurrence.week = 1
		occurrence.event = event

		val notification = Notification.init(new SmallGroupEventAttendanceReminderNotification, new AnonymousUser, occurrence)
		notification.title should be ("CS118 seminars attendance needs recording for Tuesday, Term 1, week 1")

		department.weekNumberingSystem = WeekRange.NumberingSystem.Academic
		notification.title should be ("CS118 seminars attendance needs recording for Tuesday, Term 1, week 1")

		department.weekNumberingSystem = WeekRange.NumberingSystem.Cumulative
		notification.title should be ("CS118 seminars attendance needs recording for Tuesday, Term 1, week 1")

		// FIXME this format sucks
		department.weekNumberingSystem = WeekRange.NumberingSystem.None
		notification.title should be ("CS118 seminars attendance needs recording for Tuesday, w/c Mon 29th Sep 2014")
	}}

}

object SmallGroupEventAttendanceReminderNotificationTest {
	class MinimalContext extends FunctionalContext with Mockito {
		bean() { new TermServiceImpl }

		// Just to get things like Department working
		bean(){mock[UserLookupService]}
		bean(){mock[RelationshipService]}
		bean() {
			val permissionsService = mock[PermissionsService]
			permissionsService.ensureUserGroupFor(anArgThat(anything), anArgThat(anything))(anArgThat(anything)) returns UserGroup.ofUsercodes
			permissionsService
		}
		bean(){mock[AssessmentMembershipService]}
		bean(){mock[UserSettingsService]}
		bean(){mock[NotificationService]}
		bean(){mock[ModuleAndDepartmentService]}
		bean(){
			val sessionFactory = smartMock[SessionFactory]
			val session = smartMock[Session]
			sessionFactory.getCurrentSession returns session
			sessionFactory.openSession() returns session
			sessionFactory
		}
		bean("dataSource"){mock[DataSource]}
	}
}
