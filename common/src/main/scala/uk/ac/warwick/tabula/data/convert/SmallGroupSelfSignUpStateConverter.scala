package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSetSelfSignUpState
import uk.ac.warwick.tabula.helpers.StringUtils._

class SmallGroupSelfSignUpStateConverter extends TwoWayConverter[String, SmallGroupSetSelfSignUpState] {

	override def convertRight(value: String): SmallGroupSetSelfSignUpState =
		if (value.hasText) SmallGroupSetSelfSignUpState(value)
		else null

	override def convertLeft(state: SmallGroupSetSelfSignUpState): String = Option(state).map { _.name }.orNull

}