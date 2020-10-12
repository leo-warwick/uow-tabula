package uk.ac.warwick.tabula.system.exceptions

import java.io.{IOException, PrintWriter, StringWriter}

import com.itextpdf.text.ExceptionConverter
import freemarker.template.{Template, Configuration => FreemarkerConfiguration}
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest
import org.joda.time.DateTime
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.mail.MailException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.helpers.{Logging, UnicodeEmails}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.util.mail.WarwickMailSender

import scala.jdk.CollectionConverters._

case class ExceptionContext(token: String, exception: Throwable, request: Option[HttpServletRequest] = None)

object ExceptionHandler {
  def renderStackTrace(exception: Throwable): String = {
    val stringWriter = new StringWriter
    val writer = new PrintWriter(stringWriter)
    exception.printStackTrace(writer)
    stringWriter.toString
  }

  // Check for this exception without needing it on the classpath
  private val ClientAbortException = "org.apache.catalina.connector.ClientAbortException"

  def isClientAbortException(e: IOException): Boolean = e.getClass.getName == ClientAbortException
  def isAlreadyHandledException(e: IllegalStateException): Boolean = e.getMessage == "getOutputStream() has already been called for this response"
}

trait ExceptionHandler {
  def exception(context: ExceptionContext): Unit
}

class CompositeExceptionHandler(handlers: JList[ExceptionHandler]) extends ExceptionHandler {
  private val _handlers = handlers.asScala.toList

  override def exception(context: ExceptionContext): Unit =
    for (handler <- _handlers) handler.exception(context)
}

class LoggingExceptionHandler extends ExceptionHandler with Logging {
  override def exception(context: ExceptionContext): Unit = context.exception match {
    case userError: UserError => if (debugEnabled) logger.debug("User error", userError)
    case handled: HandledException => if (debugEnabled) logger.debug("Handled exception", handled)
    case e => logger.error("Exception " + context.token, e)
  }
}

class EmailingExceptionHandler extends ExceptionHandler with Logging with InitializingBean with FreemarkerRendering with UnicodeEmails {
  @Resource(name = "mailSender") var mailSender: WarwickMailSender = _
  @Value("${mail.exceptions.to}") var recipient: String = _
  @Value("${environment.production}") var production: Boolean = _
  @Value("${environment.standby}") var standby: Boolean = _
  @Autowired var freemarker: FreemarkerConfiguration = _
  var template: Template = _

  override def exception(context: ExceptionContext): Unit = context.exception match {
    case _: UserError =>
    case _: HandledException =>
    case e: IOException if ExceptionHandler.isClientAbortException(e) => // cancelled download.
    case e: IllegalStateException if ExceptionHandler.isAlreadyHandledException(e) => // error page already displayed.
    case e: ExceptionConverter if e.getException.isInstanceOf[IOException] && ExceptionHandler.isClientAbortException(e.getException.asInstanceOf[IOException]) => // cancelled PDF download
    case _ =>
      try {
        val message = makeEmail(context)
        mailSender.send(message)
      } catch {
        case e: MailException => logger.error("Error emailing exception " + context.token + "!", e)
      }
  }

  private def makeEmail(context: ExceptionContext) = createMessage(mailSender, multipart = false) { message =>
    val info = RequestInfo.fromThread

    val env = if (production) "PROD" else "TEST"

    message.setTo(recipient)
    message.setSubject("[HFCX] (%s) %s %s".format(env, userId(info), context.token))
    message.setText(renderToString(template, Map(
      "token" -> context.token,
      "exception" -> context.exception,
      "exceptionStack" -> ExceptionHandler.renderStackTrace(context.exception),
      "requestInfo" -> info,
      "time" -> new DateTime,
      "request" -> context.request,
      "environment" -> env,
      "standby" -> standby)))
  }

  private def userId(info: Option[RequestInfo]) = info.map(_.user).map(_.realId).getOrElse("ANON")

  override def afterPropertiesSet(): Unit = {
    template = freemarker.getTemplate("/WEB-INF/freemarker/emails/exception.ftl")
  }
}
