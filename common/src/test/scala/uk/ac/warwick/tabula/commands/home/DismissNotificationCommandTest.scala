package uk.ac.warwick.tabula.commands.home

import org.mockito.Mockito.{times, verify}
import uk.ac.warwick.tabula.data.model.HeronWarningNotification
import uk.ac.warwick.tabula.services.{NotificationService, NotificationServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class DismissNotificationCommandTest extends TestBase with Mockito {

	val agent: User = Fixtures.user("7891011", "custrd")
	val recipient: User = Fixtures.user("0123456", "custrd")
	val n1: HeronWarningNotification = Fixtures.notification(agent, recipient)
	val n2: HeronWarningNotification = Fixtures.notification(recipient, recipient)
	val notifications = Seq(n1, n2)
	val ns: NotificationService = smartMock[NotificationService]
	ns.toActivity(n1) returns None
	ns.toActivity(n2) returns None

	trait CommandTestSupport extends NotificationServiceComponent {
		def notificationService: NotificationService = ns
	}

	@Test
	def dismissal() {
		val cmd = new DismissNotificationCommandInternal(notifications, dismiss=true, recipient) with CommandTestSupport
		cmd.applyInternal()
		notifications.foreach(n =>
			n.isDismissed(recipient) should be (true)
		)
		verify(ns, times(1)).update(notifications, recipient)
	}
}
