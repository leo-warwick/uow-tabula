package uk.ac.warwick.tabula.coursework.commands.feedback

import scala.collection.JavaConversions._
import scala.beans.BeanProperty
import org.springframework.mail.MailException
import uk.ac.warwick.tabula.data.Transactions._
import org.springframework.validation.Errors
import freemarker.template.Configuration
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.helpers.ArrayList
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.util.mail.WarwickMailSender
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.FeedbackService


class PublishFeedbackCommand(val module: Module, val assignment: Assignment) extends Command[Unit] with FreemarkerRendering with SelfValidating with UnicodeEmails {

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Feedback.Publish, assignment)
	
	var studentMailSender = Wire[WarwickMailSender]("studentMailSender")
	var feedbackService = Wire.auto[FeedbackService]
	var userLookup = Wire.auto[UserLookupService]
	implicit var freemarker = Wire.auto[Configuration]

	var replyAddress = Wire.property("${mail.noreply.to}")
	var fromAddress = Wire.property("${mail.exceptions.to}")
	
	@BeanProperty var confirm: Boolean = false

	case class MissingUser(universityId: String)
	case class BadEmail(user: User, exception: Exception = null)

	var missingUsers: JList[MissingUser] = ArrayList()
	var badEmails: JList[BadEmail] = ArrayList()

	// validation done even when showing initial form.
	def prevalidate(errors: Errors) {
		if (!assignment.openEnded && !assignment.isClosed()) {
			errors.reject("feedback.publish.notclosed")
		} else if (assignment.fullFeedback.isEmpty) {
			errors.reject("feedback.publish.nofeedback")
		}
	}

	def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) {
			errors.rejectValue("confirm", "feedback.publish.confirm")
		}
	}

	def applyInternal() {
		transactional() {
			val users = getUsersForFeedback
			for ((studentId, user) <- users) {
				val feedbacks = assignment.fullFeedback.find { _.universityId == studentId }
				for (feedback <- feedbacks)
					feedback.released = true
			}
			for (info <- users) email(info)
		}
	}

	private def email(info: Pair[String, User]) {
		val (id, user) = info
		if (user.isFoundUser) {
			val email = user.getEmail
			if (email.hasText) {
				val message = messageFor(user)
				try {
					studentMailSender.send(message)
				} catch {
					case e: MailException => badEmails add BadEmail(user, exception = e)
				}
			} else {
				badEmails add BadEmail(user)
			}
		} else {
			missingUsers add MissingUser(id)
		}
	}
	
	def getUsersForFeedback = feedbackService.getUsersForFeedback(assignment)

	private def messageFor(user: User) = createMessage(studentMailSender) { message =>
		val moduleCode = assignment.module.code.toUpperCase
		message.setFrom(fromAddress)
		message.setReplyTo(replyAddress)
		message.setTo(user.getEmail)
		// TODO configurable subject
		message.setSubject(encodeSubject(moduleCode + ": Your coursework feedback is ready"))
		// TODO configurable body
		message.setText(messageTextFor(user))
	}

	def describe(d: Description) = d 
		.assignment(assignment)
		.studentIds(getUsersForFeedback map { case(userId, user) => user.getWarwickId })

	def messageTextFor(user: User) =
		renderToString("/WEB-INF/freemarker/emails/feedbackready.ftl", Map(
			"name" -> user.getFirstName,
			"assignmentName" -> assignment.name,
			"moduleCode" -> assignment.module.code.toUpperCase,
			"moduleName" -> assignment.module.name,
			"path" -> Routes.assignment.receipt(assignment)
		))

}