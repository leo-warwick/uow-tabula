package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.LocalDate
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Info, Warning}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

trait FeedbackDueNotification extends AllCompletedActionRequiredNotification {

	self : Notification[_, Unit] with NotificationPreSaveBehaviour =>

	protected def deadline: Option[LocalDate]
	protected def assignment: Assignment

	@transient private lazy val workingDaysHelper = new WorkingDaysHelperImpl

	protected def daysLeft: Int =
		deadline.map { d =>
			val now = created.toLocalDate

			// need an offset, as the helper always includes both start and end date, off-by-one from what we want to show
			val offset =
				if (d.isBefore(now)) 1
				else -1 // today or in the future

			workingDaysHelper.getNumWorkingDays(now, d) + offset
		}.getOrElse(Integer.MAX_VALUE)

	protected def dueToday: Boolean = deadline.contains(created.toLocalDate)

	override final def onPreSave(newRecord: Boolean) {
		priority = if (daysLeft == 1) {
			Warning
		} else if (daysLeft < 1) {
			Critical
		} else {
			Info
		}
	}

	override final def verb = "publish"

	override final def urlTitle = "publish this feedback"

	override def url: String = Routes.admin.assignment.submissionsandfeedback(assignment)

}

@Entity
@DiscriminatorValue("FeedbackDueGeneral")
class FeedbackDueGeneralNotification
	extends Notification[Assignment, Unit] with SingleItemNotification[Assignment] with FeedbackDueNotification {

	override final def assignment: Assignment = item.entity

	override final def title: String = "%s: Feedback for \"%s\" is due to be published".format(assignment.module.code.toUpperCase, assignment.name)

	override final def recipients: Seq[User] = {
		if (deadline.nonEmpty && assignment.needsFeedbackPublishingIgnoreExtensions) {
			val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
			moduleAndDepartmentService.getModuleByCode(assignment.module.code)
				.getOrElse(throw new IllegalStateException("No such module"))
				.managers.users
		} else {
			Seq()
		}
	}

	override final def deadline: Option[LocalDate] = assignment.feedbackDeadline

	override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/feedback_reminder_general.ftl", Map(
		"assignment" -> assignment,
		"daysLeft" -> daysLeft,
		"dateOnlyFormatter" -> dateOnlyFormatter,
		"deadline" -> deadline,
		"dueToday" -> dueToday
	))
}

@Entity
@DiscriminatorValue("FeedbackDueExtension")
class FeedbackDueExtensionNotification
	extends Notification[Extension, Unit] with SingleItemNotification[Extension] with FeedbackDueNotification {

	final def extension: Extension = item.entity

	override final def assignment: Assignment = extension.assignment
	def submission: Option[Submission] = assignment.findSubmission(extension.usercode)

	override final def title: String = "%s: Feedback for %s for \"%s\" is due to be published".format(assignment.module.code.toUpperCase, extension.studentIdentifier, assignment.name)

	override final def recipients: Seq[User] = {
		// only send to recipients if the assignments needs feedback publishing and the student actually submitted
		if (submission.nonEmpty && deadline.nonEmpty && assignment.needsFeedbackPublishingFor(extension.usercode)) {
			val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
			moduleAndDepartmentService.getModuleByCode(assignment.module.code)
				.getOrElse(throw new IllegalStateException("No such module"))
				.managers.users
		} else {
			Seq()
		}
	}

	override final def deadline: Option[LocalDate] = extension.feedbackDeadline.map(_.toLocalDate)

	override def content: FreemarkerModel = FreemarkerModel("/WEB-INF/freemarker/notifications/feedback_reminder_extension.ftl", Map(
		"extension" -> extension,
		"assignment" -> assignment,
		"daysLeft" -> daysLeft,
		"dateOnlyFormatter" -> dateOnlyFormatter,
		"deadline" -> deadline,
		"dueToday" -> dueToday
	))
}
