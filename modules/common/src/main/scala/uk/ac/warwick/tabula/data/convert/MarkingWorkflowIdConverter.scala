package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.MarkingWorkflow

import uk.ac.warwick.tabula.system.TwoWayConverter

class MarkingWorkflowIdConverter extends TwoWayConverter[String, MarkingWorkflow] with Daoisms {

	override def convertLeft(scheme: MarkingWorkflow) = Option(scheme) match {
		case Some(s) => s.id
		case None => null
	}
	
	override def convertRight(id: String) = getById[MarkingWorkflow](id).orNull

}
