package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.markingworkflow.CM2MarkingWorkflow
import uk.ac.warwick.tabula.system.TwoWayConverter

class CM2MarkingWorkflowIdConverter extends TwoWayConverter[String, CM2MarkingWorkflow] with Daoisms {

	override def convertLeft(scheme: CM2MarkingWorkflow): String = Option(scheme) match {
		case Some(s) => s.id
		case None => null
	}

	override def convertRight(id: String): CM2MarkingWorkflow = getById[CM2MarkingWorkflow](id).getOrElse(
		throw new IllegalArgumentException()
	)

}
