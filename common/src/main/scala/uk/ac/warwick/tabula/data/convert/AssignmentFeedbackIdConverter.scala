package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.data.model.{AssignmentFeedback, Feedback}
import uk.ac.warwick.tabula.system.TwoWayConverter

class AssignmentFeedbackIdConverter extends TwoWayConverter[String, AssignmentFeedback] {

	@Autowired var service: FeedbackDao = _

	override def convertRight(id: String): AssignmentFeedback = service.getAssignmentFeedback(id).orNull
	override def convertLeft(feedback: AssignmentFeedback): String = (Option(feedback) map {_.id}).orNull

}
