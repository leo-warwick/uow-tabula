package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.{DateTime, Days}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AutowiringUserLookupComponent}

import scala.collection.JavaConverters._

trait SubmissionReminder extends RecipientCompletedActionRequiredNotification {
	self : Notification[_, Unit] with NotificationPreSaveBehaviour =>

	def deadline: DateTime
	def assignment: Assignment
	def module = assignment.module
	def moduleCode = module.code.toUpperCase

	def referenceDate = created

	def daysLeft = {
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

	def url = Routes.assignment(assignment)

	def urlTitle = "upload your submission"

	def title = s"$moduleCode: Your submission for '${assignment.name}' $timeStatement"

	def timeStatement = if (daysLeft > 1){
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

	def be = if (daysLeft >= 0) "is" else "was"
	def deadlineDate = be + " " + dateTimeFormatter.print(deadline)

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

	@transient var membershipService = Wire[AssessmentMembershipService]

	def deadline = assignment.closeDate
	def assignment = item.entity

	def recipients = {
		if (!shouldSend)
			Nil
		else {
			val submissions = assignment.submissions.asScala
			val extensions = assignment.extensions.asScala.filter(_.approved) // TAB-2303
			val allStudents = membershipService.determineMembershipUsers(assignment)
			// first filter out students that have submitted already
			val withoutSubmission = allStudents.filterNot(user => submissions.exists(_.universityId == user.getWarwickId))
			// finally filter students that have an approved extension
			withoutSubmission.filterNot(user => extensions.exists(_.universityId == user.getWarwickId))
		}
	}
}

@Entity
@DiscriminatorValue("SubmissionDueExtension")
class SubmissionDueWithExtensionNotification extends Notification[Extension, Unit] with SingleItemNotification[Extension]
	with SubmissionReminder with AutowiringUserLookupComponent {

	def extension = item.entity

	def deadline = extension.expiryDate.getOrElse(
		throw new IllegalArgumentException("Can't send an SubmissionDueWithExtensionNotification without a deadline")
	)

	def assignment = extension.assignment

	def recipients = {
		val hasSubmitted = assignment.submissions.asScala.exists(_.universityId == extension.universityId)

		if (hasSubmitted || !shouldSend) {
			Nil
		} else {
			Seq(userLookup.getUserByWarwickUniId(extension.universityId))
		}
	}

}
