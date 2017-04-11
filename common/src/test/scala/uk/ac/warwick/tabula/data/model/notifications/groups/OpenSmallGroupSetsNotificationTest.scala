package uk.ac.warwick.tabula.data.model.notifications.groups

import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat
import uk.ac.warwick.tabula.{Fixtures, TestBase}

class OpenSmallGroupSetsNotificationTest extends TestBase {

	@Test def title() = withUser("cuscav", "0672089") {
		val module1 = Fixtures.module("cs118")
		val module2 = Fixtures.module("cs119")
		val module3 = Fixtures.module("cs120")

		val set1 = Fixtures.smallGroupSet("set 1")
		set1.format = SmallGroupFormat.Seminar
		set1.module = module1

		val set2 = Fixtures.smallGroupSet("set 2")
		set2.format = SmallGroupFormat.Seminar
		set2.module = module1

		val set3 = Fixtures.smallGroupSet("set 3")
		set3.format = SmallGroupFormat.Seminar
		set3.module = module1

		val notification = Notification.init(new OpenSmallGroupSetsStudentSignUpNotification, currentUser.apparentUser, Seq(set1, set2, set3))
		notification.title should be ("CS118 seminars are now open for sign up")

		set2.module = module3
		set2.format = SmallGroupFormat.Workshop
		notification.title should be ("CS118 and CS120 seminars and workshops are now open for sign up")

		set3.module = module2
		set2.format = SmallGroupFormat.Example
		notification.title should be ("CS118, CS119 and CS120 seminars and example classes are now open for sign up")
	}

}
