package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Mark, Feedback, FeedbackForSitsStatus, FeedbackForSits}
import uk.ac.warwick.spring.Wire

trait FeedbackForSitsDaoComponent {
	def feedbackForSitsDao: FeedbackForSitsDao
}

trait AutowiringFeedbackForSitsDaoComponent extends FeedbackForSitsDaoComponent {
	var feedbackForSitsDao: FeedbackForSitsDao = Wire[FeedbackForSitsDao]
}

trait FeedbackForSitsDao {
	def saveOrUpdate(feedbackForSits: FeedbackForSits)
	def saveOrUpdate(feedback: Feedback)
	def saveOrUpdate(mark: Mark)
	def feedbackToLoad: Seq[FeedbackForSits]
	def getByFeedback(feedback: Feedback): Option[FeedbackForSits]
	def getByFeedbacks(feedbacks: Seq[Feedback]): Map[Feedback, FeedbackForSits]
}

@Repository
class FeedbackForSitsDaoImpl extends FeedbackForSitsDao with Daoisms {

	def saveOrUpdate(feedbackForSits: FeedbackForSits): Unit = session.saveOrUpdate(feedbackForSits)
	def saveOrUpdate(feedback: Feedback): Unit = session.saveOrUpdate(feedback)
	def saveOrUpdate(mark: Mark): Unit = session.saveOrUpdate(mark)

	def feedbackToLoad: Seq[FeedbackForSits] =
		session.newCriteria[FeedbackForSits]
			.add(is("status", FeedbackForSitsStatus.UploadNotAttempted))
			.seq

	def getByFeedback(feedback: Feedback): Option[FeedbackForSits] = {
		session.newCriteria[FeedbackForSits]
			.add(is("feedback", feedback))
			.uniqueResult
	}

	def getByFeedbacks(feedbacks: Seq[Feedback]): Map[Feedback, FeedbackForSits] = {
		safeInSeq(
			() => session.newCriteria[FeedbackForSits],
			"feedback",
			feedbacks
		).groupBy(_.feedback).mapValues(_.head)
	}
}
