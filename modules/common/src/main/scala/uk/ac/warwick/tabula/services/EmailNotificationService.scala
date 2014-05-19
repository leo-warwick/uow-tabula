package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{Daoisms, NotificationDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.notifications.EmailNotificationListener
import uk.ac.warwick.tabula.data.Transactions._

@Service
class EmailNotificationService extends Logging with Daoisms {

	val RunBatchSize = 100

	var dao = Wire[NotificationDao]
	var listener: RecipientNotificationListener = Wire[EmailNotificationListener]

	def processNotifications() = transactional() {
		dao.unemailedRecipients.take(RunBatchSize).foreach { recipient =>
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

}
