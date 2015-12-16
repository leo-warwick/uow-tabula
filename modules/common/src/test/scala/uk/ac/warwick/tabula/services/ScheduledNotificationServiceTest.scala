package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification, HeronWarningNotification, Heron, ScheduledNotification}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.{MaintenanceModeAwareSession, MockScrollableResults, ScheduledNotificationDao}
import org.mockito.Mockito._
import org.hibernate.{SessionFactory, Session}

class ScheduledNotificationServiceTest extends TestBase with Mockito {

	val service = new ScheduledNotificationServiceImpl
	val dao =  mock[ScheduledNotificationDao]
	val notificationService = mock[NotificationService]
	service.dao = dao
	service.notificationService = notificationService

	val sessionFactory = mock[SessionFactory]
	val session = mock[Session]

	sessionFactory.getCurrentSession() returns (session)
	sessionFactory.openSession() returns (session)

	service.sessionFactory = sessionFactory

	val heron = new Heron()
	val sn1 = new ScheduledNotification("HeronWarning", heron, DateTime.now.minusDays(1))
	sn1.id = "sn1"

	val sn2 = new ScheduledNotification("HeronDefeat", heron, DateTime.now.minusDays(2))
	sn2.id = "sn2"

	val sn3 = new ScheduledNotification("HeronWarning", heron, DateTime.now.minusDays(3))
	sn3.id = "sn3"

	session.get(classOf[ScheduledNotification[_]], "sn1") returns (sn1)
	session.get(classOf[ScheduledNotification[_]], "sn2") returns (sn2)
	session.get(classOf[ScheduledNotification[_]], "sn3") returns (sn3)

	val scheduledNotifications = Seq(sn1, sn2, sn3)
	//val itr = scheduledNotifications.iterator

	val scrollingScheduledNotifications = new MockScrollableResults(scheduledNotifications)

	when (dao.notificationsToComplete) thenReturn MaintenanceModeAwareSession(session).scrollable[ScheduledNotification[_  >: Null <: ToEntityReference]](scrollingScheduledNotifications)

	@Test
	def generateNotifications() {
		val notification = service.generateNotification(sn1).get

		notification.isInstanceOf[HeronWarningNotification] should be (true)
		notification.title should be("You all need to know. Herons would love to kill you in your sleep")
		notification.url should be ("/beware/herons")
		notification.urlTitle should be ("see how evil herons really are")
	}

	@Test
	def processNotifications() {
		service.processNotifications()

		verify(session, times(3)).saveOrUpdate(isA[Notification[_,_]])

		for(sn <- scheduledNotifications) {
			sn.completed should be (true)
		}
	}

}
