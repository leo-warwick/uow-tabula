package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, ExamFeedback}
import uk.ac.warwick.tabula.system.TwoWayConverter

class ExamFeedbackIdConverter extends TwoWayConverter[String, ExamFeedback] {

	@Autowired var service: FeedbackDao = _

	override def convertRight(id: String): ExamFeedback = service.getExamFeedback(id).orNull
	override def convertLeft(feedback: ExamFeedback): String = (Option(feedback) map {_.id}).orNull

}
