package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.FeedbackTemplate
import uk.ac.warwick.tabula.system.TwoWayConverter

class FeedbackTemplateIdConverter extends TwoWayConverter[String, FeedbackTemplate] with Daoisms {

	override def convertRight(id: String) = session.getById[FeedbackTemplate](id).orNull
	override def convertLeft(template: FeedbackTemplate) = (Option(template) map {_.id}).orNull

}
