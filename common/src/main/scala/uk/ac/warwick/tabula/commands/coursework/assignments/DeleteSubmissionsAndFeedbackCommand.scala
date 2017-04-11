package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.data.HibernateHelpers

import scala.collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.FeedbackService
import uk.ac.warwick.tabula.services.SubmissionService

/**
 * Takes a list of student university IDs and deletes either all their submissions, or all their feedback, or both,
 * depending on the value of submissionOrFeedback
 */
class DeleteSubmissionsAndFeedbackCommand(val module: Module, val assignment: Assignment)
	extends Command[(Seq[Submission], Seq[Feedback])] with SelfValidating {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.AssignmentFeedback.Manage, assignment)
	PermissionCheck(Permissions.Submission.Delete, assignment)

	var submissionService: SubmissionService = Wire.auto[SubmissionService]
	var feedbackService: FeedbackService = Wire.auto[FeedbackService]

	var zipService: ZipService = Wire.auto[ZipService]
	var userLookup: UserLookupService = Wire.auto[UserLookupService]

	var students: JList[String] = JArrayList()
	var submissionOrFeedback: String = ""
	var confirm: Boolean = false

	val SubmissionOnly = "submissionOnly"
	val FeedbackOnly = "feedbackOnly"
	val SubmissionAndFeedback = "submissionAndFeedback"

	def shouldDeleteSubmissions: Boolean = submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == SubmissionOnly
	def shouldDeleteFeedback: Boolean = submissionOrFeedback == SubmissionAndFeedback || submissionOrFeedback == FeedbackOnly

	def applyInternal(): (Seq[Submission], Seq[AssignmentFeedback]) = {
		val submissions = if (shouldDeleteSubmissions) {
			val submissions = for (usercode <- students; submission <- submissionService.getSubmissionByUsercode(assignment, usercode)) yield {
				HibernateHelpers.initialiseAndUnproxy(submission.allAttachments)
				submissionService.delete(mandatory(submission))
				submission
			}
			zipService.invalidateSubmissionZip(assignment)
			submissions
		} else Nil

		val feedbacks = if (shouldDeleteFeedback) {
			val feedbacks = for (usercode <- students; feedback <- feedbackService.getAssignmentFeedbackByUsercode(assignment, usercode)) yield {
				HibernateHelpers.initialiseAndUnproxy(feedback.attachments)
				feedbackService.delete(mandatory(feedback))
				zipService.invalidateIndividualFeedbackZip(feedback)
				feedback
			}
			feedbacks
		} else Nil

		(submissions, feedbacks)
	}

	def prevalidate(errors: Errors) {
		for (usercode <- students; submission <- submissionService.getSubmissionByUsercode(assignment, usercode)) {
			if (mandatory(submission).assignment != assignment) errors.reject("submission.bulk.wrongassignment")
		}

		for (usercode <- students; feedback <- feedbackService.getAssignmentFeedbackByUsercode(assignment, usercode)) {
			if (mandatory(feedback).assignment != assignment) errors.reject("feedback.bulk.wrongassignment")
		}

		if (!Seq(SubmissionOnly, FeedbackOnly, SubmissionAndFeedback).contains(submissionOrFeedback)) {
			errors.rejectValue("submissionOrFeedback", "invalid")
		}
	}

	def validate(errors: Errors) {
		prevalidate(errors)
		if (!confirm) errors.rejectValue("confirm", "submissionOrFeedback.delete.confirm")
	}


	def getStudentsAsUsers: JList[User] = userLookup.getUsersByUserIds(students).values.toSeq

	override def describe(d: Description): Unit = d
		.assignment(assignment)
		.property("students" -> students)

	override def describeResult(d: Description, result: (Seq[Submission], Seq[Feedback])): Unit = {
		val (submissions, feedbacks) = result
		val attachments = submissions.flatMap { _.allAttachments } ++ feedbacks.flatMap { _.attachments }

		d.assignment(assignment)
			.property("submissionsDeleted" -> submissions.length)
			.property("feedbacksDeleted" -> feedbacks.length)
			.fileAttachments(attachments)
	}
}
