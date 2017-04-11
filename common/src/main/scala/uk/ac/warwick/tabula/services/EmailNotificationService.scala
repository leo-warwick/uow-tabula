package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.{Daoisms, NotificationDao, Scrollable}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.notifications.EmailNotificationListener

@Service
class EmailNotificationService extends Logging with Daoisms {

	val RunBatchSize = 100

	var dao: NotificationDao = Wire[NotificationDao]
	var listener: RecipientNotificationListener = Wire[EmailNotificationListener]

	def processNotifications(): Unit = transactional() {
		unemailedRecipients.take(RunBatchSize).foreach { recipient =>
			try {
				logger.info("Emailing recipient - " + recipient)
				listener.listen(recipient)
				session.flush()
			} catch {
				case throwable: Throwable => {
					// TAB-2238 Catch and log, so that the overall transaction can still commit
					logger.error("Exception handling email:", throwable)
				}
			}
		}
	}

	def recentRecipients(start: Int, count: Int): Seq[RecipientNotificationInfo] = dao.recentRecipients(start, count)
	def unemailedRecipientCount: Number = dao.unemailedRecipientCount
	def unemailedRecipients: Scrollable[RecipientNotificationInfo] = dao.unemailedRecipients
	def oldestUnemailedRecipient: Option[RecipientNotificationInfo] = dao.oldestUnemailedRecipient

}
