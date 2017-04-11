package uk.ac.warwick.tabula.services

import java.util.concurrent.Future
import javax.mail.Message.RecipientType
import javax.mail.internet.{MimeMessage, MimeMultipart}

import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.mail.SimpleMailMessage
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.{Logging, UnicodeEmails}
import uk.ac.warwick.util.mail.WarwickMailSender

import scala.language.implicitConversions
import scala.reflect.ClassTag

final class RedirectingMailSender(delegate: WarwickMailSender) extends WarwickMailSender with Logging with UnicodeEmails {

	@Autowired var features: Features = _

	@Value("${redirect.test.emails.to}") var testEmailTo: String = _

	val nonProductionMessage1 = "This is a copy of a message that isn't being sent to the real recipients ("
	val nonProductionMessage2 = ") because it is being sent from a non-production server.\n\n-------\n\n"

	override def createMimeMessage(): MimeMessage = delegate.createMimeMessage()

	override def send(message: MimeMessage): Future[JBoolean] = {
		val messageToSend = if (!features.emailStudents) {
			prepareMessage(message) { helper =>
				val oldTo = message.getRecipients(RecipientType.TO).map({_.toString}).mkString(", ")

				helper.setTo(Array(testEmailTo))
				helper.setBcc(Array(): Array[String])
				helper.setCc(Array(): Array[String])

				message.getContent match {
					case contentString: String =>
						helper.setText(s"$nonProductionMessage1$oldTo$nonProductionMessage2$contentString")
					case multipart: MimeMultipart =>
						val bodyPart = multipart.getBodyPart(0)
						val oldBodyContent = bodyPart.getContent.toString
						bodyPart.setText(s"$nonProductionMessage1$oldTo$nonProductionMessage2$oldBodyContent")
				}

			}
		} else message

		delegate.send(messageToSend)
	}

	implicit def ArrayOrEmpty[A: ClassTag](a: Array[A]) = new {
		def orEmpty: Array[A] = Option(a).getOrElse(Array.empty)
	}

	override def send(simpleMessage: SimpleMailMessage): Future[JBoolean] = send(createMessage(delegate) { message =>
		Option(simpleMessage.getFrom) foreach {message.setFrom}
		Option(simpleMessage.getReplyTo) foreach {message.setReplyTo}

		Option(simpleMessage.getTo) foreach {message.setTo}
		message.setCc(Option(simpleMessage.getCc).getOrElse(Array(): Array[String]))
		message.setBcc(Option(simpleMessage.getBcc).getOrElse(Array(): Array[String]))

		Option(simpleMessage.getSubject) foreach {message.setSubject}
		Option(simpleMessage.getText) foreach {message.setText}
	})

}