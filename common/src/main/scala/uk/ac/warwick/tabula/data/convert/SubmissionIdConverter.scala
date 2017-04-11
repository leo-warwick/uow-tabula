package uk.ac.warwick.tabula.data.convert
import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.system.TwoWayConverter

class SubmissionIdConverter extends TwoWayConverter[String, Submission] {

	@Autowired var service: SubmissionService = _

	override def convertRight(id: String): Submission = service.getSubmission(id).orNull
	override def convertLeft(submission: Submission): String = (Option(submission) map { _.id }).orNull

}