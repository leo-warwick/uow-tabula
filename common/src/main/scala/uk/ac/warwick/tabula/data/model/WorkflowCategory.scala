package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class WorkflowCategory(val code: String, val displayName: String) {
	// For Spring
	def getCode: String = code

	def getDisplayName: String = displayName
}

object WorkflowCategory {

	case object NotDecided extends WorkflowCategory("ND", "Please select a workflow")

	case object NoneUse extends WorkflowCategory("N", "None")

	case object SingleUse extends WorkflowCategory("S", "Single Use")

	case object Reusable extends WorkflowCategory("R", "Reusable")


	def values = Seq(NotDecided, NoneUse, SingleUse, Reusable)

	def fromCode(code: String): WorkflowCategory = code match {
		case NotDecided.code => NotDecided
		case NoneUse.code => NoneUse
		case SingleUse.code => SingleUse
		case Reusable.code => Reusable
		case null => null
		case _ => throw new IllegalArgumentException()
	}

}

class WorkflowCategoryUserType extends AbstractBasicUserType[WorkflowCategory, String] {

	val basicType = StandardBasicTypes.STRING

	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): WorkflowCategory = WorkflowCategory.fromCode(string)

	override def convertToValue(ctg: WorkflowCategory): String = ctg.code
}
