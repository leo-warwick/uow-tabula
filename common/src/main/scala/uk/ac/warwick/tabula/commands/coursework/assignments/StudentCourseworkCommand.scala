package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.commands.coursework.assignments.StudentCourseworkCommand.StudentAssignments
import uk.ac.warwick.tabula.data.model.{Submission, Assignment}
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, CommandInternal}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AssessmentServiceComponent}
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

trait CourseworkCommandTypes {
	type AssignmentInfo = Map[String, Any]
}

object StudentCourseworkCommand extends CourseworkCommandTypes {
	case class StudentAssignments(enrolledAssignments: Seq[AssignmentInfo], historicAssignments: Seq[AssignmentInfo])
}

trait StudentCourseworkCommandInternal
	extends CommandInternal[StudentAssignments] {
	self: AssessmentServiceComponent with
		AssessmentMembershipServiceComponent with
		FeaturesComponent with
		StudentCourseworkCommandHelper =>

	def applyInternal() = StudentAssignments(
		enrolledAssignments = enrolledAssignmentsInfo,
		historicAssignments = getHistoricAssignmentsInfo(assignmentsWithFeedbackInfo, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo)
	)
}

trait StudentCourseworkCommandHelper
	extends CourseworkCommandTypes
	with TaskBenchmarking {

	self: AssessmentServiceComponent with
		AssessmentMembershipServiceComponent with
		FeaturesComponent =>

	val overridableAssignmentsWithFeedback: Seq[Assignment]
	val overridableEnrolledAssignments: Seq[Assignment]
	val overridableAssignmentsWithSubmission: Seq[Assignment]
	val user: User
	val usercode: String

	val assignmentsWithFeedback: Seq[Assignment] = benchmarkTask("Get assignments with feedback") { overridableAssignmentsWithFeedback }

	val enrolledAssignments: Seq[Assignment] = benchmarkTask("Get enrolled assignments") {
		if (features.assignmentMembership) overridableEnrolledAssignments
		else Nil
	}

	val assignmentsWithSubmission: Seq[Assignment] = benchmarkTask("Get assignments with submission") {
		if (features.submissions) overridableAssignmentsWithSubmission
		else Nil
	}

	val lateFormativeAssignments: Seq[Assignment] = {
		enrolledAssignments.filter {
			ass => !ass.summative && ass.isClosed
		}
	} // TAB-706

	// exclude assignments already included in other lists.
	def enrolledAssignmentsTrimmed(user: User, usercode: String): Seq[Assignment] =
		enrolledAssignments
			.diff(assignmentsWithFeedback)
			.diff(assignmentsWithSubmission)
			.filter {_.collectSubmissions} // TAB-475
			.filterNot(lateFormativeAssignments.contains(_))
			.sortWith { (ass1, ass2) =>
				// TAB-569 personal time to deadline - if ass1 is "due" before ass2 for the current user
				// Show open ended assignments after
				if (ass2.openEnded && !ass1.openEnded) true
				else if (ass1.openEnded && !ass2.openEnded) false
				else {
					def timeToDeadline(ass: Assignment) = {
						val extension = ass.extensions.asScala.find(e => e.usercode == usercode || e.usercode == user.getUserId)
						val isExtended = ass.isWithinExtension(user)

						if (ass.openEnded) ass.openDate
						else if (isExtended) extension.flatMap(_.expiryDate).getOrElse(ass.closeDate)
						else ass.closeDate
					}

					timeToDeadline(ass1) < timeToDeadline(ass2)
				}
			}

	private def enhanced(assignment: Assignment, user: User, usercode: String) = {
		val extension = assignment.extensions.asScala.find(e => e.isForUser(user))
		// isExtended: is within an approved extension
		val isExtended = assignment.isWithinExtension(user)
		// hasActiveExtension: active = approved
		val hasActiveExtension = extension.exists(_.approved)
		val extensionRequested = extension.isDefined && !extension.get.isManual
		val submission = assignment.submissions.asScala.find(_.usercode == usercode)
		val feedback = assignment.feedbacks.asScala.filter(_.released).find(_.usercode == usercode)
		Map(
			"assignment" -> assignment,
			"submission" -> submission,
			"hasSubmission" -> submission.isDefined,
			"feedback" -> feedback,
			"hasFeedback" -> feedback.isDefined,
			"hasExtension" -> extension.isDefined,
			"hasActiveExtension" -> hasActiveExtension,
			"extension" -> extension,
			"isExtended" -> isExtended,
			"extensionRequested" -> extensionRequested,
			"studentDeadline" -> assignment.submissionDeadline(user),
			"submittable" -> assignment.submittable(user),
			"resubmittable" -> assignment.resubmittable(user),
			"closed" -> assignment.isClosed,
			"summative" -> assignment.summative.booleanValue
		)

	}

	// adorn the enrolled assignments with extra data.
	val enrolledAssignmentsInfo: Seq[Map[String, Any]] = for (assignment <- enrolledAssignmentsTrimmed(user, usercode)) yield enhanced(assignment, user, usercode)
	val assignmentsWithFeedbackInfo: Seq[Map[String, Any]] = for (assignment <- assignmentsWithFeedback) yield enhanced(assignment, user, usercode)
	val assignmentsWithSubmissionInfo: Seq[Map[String, Any]] = for (assignment <- assignmentsWithSubmission.diff(assignmentsWithFeedback)) yield enhanced(assignment, user, usercode)
	val lateFormativeAssignmentsInfo: Seq[Map[String, Any]] = for (assignment <- lateFormativeAssignments) yield enhanced(assignment, user, usercode)

	def getHistoricAssignmentsInfo(
																	assignmentsWithFeedbackInfo: Seq[AssignmentInfo],
																	assignmentsWithSubmissionInfo: Seq[AssignmentInfo],
																	lateFormativeAssignmentsInfo: Seq[AssignmentInfo]): Seq[AssignmentInfo] = {
		assignmentsWithFeedbackInfo
			.union(assignmentsWithSubmissionInfo)
			.union(lateFormativeAssignmentsInfo)
			.sortWith {	(info1, info2) =>
			def toDate(info: AssignmentInfo) = {
				val assignment = info("assignment").asInstanceOf[Assignment]
				val submission = info("submission").asInstanceOf[Option[Submission]]

				submission map { _.submittedDate } getOrElse { if (assignment.openEnded) assignment.openDate else assignment.closeDate }
			}

			toDate(info1) < toDate(info2)
		}.distinct.reverse
	}
}
