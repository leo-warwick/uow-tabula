package uk.ac.warwick.tabula.services
import scala.collection.JavaConversions._

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.{FeedbackDaoComponent, AutowiringFeedbackDaoComponent, Daoisms, FeedbackDao}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire

trait FeedbackService {
	def getStudentFeedback(assessment: Assessment, warwickId: String): Option[Feedback]
	def countPublishedFeedback(assignment: Assignment): Int
	def getUsersForFeedback(assignment: Assignment): Seq[(String, User)]
	def getAssignmentFeedbackByUniId(assignment: Assignment, uniId: String): Option[AssignmentFeedback]
	def getAssignmentFeedbackById(feedbackId: String): Option[AssignmentFeedback]
	def getMarkerFeedbackById(markerFeedbackId: String): Option[MarkerFeedback]
	def saveOrUpdate(feedback: Feedback)
	def saveOrUpdate(mark: Mark)
	def delete(feedback: Feedback)
	def save(feedback: MarkerFeedback)
	def delete(feedback: MarkerFeedback)
	def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback]
}

abstract class AbstractFeedbackService extends FeedbackService with Logging {
	self: UserLookupComponent with FeedbackDaoComponent =>

	/* get users whose feedback is not published and who have not submitted work suspected
	 * of being plagiarised */
	def getUsersForFeedback(assignment: Assignment): Seq[(String, User)] = {
		val plagiarisedSubmissions = assignment.submissions.filter { submission => submission.suspectPlagiarised }
		val plagiarisedIds = plagiarisedSubmissions.map { _.universityId }
		val unreleasedIds = assignment.unreleasedFeedback.map { _.universityId }
		val unplagiarisedUnreleasedIds = unreleasedIds.filter { uniId => !plagiarisedIds.contains(uniId) }
		userLookup.getUsersByWarwickUniIds(unplagiarisedUnreleasedIds).toSeq
	}

	def getStudentFeedback(assessment: Assessment, uniId: String) = {
		assessment.findFullFeedback(uniId)
	}

	def countPublishedFeedback(assignment: Assignment): Int = feedbackDao.countPublishedFeedback(assignment)

	def getAssignmentFeedbackByUniId(assignment: Assignment, uniId: String) = transactional(readOnly = true) {
		feedbackDao.getAssignmentFeedbackByUniId(assignment, uniId)
	}

	def getAssignmentFeedbackById(feedbackId: String): Option[AssignmentFeedback] = {
		feedbackDao.getAssignmentFeedback(feedbackId)
	}

	def getMarkerFeedbackById(markerFeedbackId: String): Option[MarkerFeedback] = {
		feedbackDao.getMarkerFeedback(markerFeedbackId)
	}

	def delete(feedback: Feedback) = transactional() {
		feedbackDao.delete(feedback)
	}

	def saveOrUpdate(feedback:Feedback) = transactional() { feedbackDao.save(feedback) }
	def saveOrUpdate(mark: Mark) = transactional() { feedbackDao.save(mark) }

	def save(feedback: MarkerFeedback) = transactional() {
		feedbackDao.save(feedback)
	}

	def delete(markerFeedback: MarkerFeedback) = transactional() {

		// remove link to parent
		val parentFeedback = markerFeedback.feedback
		if (markerFeedback == parentFeedback.firstMarkerFeedback) parentFeedback.firstMarkerFeedback = null
		else if (markerFeedback == parentFeedback.secondMarkerFeedback) parentFeedback.secondMarkerFeedback = null
		else if (markerFeedback == parentFeedback.thirdMarkerFeedback) parentFeedback.thirdMarkerFeedback = null
		saveOrUpdate(parentFeedback)

		feedbackDao.delete(markerFeedback)
	}

	def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback] =
		feedbackDao.getExamFeedbackMap(exam, users)

}

@Service(value = "feedbackService")
class FeedbackServiceImpl extends AbstractFeedbackService
	with AutowiringFeedbackDaoComponent
	with AutowiringUserLookupComponent

trait FeedbackServiceComponent {
	def feedbackService: FeedbackService
}

trait AutowiringFeedbackServiceComponent extends FeedbackServiceComponent {
	var feedbackService = Wire[FeedbackService]
}
