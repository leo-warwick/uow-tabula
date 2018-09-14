package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.SubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand
import uk.ac.warwick.tabula.commands.cm2.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.{Futures, Logging}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.cm2.SubmissionAndFeedbackInfoFilters.OverlapPlagiarismFilter
import uk.ac.warwick.tabula.helpers.cm2._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.{AutowiringCM2WorkflowProgressServiceComponent, CM2WorkflowProgressServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.concurrent.{Await, Future, TimeoutException}
import scala.concurrent.duration._

object SubmissionAndFeedbackCommand {
	type CommandType = Appliable[SubmissionAndFeedbackResults]

	def apply(assignment: Assignment) =
		new SubmissionAndFeedbackCommandInternal(assignment)
			with ComposableCommand[SubmissionAndFeedbackResults]
			with SubmissionAndFeedbackRequest
			with SubmissionAndFeedbackPermissions
			with CommandSubmissionAndFeedbackEnhancer
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringCM2WorkflowProgressServiceComponent
			with Unaudited with ReadOnly

	case class SubmissionAndFeedbackResults(
		students: Seq[AssignmentSubmissionStudentInfo],
		whoDownloaded: Seq[(User, DateTime)],
		stillToDownload: Seq[AssignmentSubmissionStudentInfo],
		hasPublishedFeedback: Boolean,
		hasOriginalityReport: Boolean,
		workflowMarkers: Seq[String]
	)

}

trait SubmissionAndFeedbackState extends SelectedStudentsState {
	def assignment: Assignment
}

trait SubmissionAndFeedbackRequest extends SubmissionAndFeedbackState with SelectedStudentsRequest {
	var submissionStatesFilters: JList[SubmissionAndFeedbackInfoFilter] = JArrayList()
	var plagiarismFilters: JList[SubmissionAndFeedbackInfoFilter] = JArrayList()
	var statusesFilters: JList[SubmissionAndFeedbackInfoFilter] = JArrayList()
	var extensionFilters: JList[SubmissionAndFeedbackInfoFilter] = JArrayList()
	var overlapFilter: OverlapPlagiarismFilter = new OverlapPlagiarismFilter
}


trait SubmissionAndFeedbackEnhancer {
	def enhanceSubmissions(): Seq[SubmissionListItem]

	def enhanceFeedback(): ListFeedbackResult
}

trait CommandSubmissionAndFeedbackEnhancer extends SubmissionAndFeedbackEnhancer {
	self: SubmissionAndFeedbackState with Logging =>

	val enhancedSubmissionsCommand = ListSubmissionsCommand(assignment)
	val enhancedFeedbacksCommand = ListFeedbackCommand(assignment)

	val timeout = 5.seconds

	override def enhanceSubmissions(): Seq[SubmissionListItem] = try {
		Await.result(enhancedSubmissionsCommand.apply(), 5.seconds)
	} catch {
		case _: TimeoutException => {
			logger.info(s"enhancedSubmissionsCommand is taking more than $timeout to process ${assignment.id}, skipping.")
			Seq.empty
		}
	}

	override def enhanceFeedback(): ListFeedbackResult = try {
		Await.result(enhancedFeedbacksCommand.apply(), 5.seconds)
	} catch {
		case _: TimeoutException => {
			logger.info(s"enhancedFeedbacksCommand is taking more than $timeout to process ${assignment.id}, skipping.")
			ListFeedbackResult(
				Seq.empty,
				Map.empty,
				Map.empty,
				None
			)
		}
	}
}


trait SubmissionAndFeedbackPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SubmissionAndFeedbackState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		mustBeLinked(notDeleted(mandatory(assignment)), mandatory(assignment.module))
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
		with CM2WorkflowProgressServiceComponent =>

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
			assignment.getUsercodesWithSubmissionOrFeedback.filter(_.hasText).toSeq.sorted
		}
		val moduleMembers = benchmarkTask("Get module membership") {
			assessmentMembershipService.determineMembershipUsers(assignment)
		}
		val unsubmittedMembers = moduleMembers.filterNot(m => usercodesWithSubmissionOrFeedback.contains(m.getUserId))

		val allFeedback = assignment.feedbacks.asScala
		val allExtensions = assignment.extensions.asScala

		def enhancedFeedbackForUsercode(usercode: String) = {
			val usersFeedback = allFeedback.filter(feedback => feedback.usercode == usercode)
			if (usersFeedback.size > 1) throw new IllegalStateException("More than one Feedback for " + usercode)
			usersFeedback.headOption.map { feedback =>
				val downloaded = feedback.hasContent && !feedback.attachments.isEmpty && (whoDownloaded exists { case (user, dateTime) =>
					user.getUserId == feedback.usercode &&
						dateTime.isAfter(feedback.mostRecentAttachmentUpload)
				})

				val viewed = feedback.hasContent && (feedback.hasOnlineFeedback || feedback.hasGenericFeedback) && whoViewed.filterKeys(_.getUserId == feedback.usercode).exists { case (user, dateTime) =>
					val usercode = user.getUserId

					val latestOnlineUpdate = latestModifiedOnlineFeedback
						.find { case (u, _) => u.getUserId == usercode }
						.map { case (_, dt) => dt }
						.getOrElse(new DateTime(0))

					val latestUpdate = latestGenericFeedbackUpdate
						.filter(_.isAfter(latestOnlineUpdate))
						.getOrElse(latestOnlineUpdate)

					dateTime.isAfter(latestUpdate)
				}

				FeedbackListItem(feedback, downloaded, viewed, if (feedback.hasContent) feedbackForSitsService.getByFeedback(feedback) else None)
			}
		}

		val unsubmitted: Seq[AssignmentSubmissionStudentInfo] = benchmarkTask("Get unsubmitted users") {
			for (user <- unsubmittedMembers) yield benchmarkTask(s"Inflate information for ${user.getFullName} (${user.getUserId})") {
				val usersExtension = benchmarkTask("Find extension") { allExtensions.filter(_.usercode == user.getUserId) }
				if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + user.getUserId)

				val enhancedExtensionForUniId = benchmarkTask("Enhance extension") { usersExtension.headOption map { extension =>
					ExtensionListItem(
						extension = extension,
						within = assignment.isWithinExtension(user)
					)
				}}

				val coursework = benchmarkTask("Create workflow items") { WorkflowItems(
					user,
					enhancedSubmission = None,
					enhancedFeedback = benchmarkTask("Enhance feedback") { enhancedFeedbackForUsercode(user.getUserId) },
					enhancedExtension = enhancedExtensionForUniId
				) }

				val progress = benchmarkTask("Get progress") {
					workflowProgressService.progress(assignment)(coursework)
				}

				AssignmentSubmissionStudentInfo(
					user = user,
					progress = Progress(progress.percentage, progress.cssClass, progress.messageCode),
					nextStage = progress.nextStage,
					stages = progress.stages,
					coursework = coursework,
					assignment = assignment,
					disability = None
				)
			}
		}

		val submitted: Seq[AssignmentSubmissionStudentInfo] = benchmarkTask("Get submitted users") {
			for (usercode <- usercodesWithSubmissionOrFeedback) yield {
				val usersSubmissions = enhancedSubmissions.filter(_.submission.usercode == usercode)
				val usersExtension = allExtensions.filter(extension => extension.usercode == usercode)

				val userFilter = moduleMembers.filter(u => u.getUserId == usercode)
				val user = if (userFilter.isEmpty) {
					userLookup.getUserByUserId(usercode)
				} else {
					userFilter.head
				}

				if (usersSubmissions.size > 1) throw new IllegalStateException("More than one Submission for " + usercode)
				if (usersExtension.size > 1) throw new IllegalStateException("More than one Extension for " + usercode)

				val enhancedSubmissionForUniId = usersSubmissions.headOption
				//for expired users store university ID -TAB-5652
				if (!user.isFoundUser && enhancedSubmissionForUniId.isDefined) {
					user.setWarwickId(enhancedSubmissionForUniId.head.submission._universityId)
				}

				val enhancedExtensionForUniId = usersExtension.headOption map { extension =>
					ExtensionListItem(
						extension = extension,
						within = assignment.isWithinExtension(user)
					)
				}

				val coursework = WorkflowItems(
					user,
					enhancedSubmission = enhancedSubmissionForUniId,
					enhancedFeedback = enhancedFeedbackForUsercode(usercode),
					enhancedExtension = enhancedExtensionForUniId
				)

				val progress = workflowProgressService.progress(assignment)(coursework)

				AssignmentSubmissionStudentInfo(
					user = user,
					progress = Progress(progress.percentage, progress.cssClass, progress.messageCode),
					nextStage = progress.nextStage,
					stages = progress.stages,
					coursework = coursework,
					assignment = assignment,
					disability = {
						if (enhancedSubmissionForUniId.exists(_.submission.useDisability)) {
							profileService.getMemberByUser(user).flatMap {
								case student: StudentMember => Option(student)
								case _ => None
							}.flatMap(s => s.disability)
						} else {
							None
						}
					}
				)
			}
		}

		val membersWithPublishedFeedback = submitted.filter { student =>
			student.coursework.enhancedFeedback exists {
				_.feedback.checkedReleased
			}
		}

		// True if any feedback exists that's been published. To decide whether to show whoDownloaded count.
		val hasPublishedFeedback = membersWithPublishedFeedback.nonEmpty

		val stillToDownload = membersWithPublishedFeedback.filterNot(_.coursework.enhancedFeedback.exists(_.downloaded))

		val studentsFiltered = benchmarkTask("Do filtering") {
			val filteredStudents: Seq[AssignmentSubmissionStudentInfo] = (unsubmitted ++ submitted).filter {
				assignmentSubmissionStudentInfo => {
					val itemExistsInPlagiarismFilters = plagiarismFilters.asScala.isEmpty || plagiarismFilters.asScala.exists {
						submissionFeedbackInfoFilter => {
							submissionFeedbackInfoFilter.predicate(assignmentSubmissionStudentInfo) && submissionFeedbackInfoFilter.predicateWithAdditionalFilters(assignmentSubmissionStudentInfo, Seq(overlapFilter))
						}
					}
					val itemExistsInSubmissionStatesFilters = submissionStatesFilters.asScala.isEmpty || submissionStatesFilters.asScala.exists(_.predicate(assignmentSubmissionStudentInfo))
					val itemExistsInStatusesFilters = statusesFilters.asScala.isEmpty || statusesFilters.asScala.exists(_.predicate(assignmentSubmissionStudentInfo))
					val itemExistsInExtensionFilters = extensionFilters.asScala.isEmpty || extensionFilters.asScala.exists(_.predicate(assignmentSubmissionStudentInfo))

					itemExistsInPlagiarismFilters && itemExistsInSubmissionStatesFilters && itemExistsInStatusesFilters && itemExistsInExtensionFilters
				}
			}
			val studentsFiltered = if (students.isEmpty) filteredStudents else filteredStudents.filter(studentInfo => students.contains(studentInfo.user.getUserId))
			studentsFiltered
		}

		val workflowMarkers = if (!assignment.cm2Assignment || assignment.cm2MarkingWorkflow == null) {
			Nil
		} else {
			assignment.cm2MarkingWorkflow.allocationOrder
		}
		SubmissionAndFeedbackResults(
			students = studentsFiltered,
			whoDownloaded = whoDownloaded,
			stillToDownload = stillToDownload,
			hasPublishedFeedback = hasPublishedFeedback,
			hasOriginalityReport = hasOriginalityReport,
			workflowMarkers = workflowMarkers
		)
	}
}