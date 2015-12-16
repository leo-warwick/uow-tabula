package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Restrictions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import org.springframework.stereotype.Repository
import uk.ac.warwick.userlookup.User

trait FeedbackDao {
	def getAssignmentFeedback(id: String): Option[AssignmentFeedback]
	def getAssignmentFeedbackByUniId(assignment: Assignment, uniId: String): Option[AssignmentFeedback]
	def getMarkerFeedback(id: String): Option[MarkerFeedback]
	def save(feedback: Feedback)
	def delete(feedback: Feedback)
	def save(feedback: MarkerFeedback)
	def delete(feedback: MarkerFeedback)
	def save(mark: Mark)
	def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback]
	def countPublishedFeedback(assignment: Assignment): Int
	def countFullFeedback(assignment: Assignment): Int
}

abstract class AbstractFeedbackDao extends FeedbackDao with Daoisms {
	self: SessionComponent =>

	override def getAssignmentFeedback(id: String) = session.getById[AssignmentFeedback](id)
	override def getMarkerFeedback(id: String) = session.getById[MarkerFeedback](id)

	override def getAssignmentFeedbackByUniId(assignment: Assignment, uniId: String): Option[AssignmentFeedback] =
		session.newCriteria[AssignmentFeedback]
			.add(is("universityId", uniId))
			.add(is("assignment", assignment))
			.uniqueResult

	override def save(feedback: Feedback) = {
		session.saveOrUpdate(feedback)
	}

	override def delete(feedback: Feedback) = {
		// We need to delete any markerfeedback first
		Option(feedback.firstMarkerFeedback) foreach { _.markDeleted() }
		Option(feedback.secondMarkerFeedback) foreach { _.markDeleted() }
		Option(feedback.thirdMarkerFeedback) foreach { _.markDeleted() }

		feedback.clearAttachments()

		session.delete(feedback)
	}

	override def save(feedback: MarkerFeedback) = {
		session.saveOrUpdate(feedback)
	}

	override def delete(feedback: MarkerFeedback) = {
		session.delete(feedback)
	}

	override def save(mark: Mark) = {
		session.saveOrUpdate(mark)
	}

	override def getExamFeedbackMap(exam: Exam, users: Seq[User]): Map[User, ExamFeedback] = {
		safeInSeq(
			() => {
				session.newCriteria[ExamFeedback]
					.add(is("exam", exam))
			},
			"universityId",
			users.map(_.getWarwickId)
		).groupBy(_.universityId).map{case(universityId, feedbacks) =>
			users.find(_.getWarwickId == universityId).get -> feedbacks.head
		}
	}

	override def countPublishedFeedback(assignment: Assignment): Int =
		session.newCriteria[AssignmentFeedback]
			.add(is("assignment", assignment))
			.add(is("released", true))
			.count.intValue()

	override def countFullFeedback(assignment: Assignment): Int =
		session.newCriteria[AssignmentFeedback]
			.add(is("assignment", assignment))
			.add(not(
				conjunction()
					.add(isNull("actualMark"))
					.add(isNull("actualGrade"))
					.add(isEmpty("attachments"))
			))
			.count.intValue()
}

trait FeedbackDaoComponent {
	def feedbackDao: FeedbackDao
}

trait AutowiringFeedbackDaoComponent extends FeedbackDaoComponent {
	override val feedbackDao = Wire[FeedbackDao]
}

@Repository
class FeedbackDaoImpl extends AbstractFeedbackDao with Daoisms