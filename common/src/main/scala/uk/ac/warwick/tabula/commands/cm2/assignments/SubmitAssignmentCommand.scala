package uk.ac.warwick.tabula.commands.cm2.assignments

import org.apache.commons.collections.Factory
import org.apache.commons.collections.map.LazyMap
import org.joda.time.DateTime
import org.springframework.util.Assert
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.events.{NotificationHandling, TriggerHandling}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser, FeaturesComponent}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{BooleanFormValue, FormValue, SavedFormValue}
import uk.ac.warwick.tabula.data.model.notifications.coursework._
import uk.ac.warwick.tabula.data.model.triggers.{SubmissionAfterCloseDateTrigger, Trigger}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringCourseworkSubmissionServiceComponent, AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object SubmitAssignmentCommand {
	type SubmitAssignmentCommand = Appliable[Submission] with SubmitAssignmentRequest

	def self(module: Module, assignment: Assignment, user: CurrentUser) =
		new SubmitAssignmentCommandInternal(module, assignment, MemberOrUser(user.profile, user.apparentUser))
			with ComposableCommand[Submission]
			with SubmitAssignmentBinding
			with SubmitAssignmentAsSelfPermissions
			with SubmitAssignmentDescription
			with SubmitAssignmentValidation
			with SubmitAssignmentNotifications
			with SubmitAssignmentTriggers
			with AutowiringSubmissionServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringZipServiceComponent
			with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent

	def onBehalfOf(module: Module, assignment: Assignment, member: Member) =
		new SubmitAssignmentCommandInternal(module, assignment, MemberOrUser(member))
			with ComposableCommand[Submission]
			with SubmitAssignmentBinding
			with SubmitAssignmentOnBehalfOfPermissions
			with SubmitAssignmentDescription
			with SubmitAssignmentValidation
			with SubmitAssignmentNotifications
			with SubmitAssignmentTriggers
			with AutowiringSubmissionServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringZipServiceComponent
			with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent
}

trait SubmitAssignmentState {
	def module: Module
	def assignment: Assignment
	def user: MemberOrUser
}

trait SubmitAssignmentRequest extends SubmitAssignmentState {

	var fields: JMap[String, FormValue] = {
		/*
		 * Goes through the assignment's fields building a set of empty FormValue
		 * objects that can be attached to the form and used for binding form values.
		 * The key is the form field's ID, so binding should be impervious to field reordering,
		 * though it will fail if a field is removed between a user loading a submission form
		 * and submitting it.
		 */

		val fields = JHashMap(assignment.submissionFields.map { field => field.id -> field.blankFormValue }.toMap)

		LazyMap.decorate(fields, new Factory {
			def create() = new FormValue {
				val field = null
				def persist(value: SavedFormValue) {}
			}
		}).asInstanceOf[JMap[String, FormValue]]
	}

	var useDisability: JBoolean = _

	var plagiarismDeclaration: Boolean = false

	// used as a hint to the view.
	var justSubmitted: Boolean = false

}

abstract class SubmitAssignmentCommandInternal(val module: Module, val assignment: Assignment, val user: MemberOrUser)
	extends CommandInternal[Submission] with SubmitAssignmentRequest {

	self: SubmissionServiceComponent
		with FeaturesComponent
		with ZipServiceComponent
		with AttendanceMonitoringCourseworkSubmissionServiceComponent
		with TriggerHandling =>

	override def applyInternal(): Submission = transactional() {
		assignment.submissions.asScala.find(_.isForUser(user.asUser)).foreach { existingSubmission =>
			if (assignment.resubmittable(user.asUser)) {
				triggerService.removeExistingTriggers(existingSubmission)
				submissionService.delete(existingSubmission)
			} else { // Validation should prevent ever reaching here.
				throw new IllegalArgumentException("Submission already exists and can't overwrite it")
			}
		}

		val submitterMember = user.asMember

		val submission = new Submission
		submission.assignment = assignment
		submission.submitted = true
		submission.submittedDate = new DateTime
		submission.usercode = user.usercode
		submission._universityId = user.universityId

		val savedValues = fields.asScala.map {
			case (_, submissionValue) =>
				val value = new SavedFormValue()
				value.name = submissionValue.field.name
				value.submission = submission
				submissionValue.persist(value)
				value
		}.toBuffer

		if (features.disabilityOnSubmission && submitterMember.exists{
			case student: StudentMember => student.disability.exists(_.reportable)
			case _ => false
		} && useDisability != null) {
			val useDisabilityValue = new BooleanFormValue(null)
			useDisabilityValue.value = useDisability
			val value = new SavedFormValue
			value.name = Submission.UseDisabilityFieldName
			value.submission = submission
			useDisabilityValue.persist(value)
			savedValues.append(value)
		}

		submission.values = savedValues.toSet[SavedFormValue].asJava

		// TAB-413 assert that we have at least one attachment
		Assert.isTrue(
			submission.values.asScala.exists(value => Option(value.attachments).isDefined && !value.attachments.isEmpty),
			"Submission must have at least one attachment"
		)

		zipService.invalidateSubmissionZip(assignment)
		submissionService.saveSubmission(submission)

		attendanceMonitoringCourseworkSubmissionService.updateCheckpoints(submission)

		submission
	}
}

trait SubmitAssignmentBinding extends BindListener {
	self: SubmitAssignmentRequest =>

	override def onBind(result:BindingResult) {
		for ((key, field) <- fields.asScala) field.onBind(result)
	}
}

trait SubmitAssignmentAsSelfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SubmitAssignmentState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Submission.Create, assignment)
	}
}

trait SubmitAssignmentOnBehalfOfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SubmitAssignmentState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(mandatory(assignment), mandatory(module))
		p.PermissionCheck(Permissions.Submission.CreateOnBehalfOf, assignment)
		p.PermissionCheck(Permissions.Submission.CreateOnBehalfOf, mandatory(user.asMember))
	}
}

trait SubmitAssignmentValidation extends SelfValidating {
	self: SubmitAssignmentRequest
		with FeaturesComponent =>

	override def validate(errors: Errors) {
		if (!assignment.isOpened) {
			errors.reject("assignment.submit.notopen")
		}

		if (!assignment.collectSubmissions) {
			errors.reject("assignment.submit.disabled")
		}

		val hasExtension = assignment.isWithinExtension(user.asUser)

		if (!assignment.allowLateSubmissions && (assignment.isClosed && !hasExtension)) {
			errors.reject("assignment.submit.closed")
		}

		// HFC-164
		if (assignment.submissions.asScala.exists(_.usercode == user.usercode)) {
			if (assignment.allowResubmission) {
				if (assignment.allowLateSubmissions && (assignment.isClosed && !hasExtension)) {
					errors.reject("assignment.resubmit.closed")
				}
			} else {
				errors.reject("assignment.submit.already")
			}
		}

		if (assignment.displayPlagiarismNotice && !plagiarismDeclaration) {
			errors.rejectValue("plagiarismDeclaration", "assignment.submit.plagiarism")
		}

		// TODO for multiple attachments, check filenames are unique

		// Individually validate all the custom fields
		// If a submitted ID is not found in assignment, it's ignored.
		assignment.submissionFields.foreach { field =>
			errors.pushNestedPath("fields[%s]".format(field.id))
			fields.asScala.get(field.id).foreach { field.validate(_, errors) }
			errors.popNestedPath()
		}

		if (features.disabilityOnSubmission && user.asMember.exists {
			case student: StudentMember => student.disability.exists(_.reportable)
			case _ => false
		} && useDisability == null) {
			errors.rejectValue("useDisability", "assignment.submit.chooseDisability")
		}
	}
}

trait SubmitAssignmentDescription extends Describable[Submission] {
	self: SubmitAssignmentState =>

	override def describe(d: Description): Unit =	{
		d.assignment(assignment)

		assignment.submissions.asScala.find(_.usercode == user.usercode).map { existingSubmission =>
			d.properties(
				"existingSubmission" -> existingSubmission.id,
				"existingAttachments" -> existingSubmission.allAttachments.map { _.id }
			)
		}
	}

	override def describeResult(d: Description, s: Submission): Unit = {
		d.assignment(assignment).properties("submission" -> s.id).fileAttachments(s.allAttachments)
		if (s.isNoteworthy)
			d.properties("submissionIsNoteworthy" -> true)
	}
}

trait SubmitAssignmentNotifications extends Notifies[Submission, Submission] with CompletesNotifications[Submission] {
	self: SubmitAssignmentState with NotificationHandling =>

	override def emit(submission: Submission): Seq[SubmissionNotification] = {
		val studentNotifications =
			if (assignment.isVisibleToStudents)
				Seq(Notification.init(new SubmissionReceiptNotification, user.asUser, Seq(submission), assignment))
			else Seq()

		studentNotifications ++ Seq(Notification.init(new SubmissionReceivedNotification, user.asUser, Seq(submission), assignment))
	}

	override def notificationsToComplete(commandResult: Submission): CompletesNotificationsResult = {
		CompletesNotificationsResult(
			notificationService.findActionRequiredNotificationsByEntityAndType[SubmissionDueGeneralNotification](assignment) ++
				assignment.findExtension(user.usercode).map(
					notificationService.findActionRequiredNotificationsByEntityAndType[SubmissionDueWithExtensionNotification]
				).getOrElse(Seq()),
			user.asUser
		)
	}
}

trait SubmitAssignmentTriggers extends GeneratesTriggers[Submission] {
	self: TriggerHandling =>

	override def generateTriggers(commandResult: Submission): Seq[Trigger[_ >: Null <: ToEntityReference, _]] = {
		if (commandResult.isLate || commandResult.isAuthorisedLate) {
			Seq(SubmissionAfterCloseDateTrigger(DateTime.now, commandResult))
		} else {
			Seq()
		}
	}
}