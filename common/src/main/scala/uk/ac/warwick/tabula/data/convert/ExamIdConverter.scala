package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Exam
import uk.ac.warwick.tabula.services.AssessmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

class ExamIdConverter extends TwoWayConverter[String, Exam] {

	@Autowired var service: AssessmentService = _

	override def convertRight(id: String): Exam = (Option(id) flatMap { service.getExamById }).orNull
	override def convertLeft(exam: Exam): String = (Option(exam) map {_.id}).orNull

}