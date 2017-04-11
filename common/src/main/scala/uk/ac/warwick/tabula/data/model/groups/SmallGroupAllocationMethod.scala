package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class SmallGroupAllocationMethod(val dbValue: String, val description: String)

object SmallGroupAllocationMethod {
	case object Manual extends SmallGroupAllocationMethod("Manual", "Manual")
	case object StudentSignUp extends SmallGroupAllocationMethod("StudentSignUp", "Self sign-up")
	case object Linked extends SmallGroupAllocationMethod("Linked", "Linked")
	case object Random extends SmallGroupAllocationMethod("Random", "Random")

  val Default = Manual
	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Manual, StudentSignUp, Linked, Random)

	def fromDatabase(dbValue: String): SmallGroupAllocationMethod ={
		if (dbValue == null) null
		else members.find{_.dbValue == dbValue} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
  }

  def apply(value:String): SmallGroupAllocationMethod = fromDatabase(value)
}

class SmallGroupAllocationMethodUserType extends AbstractBasicUserType[SmallGroupAllocationMethod, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): SmallGroupAllocationMethod = SmallGroupAllocationMethod.fromDatabase(string)
	override def convertToValue(method: SmallGroupAllocationMethod): String = method.dbValue
}