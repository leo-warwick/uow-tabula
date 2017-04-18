package uk.ac.warwick.tabula.commands.cm2.assignments

import javax.validation.constraints.NotNull

import org.joda.time._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.model.{Assignment, _}
import uk.ac.warwick.tabula.helpers.cm2.{FeedbackListItem, Progress, SubmissionListItem, _}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.{AutowiringCM2WorkflowServiceProgressComponent, CM2WorkflowServiceProgressComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object SubmissionAndFeedbackCommand {
	type CommandType = Appliable[SubmissionAndFeedbackResults]

	def apply(assignment: Assignment) =
		new SubmissionAndFeedbackCommandInternal(assignment)
			with ComposableCommand[SubmissionAndFeedbackResults]
			with SubmissionAndFeedbackRequest
			with SubmissionAndFeedbackPermissions
			with SubmissionAndFeedbackValidation
			with CommandSubmissionAndFeedbackEnhancer
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringCM2WorkflowServiceProgressComponent
			with Unaudited with ReadOnly

case class SubmissionAndFeedbackResults (
	students:Seq[WorkFlowStudent],
	whoDownloaded: Seq[(User, DateTime)],
	stillToDownload: Seq[WorkFlowStudent],
	hasPublishedFeedback: Boolean,
	hasOriginalityReport: Boolean,
	mustReleaseForMarking: Boolean
)
}

trait SubmissionAndFeedbackState {
def assignment: Assignment
def module: Module = assignment.module
}

trait SubmissionAndFeedbackRequest extends SubmissionAndFeedbackState {
@NotNull var filter: Cm2Filter = Cm2Filters.AllStudents
var filterParameters: JMap[String, String] = JHashMap()

// When we call export commands, we may want to further filter by a subset of student IDs
var usercodes: JList[String] = JArrayList()
}

trait SubmissionAndFeedbackEnhancer {
def enhanceSubmissions(): Seq[SubmissionListItem]
def enhanceFeedback(): ListFeedbackResult
}

trait CommandSubmissionAndFeedbackEnhancer extends SubmissionAndFeedbackEnhancer {
self: SubmissionAndFeedbackState =>

val enhancedSubmissionsCommand = ListSubmissionsCommand(module, assignment)
val enhancedFeedbacksCommand = ListFeedbackCommand(module, assignment)

override def enhanceSubmissions(): Seq[SubmissionListItem] = enhancedSubmissionsCommand.apply()
override def enhanceFeedback(): ListFeedbackResult = enhancedFeedbacksCommand.apply()
}

trait SubmissionAndFeedbackValidation extends SelfValidating {
self: SubmissionAndFeedbackRequest =>

override def validate(errors: Errors): Unit = {
Option(filter) foreach { _.validate(filterParameters.asScala.toMap)(errors) }
}
}

trait SubmissionAndFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
self: SubmissionAndFeedbackState =>

override def permissionsCheck(p: PermissionsChecking): Unit = {
mustBeLinked(notDeleted(mandatory(assignment)), mandatory(module))
p.PermissionCheck(Permissions.Submission.Read, assignment)
}
}

abstract class SubmissionAndFeedbackCommandInternal(val assignment: Assignment)
extends CommandInternal[SubmissionAndFeedbackResults] with SubmissionAndFeedbackState with TaskBenchmarking {
self: SubmissionAndFeedbackRequest
with AssessmentMembershipServiceComponent
with UserLookupComponent
with FeedbackForSitsServiceComponent
with ProfileServiceComponent
with SubmissionAndFeedbackEnhancer
with CM2WorkflowServiceProgressComponent =>

override def applyInternal(): SubmissionAndFeedbackResults = {

// an "enhanced submission" is simply a submission with a Boolean flag to say whether it has been downloaded
val enhancedSubmissions = enhanceSubmissions()
val enhancedFeedbacks = enhanceFeedback()
val latestModifiedOnlineFeedback = enhancedFeedbacks.latestOnlineAdded
val whoDownloaded = enhancedFeedbacks.downloads
val whoViewed = enhancedFeedbacks.latestOnlineViews
val latestGenericFeedbackUpdate = enhancedFeedbacks.latestGenericFeedback
val hasOriginalityReport = benchmarkTask("Check for originality reports") {
	enhancedSubmissions.exists(_.submission.hasOriginalityReport)
}
val usercodesWithSubmissionOrFeedback = benchmarkTask("Get usercodes with submissions or feedback") {
	assignment.getUsercodesWithSubmissionOrFeedback.toSeq.sorted
}
val moduleMembers = benchmarkTask("Get module membership") {
	assessmentMembershipService.determineMembershipUsers(assignment)
}
val unsubmittedMembers = moduleMembers.filterNot(m => usercodesWithSubmissionOrFeedback.contains(m.getUserId))

def enhancedFeedbackForUsercode(usercode: String) = {
	val usersFeedback = assignment.feedbacks.asScala.filter(feedback => feedback.usercode == usercode)
	if (usersFeedback.size > 1) throw new IllegalStateException("More than one Feedback for " + usercode)
	usersFeedback.headOption map { feedback =>
		val downloaded = !feedback.attachments.isEmpty && (whoDownloaded exists { case (user, dateTime) =>
			user.getUserId == feedback.usercode &&
				dateTime.isAfter(feedback.mostRecentAttachmentUpload)
		})

		val viewed = (feedback.hasOnlineFeedback || feedback.hasGenericFeedback) && (whoViewed exists { case (user, dateTime) =>
			val usercode = user.getUserId

			val latestOnlineUpdate = latestModifiedOnlineFeedback
				.find{ case (u, _) => user.getUserId == usercode }
				.map { case (_, dt) => dt }
				.getOrElse(new DateTime(0))

			val latestUpdate = latestGenericFeedbackUpdate
				.filter(_.isAfter(latestOnlineUpdate))
				.getOrElse(latestOnlineUpdate)

			usercode == feedback.usercode && dateTime.isAfter(latestUpdate)
		})

		FeedbackListItem(feedback, downloaded, viewed, feedbackForSitsService.getByFeedback(feedback).orNull)
	}
}

val unsubmitted: Seq[WorkFlowStudent] = benchmarkTask("Get unsubmitted users") {
	for (user <- unsubmittedMembers) yield {
		val usersExtension = assignment.extensions.asScala.filter(_.usercode == user.getUserId)
		if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + user.getWarwickId)

		val enhancedExtensionForUniId = usersExtension.headOption.map { extension =>
			new ExtensionListItem(
				extension,
				assignment.isWithinExtension(user)
			)
		}

		val coursework = WorkflowItems(
			user,
			enhancedSubmission = None,
			enhancedFeedback=enhancedFeedbackForUsercode(user.getUserId),
			enhancedExtension=None //enhancedExtensionForUniId
		)

		val progress = cm2WorkflowProgressService.progress(assignment)(coursework)
		WorkFlowStudent(
			user,
			Progress(progress.percentage, progress.cssClass, progress.messageCode),
			progress.nextStage,
			progress.stages,
			coursework,
			assignment,
			disability = None
		)
	}
}


val submitted = benchmarkTask("Get submitted users") { for (usercode <- usercodesWithSubmissionOrFeedback) yield {
	val usersSubmissions = enhancedSubmissions.filter(_.submission.usercode == usercode)
	val usersExtension = assignment.extensions.asScala.filter(extension => extension.usercode == usercode)

	val userFilter = moduleMembers.filter(u => u.getUserId == usercode)
	val user = if(userFilter.isEmpty) {
		userLookup.getUserByUserId(usercode)
	} else {
		userFilter.head
	}

	if (usersSubmissions.size > 1) throw new IllegalStateException("More than one Submission for " + usercode)
	if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + usercode)

	val enhancedSubmissionForUniId = usersSubmissions.headOption

	val enhancedExtensionForUniId = usersExtension.headOption map { extension =>
		new ExtensionListItem(
			extension=extension,
			within=assignment.isWithinExtension(user)
		)
	}

		val enhancedSubmissionForUsercode = usersSubmissions.headOption.map (submission => SubmissionListItem(assignment.findSubmission(usercode).get, downloaded=false))

		val enhancedExtensionForUniUsercode = usersExtension.headOption map { extension =>
			new ExtensionListItem(
				extension,
				assignment.isWithinExtension(user)
			)
		}

		val coursework = WorkflowItems(
			user,
			enhancedSubmissionForUsercode,
			enhancedFeedbackForUsercode(usercode),
			enhancedExtensionForUniUsercode
		)

		val progress = cm2WorkflowProgressService.progress(assignment)(coursework)

		WorkFlowStudent(
			user=user,
			Progress(progress.percentage, progress.cssClass, progress.messageCode),
			nextStage=progress.nextStage,
			stages=progress.stages,
			coursework=coursework,
			assignment=assignment,
			disability = {
			if (enhancedSubmissionForUniId.exists(_.submission.useDisability)) {
				profileService.getMemberByUser(user).flatMap{
					case student: StudentMember => Option(student)
					case _ => None
				}.flatMap(s => s.disability)
			}	else {
				None
			}
		}
	)
}}

val membersWithPublishedFeedback = submitted.filter { student =>
	student.coursework.enhancedFeedback exists { _.feedback.checkedReleased }
}

// True if any feedback exists that's been published. To decide whether to show whoDownloaded count.
val hasPublishedFeedback = membersWithPublishedFeedback.nonEmpty

val stillToDownload = membersWithPublishedFeedback.filterNot(_.coursework.enhancedFeedback.exists(_.downloaded))

val studentsFiltered = benchmarkTask("Do filtering") {
	val allStudents = (unsubmitted ++ submitted).filter(filter.predicate(filterParameters.asScala.toMap))
	val studentsFiltered =
		if (usercodes.isEmpty) allStudents
		else allStudents.filter { student => usercodes.contains(student.user.getUserId) }

	studentsFiltered
}

SubmissionAndFeedbackResults(
	students = studentsFiltered,
	whoDownloaded = whoDownloaded,
	stillToDownload = stillToDownload,
	hasPublishedFeedback = hasPublishedFeedback,
	hasOriginalityReport = hasOriginalityReport,
	mustReleaseForMarking = assignment.mustReleaseForMarking
)
}
}