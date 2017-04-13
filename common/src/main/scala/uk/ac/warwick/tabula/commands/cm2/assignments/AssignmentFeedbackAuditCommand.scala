package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.helpers.cm2._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.{AutowiringCM2WorkflowServiceProgressComponent, CM2WorkflowServiceProgressComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._


object AssignmentFeedbackAuditCommand {
	def apply(assignment: Assignment) =
		new AssignmentFeedbackAuditCommandInternal(assignment)
			with ComposableCommand[AssignmentFeedbackAuditResults]
			with AssignmentFeedbackAuditPermissions
			with AutowiringAssessmentMembershipServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringFeedbackForSitsServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringCM2WorkflowServiceProgressComponent
			with Unaudited with ReadOnly
}

case class MarkerInfo(
	firstMarkersWithStudentAllocationCountMap: Map[User, Int],
	secondMarkersWithStudentAllocationCountMap: Map[User, Int]
)

case class ExtensionInfo(
	approvedExtensionCount: Int,
	rejectedExtensionCount: Int
)

case class AssignmentFeedbackAuditResults(
	students: Seq[WorkFlowStudent],
	totalFilesCheckedForPlagiarism: Int,
	extensionInfo: ExtensionInfo,
	markerInfo: MarkerInfo
)

trait AssignmentFeedbackAuditState {
	def assignment: Assignment
}

trait AssignmentFeedbackAuditPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AssignmentFeedbackAuditState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Submission.Read, assignment)
	}
}

class AssignmentFeedbackAuditCommandInternal(val assignment: Assignment) extends CommandInternal[AssignmentFeedbackAuditResults]
	with AssignmentFeedbackAuditState with TaskBenchmarking {

	self: AssessmentMembershipServiceComponent
		with UserLookupComponent
		with FeedbackForSitsServiceComponent
		with ProfileServiceComponent
		with CM2WorkflowServiceProgressComponent =>

	override def applyInternal(): AssignmentFeedbackAuditResults = {
		//most of the logic copied from cm1
		val submissions = assignment.submissions
		val extensionCountsByStatus = benchmarkTask("Get extension counts") {
			assignment.extensionCountByStatus
		}

		val usercodesWithSubmissionOrFeedback = benchmarkTask("Get uni IDs with submissions or feedback") {
			assignment.getUsercodesWithSubmissionOrFeedback.toSeq.sorted
		}
		val moduleMembers = benchmarkTask("Get module membership") {
			assessmentMembershipService.determineMembershipUsers(assignment)
		}
		val unsubmittedMembers = moduleMembers.filterNot(m => usercodesWithSubmissionOrFeedback.contains(m.getUserId))

		def enhancedFeedbackForUsercode(usercode: String) = {
			val usersFeedback = assignment.feedbacks.asScala.filter(feedback => feedback.usercode == usercode)
			if (usersFeedback.size > 1) throw new IllegalStateException("More than one Feedback for " + usercode)
			// we only need feedback for audit. Utilising existing workflow structure to extract FeedbackListItem as that might be needed at other places.
			usersFeedback.headOption.map { feedback => FeedbackListItem(feedback, downloaded = false, onlineViewed = false, feedbackForSits = null) }
		}


		def extensionCount(extensionStatusCount: Map[ExtensionState, Int], extensionState: ExtensionState): Int = {
			extensionStatusCount.find { case (state, _) => state == extensionState } match {
				case Some((state, count)) =>
					count
				case _ =>
					0

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
					enhancedFeedbackForUsercode(user.getUserId),
					enhancedExtensionForUniId
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

		val submitted: Seq[WorkFlowStudent] = benchmarkTask("Get submitted users") {
			for (usercode <- usercodesWithSubmissionOrFeedback) yield {
				val usersSubmissions = submissions.asScala.filter(_.usercode == usercode)
				val usersExtension = assignment.extensions.asScala.filter(_.usercode == usercode)

				val userFilter = moduleMembers.filter(member => member.getUserId == usercode)
				val user = if (userFilter.isEmpty) {
					userLookup.getUserByUserId(usercode)
				} else {
					userFilter.head
				}

				if (usersSubmissions.size > 1) throw new IllegalStateException(s"More than one Submission for $usercode")
				if (usersExtension.size > 1) throw new IllegalStateException(s"More than one Extension for $usercode")

				// we only need submission but utilising existing workflow structure to extract SubmissionListItem as that might be needed at other places
				val enhancedSubmissionForUsercode = usersSubmissions.headOption.map (submission => SubmissionListItem(submission, downloaded=false))

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

		val totalFilesCheckedForPlagiarism = benchmarkTask("Check for originality reports") {
			assignment.submissions.asScala.map { sub => sub.allAttachments.count(_.originalityReportReceived) }.sum
		}
		val extensionInfo = ExtensionInfo(extensionCount(extensionCountsByStatus, ExtensionState.Approved), extensionCount(extensionCountsByStatus, ExtensionState.Rejected))
		val markerInfo = MarkerInfo(assignment.firstMarkersWithStudentAllocationCountMap, assignment.secondMarkersWithStudentAllocationCountMap)

		AssignmentFeedbackAuditResults(
			unsubmitted ++ submitted,
			totalFilesCheckedForPlagiarism,
			extensionInfo,
			markerInfo
		)
	}

}
