package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.notifications.coursework.MarkedPlagiarisedNotification
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.{Notification, Submission, Assignment}
import uk.ac.warwick.tabula.services.{AutowiringSubmissionServiceComponent, SubmissionServiceComponent}
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.{InvestigationCompleted, SuspectPlagiarised}
import uk.ac.warwick.userlookup.User

object PlagiarismInvestigationCommand {
	def apply(assignment: Assignment, _user: User) =
		new PlagiarismInvestigationCommandInternal(assignment)
			with ComposableCommand[Unit]
			with PlagiarismInvestigationCommandPermissions
			with PlagiarismInvestigationCommandDescription
			with PlagiarismInvestigationCommandValidation
			with PlagiarismInvestigationCommandNotification
			with UserAware
			with AutowiringSubmissionServiceComponent {
			val user: User = _user
		}
}

class PlagiarismInvestigationCommandInternal(val assignment: Assignment)
	extends CommandInternal[Unit] with PlagiarismInvestigationCommandState {

	self: SubmissionServiceComponent =>

	def applyInternal(): Unit = {
		submissions = students.asScala.flatMap(submissionService.getSubmissionByUsercode(assignment, _))
		submissions.foreach { submission =>
			submission.plagiarismInvestigation =
				if (markPlagiarised) SuspectPlagiarised
				else InvestigationCompleted
			submissionService.saveSubmission(submission)
		}
	}
}

trait PlagiarismInvestigationCommandValidation extends SelfValidating {
	self: PlagiarismInvestigationCommandState =>
	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}
}

trait PlagiarismInvestigationCommandState {
	val assignment: Assignment

	var students: JList[String] = JArrayList()
	var confirm: Boolean = false
	var markPlagiarised: Boolean = true

	var submissions: Seq[Submission] = Seq()
}

trait PlagiarismInvestigationCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: PlagiarismInvestigationCommandState =>
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Submission.ManagePlagiarismStatus, mandatory(assignment))
	}
}

trait PlagiarismInvestigationCommandDescription extends Describable[Unit] {
	self: PlagiarismInvestigationCommandState =>

	def describe(d: Description) {
		d.assignment(assignment)
			.submissions(submissions)
			.property("submissionCount" -> submissions.size)
			.property("markedPlagarised" -> markPlagiarised)
	}

	override def describeResult(d: Description) {
		d.assignment(assignment)
			.submissions(submissions)
			.property("submissionCount" -> submissions.size)
			.property("markedPlagarised" -> markPlagiarised)
	}
}

trait PlagiarismInvestigationCommandNotification extends Notifies[Unit, Unit] {
	self: PlagiarismInvestigationCommandState with UserAware =>

	def emit(result: Unit): Seq[MarkedPlagiarisedNotification] = if(markPlagiarised) {
		submissions.map(s=> Notification.init(new MarkedPlagiarisedNotification, user, s, s.assignment))
	} else {
		Seq()
	}
}