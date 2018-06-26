package uk.ac.warwick.tabula.notifications

import java.util.concurrent.{ExecutionException, TimeUnit, TimeoutException}

import javax.mail.internet.MimeMessage
import org.hibernate.ObjectNotFoundException
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.notifications.RecipientNotificationInfo
import uk.ac.warwick.tabula.data.model.{ActionRequiredNotification, FreemarkerModel, HasNotificationAttachment, Notification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{Logging, UnicodeEmails}
import uk.ac.warwick.tabula.services.{NotificationService, RecipientNotificationListener}
import uk.ac.warwick.tabula.web.views.AutowiredTextRendererComponent
import uk.ac.warwick.tabula.{CurrentUser, RequestInfo}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.mail.WarwickMailSender

@Component
class EmailNotificationListener extends RecipientNotificationListener with UnicodeEmails with AutowiredTextRendererComponent with Logging {

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	var mailSender: WarwickMailSender = Wire[WarwickMailSender]("studentMailSender")
	var service: NotificationService = Wire[NotificationService]

	// email constants
	var replyAddress: String = Wire.property("${mail.noreply.to}")
	var fromAddress: String = Wire.property("${mail.admin.to}")
	val mailHeader = "Dear %s,\n\n"
	val mailFooter = "\n\nThank you,\nTabula"
	val replyWarning = "\n\nThis email was sent from an automated system and replies to it will not reach a real person."

	def link(n: Notification[_, _], recipient: User): String = {
		val urlFromNotification = n.urlFor(recipient)
		val url = if (urlFromNotification.startsWith("https://")) {
			urlFromNotification
		} else {
			topLevelUrl + urlFromNotification
		}

		if (n.isInstanceOf[ActionRequiredNotification]) {
			s"\n\nYou need to ${n.urlTitle}. Please visit $url"
		} else {
			s"\n\nTo ${n.urlTitle}, please visit $url"
		}
	}

	// add an isEmail property for the model for emails
	def render(model: FreemarkerModel): String = {
		textRenderer.renderTemplate(model.template, model.model + ("isEmail" -> true))
	}

	private def generateMessage(recipientInfo: RecipientNotificationInfo): Option[MimeMessage] = {
		try {
			val notification = recipientInfo.notification
			val recipient = recipientInfo.recipient

			Some(createMessage(mailSender) { message =>
				message.setFrom(fromAddress)
				message.setReplyTo(replyAddress)
				message.setTo(recipient.getEmail)
				message.setSubject(notification.title)

				val content: String = {
					// Access to restricted properties requires user inside RequestInfo
					val currentUser = new CurrentUser(recipient, recipient)
					val info = new RequestInfo(
						user = currentUser,
						requestedUri = null,
						requestParameters = Map()
					)
					RequestInfo.use(info) {
						render(notification.content)
					}
				}

				val body = new StringBuilder("")
				body.append(mailHeader.format(recipient.getFirstName))
				body.append(content)
				body.append(link(notification, recipient))
				body.append(mailFooter)
				body.append(replyWarning)
				message.setText(body.toString())
				notification match {
					case n: HasNotificationAttachment => n.generateAttachments(message)
					case _ => () // do nothing
				}
			})
		} catch {
			// referenced entity probably missing, oh well.
			case e: ObjectNotFoundException => None
		}
	}

	def listen(recipientInfo: RecipientNotificationInfo): Unit = {
		if (!recipientInfo.emailSent) {
			def cancelSendingEmail() {
				// TODO This is incorrect, really - we're not sending the email, we're cancelling the sending of the email
				recipientInfo.emailSent = true
				service.save(recipientInfo)
			}

			if (recipientInfo.dismissed) {
				logger.info(s"Not sending email for Notification as it is dismissed for $recipientInfo")
				cancelSendingEmail()
			} else if (recipientInfo.notification.priority < Notification.PriorityEmailThreshold) {
				logger.info(s"Not sending email as notification priority ${recipientInfo.notification.priority} below threshold: $recipientInfo")
				cancelSendingEmail()
			} else if (!recipientInfo.recipient.isFoundUser) {
				logger.error(s"Couldn't send email for Notification because usercode didn't match a user: $recipientInfo")
				cancelSendingEmail()
			} else if (recipientInfo.recipient.getEmail.isEmptyOrWhitespace) {
				logger.warn(s"Couldn't send email for Notification because recipient has no email address: $recipientInfo")
				cancelSendingEmail()
			} else if(recipientInfo.recipient.isLoginDisabled) {
				logger.warn(s"Couldn't send email for Notification because recipients login is disabed: $recipientInfo")
				cancelSendingEmail()
			} else {
				generateMessage(recipientInfo) match {
					case Some(message) =>
						val future = mailSender.send(message)
						try {
							val successful = future.get(30, TimeUnit.SECONDS)
							if (successful) {
								recipientInfo.emailSent = true
							}
						} catch {
							case e: TimeoutException =>
								logger.info(s"Timeout waiting for message $message to be sent; cancelling to try again later", e)
								future.cancel(true)
							case e@(_: ExecutionException | _: InterruptedException) =>
								logger.warn(s"Could not send email $message, will try later", e)
						} finally {
							/* TAB-4544 log the time at which we tried to send this notification and save
									* gives us more info for diagnostics
									* allows us to see which mails were cancelled (time attempted = null but sent is true)
									* stops us from trying to send the same notification again and again if sending fails
									 * (don't send ones that we tried to send in the last 5 mins or so)
							 */
							recipientInfo.attemptedAt = DateTime.now
							service.save(recipientInfo)
						}
					case None =>
						logger.warn(s"Couldn't send email for Notification because object no longer exists: $recipientInfo")

						cancelSendingEmail()
				}
			}
		}
	}

}