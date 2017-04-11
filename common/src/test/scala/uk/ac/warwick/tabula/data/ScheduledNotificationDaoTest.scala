package uk.ac.warwick.tabula.data

import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.{Mockito, PersistenceTestBase}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.{ScheduledNotification, Heron, SSOUserType}
import uk.ac.warwick.tabula.services.UserLookupService
import org.joda.time.DateTime

@Transactional
class ScheduledNotificationDaoTest extends PersistenceTestBase with Mockito {

	val dao = new ScheduledNotificationDaoImpl
	val heron = new Heron()
	val heron2 = new Heron()

	def testNotification(target: Heron, date: DateTime): ScheduledNotification[Heron] = {
		val sn = new ScheduledNotification[Heron]("heronWarning", target, date)
		sn
	}

	@Before
	def setup() {
		dao.sessionFactory = sessionFactory
		SSOUserType.userLookup = smartMock[UserLookupService]
		// hbm2ddl generates a swathe of conflicting foreign key constraints for entity_id, so ignore for this test
		session.createSQLQuery("SET DATABASE REFERENTIAL INTEGRITY FALSE").executeUpdate()
	}

	@Test def saveAndFetch() {

		val tomorrow = DateTime.now.plusDays(1)
		val notification = testNotification(heron, tomorrow)

		session.save(heron)

		dao.getById("heronWarningNotification") should be (None)
		dao.save(notification)
		dao.getById(notification.id) should be (Option(notification))

		session.flush()
		session.clear()

		val retrievedNotification = dao.getById(notification.id).get.asInstanceOf[ScheduledNotification[Heron]]
		retrievedNotification.completed should be (false)
		retrievedNotification.scheduledDate should be (tomorrow)
		retrievedNotification.target should not be null
		retrievedNotification.target.entity should be(heron)

		retrievedNotification.completed = true
		dao.save(retrievedNotification)
		session.flush()
		session.clear()
		dao.getById(notification.id).get.asInstanceOf[ScheduledNotification[Heron]].completed should be (true)

		session.clear()
		session.delete(notification)
		session.delete(heron)
		session.flush()
	}

	@Test def scheduledNotifications() {

		session.save(heron)
		session.save(heron2)

		val n1 = testNotification(heron, DateTime.now.plusDays(1))
		val n2 = testNotification(heron2, DateTime.now.plusDays(2))
		val n3 = testNotification(heron, DateTime.now.plusDays(3))
		val n4 = testNotification(heron, DateTime.now.plusDays(4))
		n4.completed = true

		val notifications = Seq(n1, n2, n3, n4)
		notifications.foreach(dao.save)

		session.flush()

		dao.getScheduledNotifications(heron) should be (Seq(n1, n3))
		dao.getScheduledNotifications(heron2) should be (Seq(n2))

		session.clear()
		session.delete(heron)
		session.flush()
	}

	@Test def getNotificationsToComplete() {
		session.save(heron)

		val n1 = testNotification(heron, DateTime.now.minusDays(2))
		n1.completed = true
		val n2 = testNotification(heron, DateTime.now.minusDays(1))
		val n3 = testNotification(heron, DateTime.now)
		val n4 = testNotification(heron, DateTime.now.plusDays(1))


		val notifications = Seq(n1, n2, n3, n4)
		notifications.foreach(dao.save)

		session.flush()
		session.clear()

		// FIXME make Scrollable.map() work properly so I don't have to do this silly list append
		val result = collection.mutable.ListBuffer[ScheduledNotification[_]]()
		dao.notificationsToComplete.take(100).foreach{ e =>
			result += e
		}

		result should be (Seq(n2,n3))

		session.clear()
		session.delete(heron)
		session.flush()
	}

}