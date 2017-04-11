package uk.ac.warwick.tabula.data.convert


import uk.ac.warwick.tabula.data.model.WorkflowCategory
import uk.ac.warwick.tabula.system.TwoWayConverter

class WorkflowCategoryConverter extends TwoWayConverter[String, WorkflowCategory] {

		override def convertRight(code: String): WorkflowCategory = WorkflowCategory.fromCode(code)
		override def convertLeft(ctg: WorkflowCategory): String = (Option(ctg) map { _.code }).orNull

}
