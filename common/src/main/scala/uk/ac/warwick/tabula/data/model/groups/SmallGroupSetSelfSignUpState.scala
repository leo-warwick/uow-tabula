package uk.ac.warwick.tabula.data.model.groups

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType

sealed abstract class SmallGroupSetSelfSignUpState(val name: String)

object SmallGroupSetSelfSignUpState {
	case object Open extends SmallGroupSetSelfSignUpState("open")
	case object Closed extends SmallGroupSetSelfSignUpState("close")

	//Traditional comment - lame manual set keep in synch with above
	val members = Seq(Open, Closed)
	private[groups] def find(i: String) = members find { _.name == i }

	def apply(i: String): SmallGroupSetSelfSignUpState = find(i) getOrElse { throw new IllegalArgumentException(s"Invalid value for self sign-up state: $i") }
}

class SmallGroupSetSelfSignUpStateUserType extends AbstractBasicUserType[SmallGroupSetSelfSignUpState, String] {
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = SmallGroupSetSelfSignUpState(string)
	override def convertToValue(state: SmallGroupSetSelfSignUpState): String = state.name
}