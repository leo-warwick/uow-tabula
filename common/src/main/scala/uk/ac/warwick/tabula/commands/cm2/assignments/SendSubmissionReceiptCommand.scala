package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Command, Description, Notifies, ReadOnly}
import uk.ac.warwick.tabula.data.model.notifications.coursework.SubmissionReceiptNotification
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Notification, Submission}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._

import scala.language.implicitConversions

/**
 * Send an email confirming the receipt of a submission to the student
 * who submitted it.
 */
class SendSubmissionReceiptCommand(val assignment: Assignment, val submission: Submission, val user: CurrentUser)
	extends Command[Boolean] with Notifies[Boolean, Submission] with ReadOnly {

	PermissionCheck(Permissions.Submission.SendReceipt, mandatory(submission))

	val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("d MMMM yyyy 'at' HH:mm:ss")

	def applyInternal(): Boolean = {
		if (user.email.hasText) {
			true
		} else {
			false
		}
	}

	override def describe(d: Description) {
		d.assignment(assignment)
	}

	def emit(sendNotification: Boolean): Seq[SubmissionReceiptNotification] = {
		if (sendNotification) {
			Seq(Notification.init(new SubmissionReceiptNotification, user.apparentUser, Seq(submission), assignment))
		} else {
			Nil
		}
	}

}