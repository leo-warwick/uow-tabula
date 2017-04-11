package uk.ac.warwick.tabula.commands.coursework.assignments

import javax.validation.constraints.NotNull

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.assignments.ListSubmissionsCommand._
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.commands.coursework.feedback.ListFeedbackCommand
import uk.ac.warwick.tabula.commands.coursework.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.helpers.coursework.{CourseworkFilter, CourseworkFilters}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.coursework.{AutowiringCourseworkWorkflowServiceComponent, CourseworkWorkflowServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{WorkflowStage, WorkflowStages}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

object SubmissionAndFeedbackCommand {
	type CommandType = Appliable[SubmissionAndFeedbackResults]

	def apply(module: Module, assignment: Assignment) =
		new SubmissionAndFeedbackCommandInternal(module, assignment)
			with ComposableCommand[SubmissionAndFeedbackResults]
			with SubmissionAndFeedbackRequest
			with SubmissionAndFeedbackPermissions
			with SubmissionAndFeedbackValidation
			with CommandSubmissionAndFeedbackEnhancer
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringCourseworkWorkflowServiceComponent
			with Unaudited with ReadOnly

	case class SubmissionAndFeedbackResults (
		students:Seq[Student],
		whoDownloaded: Seq[(User, DateTime)],
		stillToDownload: Seq[Student],
		hasPublishedFeedback: Boolean,
		hasOriginalityReport: Boolean,
		mustReleaseForMarking: Boolean
	)

	// Simple object holder
	case class Student (
		user: User,
		progress: Progress,
		nextStage: Option[WorkflowStage],
		stages: ListMap[String, WorkflowStages.StageProgress],
		coursework: WorkflowItems,
		assignment:Assignment,
		disability: Option[Disability]
	)

	case class WorkflowItems (
		student: User,
		enhancedSubmission: Option[SubmissionListItem],
		enhancedFeedback: Option[FeedbackListItem],
		enhancedExtension: Option[ExtensionListItem]
	)

	case class Progress (
		percentage: Int,
		t: String,
		messageCode: String
	)

	case class ExtensionListItem (
		extension: Extension,
		within: Boolean
	)
}

trait SubmissionAndFeedbackState {
	def module: Module
	def assignment: Assignment
}

trait SubmissionAndFeedbackRequest extends SubmissionAndFeedbackState {
	@NotNull var filter: CourseworkFilter = CourseworkFilters.AllStudents
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

abstract class SubmissionAndFeedbackCommandInternal(val module: Module, val assignment: Assignment)
	extends CommandInternal[SubmissionAndFeedbackResults] with SubmissionAndFeedbackState with TaskBenchmarking {
	self: SubmissionAndFeedbackRequest
		with AssessmentMembershipServiceComponent
		with UserLookupComponent
		with FeedbackForSitsServiceComponent
		with ProfileServiceComponent
		with SubmissionAndFeedbackEnhancer
		with CourseworkWorkflowServiceComponent =>

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

		val unsubmitted = benchmarkTask("Get unsubmitted users") {
			for (user <- unsubmittedMembers) yield {
				val usersExtension = assignment.extensions.asScala.filter(_.usercode == user.getUserId)
				if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + user.getUserId)

				val enhancedExtensionForUniId = usersExtension.headOption map { extension =>
					new ExtensionListItem(
						extension=extension,
						within=assignment.isWithinExtension(user)
					)
				}

				val coursework = WorkflowItems(
					user,
					enhancedSubmission=None,
					enhancedFeedback=enhancedFeedbackForUsercode(user.getUserId),
					enhancedExtension=enhancedExtensionForUniId
				)

				val progress = courseworkWorkflowService.progress(assignment)(coursework)

				Student(
					user=user,
					progress=Progress(progress.percentage, progress.cssClass, progress.messageCode),
					nextStage=progress.nextStage,
					stages=progress.stages,
					coursework=coursework,
					assignment=assignment,
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

			val coursework = WorkflowItems(
				user,
				enhancedSubmission=enhancedSubmissionForUniId,
				enhancedFeedback=enhancedFeedbackForUsercode(usercode),
				enhancedExtension=enhancedExtensionForUniId
			)

			val progress = courseworkWorkflowService.progress(assignment)(coursework)

			Student(
				user=user,
				progress=Progress(progress.percentage, progress.cssClass, progress.messageCode),
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