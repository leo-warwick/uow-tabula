package uk.ac.warwick.tabula.services


import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringFeedbackForSitsDaoComponent, FeedbackForSitsDaoComponent}

case class ValidateAndPopulateFeedbackResult(
	valid: Seq[Feedback],
	populated: Map[Feedback, String],
	invalid: Map[Feedback, String]
)

trait FeedbackForSitsService {
	def saveOrUpdate(feedbackForSits: FeedbackForSits)
	def delete(feedbackForSits: FeedbackForSits)
	def feedbackToLoad: Seq[FeedbackForSits]
	def getByFeedback(feedback: Feedback): Option[FeedbackForSits]
	def getByFeedbacks(feedbacks: Seq[Feedback]): Map[Feedback, FeedbackForSits]
	def queueFeedback(feedback: Feedback, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks): Option[FeedbackForSits]
	def validateAndPopulateFeedback(feedbacks: Seq[Feedback], gradeGenerator: GeneratesGradesFromMarks): ValidateAndPopulateFeedbackResult
}

trait FeedbackForSitsServiceComponent {
	def feedbackForSitsService: FeedbackForSitsService
}

trait AutowiringFeedbackForSitsServiceComponent extends FeedbackForSitsServiceComponent {
	var feedbackForSitsService = Wire[FeedbackForSitsService]
}

abstract class AbstractFeedbackForSitsService extends FeedbackForSitsService {

	self: FeedbackForSitsDaoComponent =>

	def saveOrUpdate(feedbackForSits: FeedbackForSits) =
		feedbackForSitsDao.saveOrUpdate(feedbackForSits)

	def delete(feedbackForSits: FeedbackForSits) =
		feedbackForSitsDao.delete(feedbackForSits)

	def feedbackToLoad: Seq[FeedbackForSits] =
		feedbackForSitsDao.feedbackToLoad

	def getByFeedback(feedback: Feedback): Option[FeedbackForSits] =
		feedbackForSitsDao.getByFeedback(feedback)

	def getByFeedbacks(feedbacks: Seq[Feedback]): Map[Feedback, FeedbackForSits] =
		feedbackForSitsDao.getByFeedbacks(feedbacks)

	def queueFeedback(feedback: Feedback, submitter: CurrentUser, gradeGenerator: GeneratesGradesFromMarks): Option[FeedbackForSits] = {
		val validatedFeedback = validateAndPopulateFeedback(Seq(feedback), gradeGenerator)
		if (validatedFeedback.valid.nonEmpty || feedback.module.adminDepartment.assignmentGradeValidation && validatedFeedback.populated.nonEmpty) {
			val feedbackForSits = getByFeedback(feedback).getOrElse {
				// create a new object for this feedback in the queue
				val newFeedbackForSits = new FeedbackForSits
				newFeedbackForSits.firstCreatedOn = DateTime.now
				newFeedbackForSits
			}
			feedbackForSits.init(feedback, submitter.realUser) // initialise or re-initialise
			saveOrUpdate(feedbackForSits)

			if (validatedFeedback.populated.nonEmpty) {
				if (feedback.latestPrivateAdjustment.isDefined) {
					feedback.latestPrivateAdjustment.foreach(m => {
						m.grade = Some(validatedFeedback.populated(feedback))
						feedbackForSitsDao.saveOrUpdate(m)
					})
				} else {
					feedback.actualGrade = Some(validatedFeedback.populated(feedback))
				}
			}
			feedbackForSitsDao.saveOrUpdate(feedback)

			Option(feedbackForSits)
		} else {
			None
		}
	}

	def validateAndPopulateFeedback(feedbacks: Seq[Feedback], gradeGenerator: GeneratesGradesFromMarks): ValidateAndPopulateFeedbackResult = {

		val studentsMarks = (for (f <- feedbacks; mark <- f.latestMark) yield {
			f.universityId -> mark
		}).toMap

		val validGrades = gradeGenerator.applyForMarks(studentsMarks)

		val parsedFeedbacks = feedbacks.groupBy(f => {
			f.latestGrade match {
				case Some(grade) if f.latestMark.isEmpty => "invalid" // a grade without a mark is invalid
				case Some(grade) =>
					if (validGrades(f.universityId).isEmpty || !validGrades(f.universityId).exists(_.grade == grade))
						"invalid"
					else
						"valid"
				case None =>
					if (f.module.adminDepartment.assignmentGradeValidation && validGrades.get(f.universityId).isDefined && validGrades(f.universityId).exists(_.isDefault))
						"populated"
					else
						"invalid"
			}
		})
		ValidateAndPopulateFeedbackResult(
			parsedFeedbacks.getOrElse("valid", Seq()),
			parsedFeedbacks.get("populated").map(feedbacksToPopulate =>
				feedbacksToPopulate.map(f => f -> validGrades(f.universityId).find(_.isDefault).map(_.grade).get).toMap
			).getOrElse(Map()),
			parsedFeedbacks.get("invalid").map(feedbacksToPopulate =>
				feedbacksToPopulate.map(f => f -> validGrades.get(f.universityId).map(_.map(_.grade).mkString(", ")).getOrElse("")).toMap
			).getOrElse(Map())
		)
	}

}

@Service("feedbackForSitsService")
class FeedbackForSitsServiceImpl
	extends AbstractFeedbackForSitsService
	with AutowiringFeedbackForSitsDaoComponent