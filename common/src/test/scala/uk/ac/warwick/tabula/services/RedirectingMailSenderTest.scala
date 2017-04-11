package uk.ac.warwick.tabula.services
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.mail.AsynchronousWarwickMailSender
import org.springframework.mail.SimpleMailMessage
import org.junit.Test
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.util.mail.WarwickMailSender
import javax.mail.internet.MimeMessage
import javax.mail.internet.InternetAddress
import javax.mail.Message.RecipientType
import javax.mail.Session
import java.util.Properties


class RedirectingMailSenderTest extends TestBase with Mockito {
	@Test def barelyTest {
	  val delegate = mock[WarwickMailSender]

	  val session = Session.getDefaultInstance(new Properties)
	  val mimeMessage = new MimeMessage(session)
	  delegate.createMimeMessage() returns mimeMessage

	  val text = """
		CONGRATS

		You have won a bike. Pick it up from the basement.

		(fools)
	  		"""

	  val sender = new RedirectingMailSender(delegate)
	  sender.features = emptyFeatures
	  sender.testEmailTo = "test@warwick.ac.uk"

	  val message = new SimpleMailMessage
	  message.setTo(Array("ron@example.com", "jim@example.net"))
	  message.setFrom("no-reply@example.com")
	  message.setText(text)
	  sender.send(message)

	  // This passes because the sender changes the mime message object.
	  // It should probably make a copy.
	  verify(delegate, times(1)).send(mimeMessage)
	}
}