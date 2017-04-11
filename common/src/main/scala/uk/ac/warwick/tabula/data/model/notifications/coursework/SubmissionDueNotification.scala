package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.{DateTime, Days}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AutowiringUserLookupComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

trait SubmissionReminder extends RecipientCompletedActionRequiredNotification {
	self : Notification[_, Unit] with NotificationPreSaveBehaviour =>

	def deadline: DateTime
	def assignment: Assignment
	def module: Module = assignment.module
	def moduleCode: String = module.code.toUpperCase

	def referenceDate: DateTime = created

	def daysLeft: Int = {
		val now = referenceDate.withTimeAtStartOfDay()
		val closeDate = deadline.withTimeAtStartOfDay()
		Days.daysBetween(now, closeDate).getDays
	}

	override final def onPreSave(newRecord: Boolean) {
		priority = if (daysLeft == 1) {
			Warning
		} else if (daysLeft < 1) {
			Critical
		} else {
			Info
		}
	}

	def url: String = Routes.assignment(assignment)

	def urlTitle = "upload your submission"

	def title = s"$moduleCode: Your submission for '${assignment.name}' $timeStatement"

	def timeStatement: String = if (daysLeft > 1){
		s"is due in $daysLeft days"
	} else if (daysLeft == 1) {
		"is due tomorrow"
	} else if (daysLeft == 0) {
		"is due today"
	} else if (daysLeft == -1) {
		"is 1 day late"
	} else {
		s"is ${0 - daysLeft} days late"
	}

	def be: String = if (daysLeft >= 0) "is" else "was"
	def deadlineDate: String = be + " " + dateTimeFormatter.print(deadline)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/submission_reminder.ftl", Map(
		"assignment" -> assignment,
		"module" -> module,
		"timeStatement" -> timeStatement,
		"cantSubmit" -> (!assignment.allowLateSubmissions && DateTime.now.isAfter(deadline)),
		"deadlineDate" -> deadlineDate
	))

	def verb = "Remind"

	def shouldSend: Boolean = assignment.collectSubmissions && !assignment.openEnded && assignment.isVisibleToStudents

}

@Entity
@DiscriminatorValue("SubmissionDueGeneral")
class SubmissionDueGeneralNotification extends Notification[Assignment, Unit] with SingleItemNotification[Assignment]
	with SubmissionReminder {

	@transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

	def deadline: DateTime = assignment.closeDate
	def assignment: Assignment = item.entity

	def recipients: Seq[User] = {
		if (!shouldSend)
			Nil
		else {
			val submissions = assignment.submissions.asScala
			val extensions = assignment.extensions.asScala.filter(_.approved) // TAB-2303
			val allStudents = membershipService.determineMembershipUsers(assignment)
			// first filter out students that have submitted already
			val withoutSubmission = allStudents.filterNot(user => submissions.exists(_.isForUser(user)))
			// finally filter students that have an approved extension
			withoutSubmission.filterNot(user => extensions.exists(_.isForUser(user)))
		}
	}
}

@Entity
@DiscriminatorValue("SubmissionDueExtension")
class SubmissionDueWithExtensionNotification extends Notification[Extension, Unit] with SingleItemNotification[Extension]
	with SubmissionReminder with AutowiringUserLookupComponent {

	def extension: Extension = item.entity

	def deadline: DateTime = extension.expiryDate.getOrElse(
		throw new IllegalArgumentException("Can't send an SubmissionDueWithExtensionNotification without a deadline")
	)

	def assignment: Assignment = extension.assignment

	def recipients: Seq[User] = {
		val hasSubmitted = assignment.submissions.asScala.exists(_.usercode == extension.usercode)

		// Don't send if the user has submitted or if there's no expiry date on the extension (i.e. it's been rejected)
		if (hasSubmitted || (!extension.approved || extension.expiryDate.isEmpty) || !shouldSend) {
			Nil
		} else {
			Seq(userLookup.getUserByUserId(extension.usercode))
		}
	}

}
