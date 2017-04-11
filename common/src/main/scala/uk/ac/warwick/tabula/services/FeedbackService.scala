package uk.ac.warwick.tabula.services
import scala.collection.JavaConversions._

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire

trait FeedbackService {
	def getStudentFeedback(assessment: Assessment, usercode: String): Option[Feedback]
	def countPublishedFeedback(assignment: Assignment): Int
	def getUsersForFeedback(assignment: Assignment): Seq[(String, User)]
	def getAssignmentFeedbackByUsercode(assignment: Assignment, usercode: String): Option[AssignmentFeedback]
	def getAssignmentFeedbackById(feedbackId: String): Option[AssignmentFeedback]
	def getMarkerFeedbackById(markerFeedbackId: String): Option[MarkerFeedback]
	def getRejectedMarkerFeedbackByFeedback(feedback: Feedback): Seq[MarkerFeedback]
	def saveOrUpdate(feedback: Feedback)
	def saveOrUpdate(mark: Mark)
	def delete(feedback: Feedback)
	def save(feedback: MarkerFeedback)
	def delete(feedback: MarkerFeedback)
	def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback]

}

@Service(value = "feedbackService")
class FeedbackServiceImpl extends FeedbackService with Daoisms with Logging {

	@Autowired var userLookup: UserLookupService = _
	@Autowired var dao: FeedbackDao = _

	/* get users whose feedback is not published and who have not submitted work suspected
	 * of being plagiarised */
	def getUsersForFeedback(assignment: Assignment): Seq[(String, User)] = {
		val plagiarisedSubmissions = assignment.submissions.filter { submission => submission.suspectPlagiarised }
		val plagiarisedIds = plagiarisedSubmissions.map { _.usercode }
		val unreleasedIds = assignment.unreleasedFeedback.map { _.usercode }
		val unplagiarisedUnreleasedIds = unreleasedIds.filter { usercode => !plagiarisedIds.contains(usercode) }
		userLookup.getUsersByUserIds(unplagiarisedUnreleasedIds).toSeq
	}

	def getStudentFeedback(assessment: Assessment, usercode: String): Option[Feedback] = {
		assessment.findFullFeedback(usercode)
	}

	def countPublishedFeedback(assignment: Assignment): Int = {
		session.createSQLQuery("""select count(*) from feedback where assignment_id = :assignmentId and released = 1""")
			.setString("assignmentId", assignment.id)
			.uniqueResult
			.asInstanceOf[Number].intValue
	}

	def getAssignmentFeedbackByUsercode(assignment: Assignment, usercode: String): Option[AssignmentFeedback] = transactional(readOnly = true) {
		dao.getAssignmentFeedbackByUsercode(assignment, usercode)
	}

	def getAssignmentFeedbackById(feedbackId: String): Option[AssignmentFeedback] = {
		dao.getAssignmentFeedback(feedbackId)
	}

	def getMarkerFeedbackById(markerFeedbackId: String): Option[MarkerFeedback] = {
		dao.getMarkerFeedback(markerFeedbackId)
	}

	def getRejectedMarkerFeedbackByFeedback(feedback: Feedback): Seq[MarkerFeedback] = {
		dao.getRejectedMarkerFeedbackByFeedback(feedback)
	}

	def delete(feedback: Feedback): Unit = transactional() {
		dao.delete(feedback)
	}

	def saveOrUpdate(feedback:Feedback){
		session.saveOrUpdate(feedback)
	}

	def saveOrUpdate(mark: Mark) {
		session.saveOrUpdate(mark)
	}

	def save(feedback: MarkerFeedback): Unit = transactional() {
		dao.save(feedback)
	}

	def delete(markerFeedback: MarkerFeedback): Unit = transactional() {

		// remove link to parent
		val parentFeedback = markerFeedback.feedback
		if (markerFeedback == parentFeedback.firstMarkerFeedback) parentFeedback.firstMarkerFeedback = null
		else if (markerFeedback == parentFeedback.secondMarkerFeedback) parentFeedback.secondMarkerFeedback = null
		else if (markerFeedback == parentFeedback.thirdMarkerFeedback) parentFeedback.thirdMarkerFeedback = null
		saveOrUpdate(parentFeedback)

		dao.delete(markerFeedback)
	}

	def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback] =
		dao.getExamFeedbackMap(exam, users)

}

trait FeedbackServiceComponent {
	def feedbackService: FeedbackService
}

trait AutowiringFeedbackServiceComponent extends FeedbackServiceComponent {
	var feedbackService: FeedbackService = Wire[FeedbackService]
}
