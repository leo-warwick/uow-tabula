package uk.ac.warwick.tabula.coursework.commands.assignments

import uk.ac.warwick.tabula.commands.{Notifies, Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{Notification, Submission, Assignment, Module}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.web.views.{FreemarkerTextRenderer}
import uk.ac.warwick.tabula.permissions._
import language.implicitConversions
import uk.ac.warwick.tabula.coursework.commands.assignments.notifications.SubmissionRecieptNotification

/**
 * Send an email confirming the receipt of a submission to the student
 * who submitted it.
 */
class SendSubmissionReceiptCommand(val module: Module, val assignment: Assignment, val submission: Submission, val user: CurrentUser)
	extends Command[Boolean] with Notifies[Submission] with ReadOnly {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.SendReceipt, mandatory(submission))

	val dateFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")

	def applyInternal() = {
		if (user.email.hasText) {
			true
		} else {
			false
		}
	}

	override def describe(d: Description) {
		d.assignment(assignment)
	}

	def emit: Seq[Notification[Submission]] = {
		if (user.email.hasText) {
			Seq(new SubmissionRecieptNotification(submission, user.apparentUser) with FreemarkerTextRenderer)
		} else {
			Nil
		}
	}

}