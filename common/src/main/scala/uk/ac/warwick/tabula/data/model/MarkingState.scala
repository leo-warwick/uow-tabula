package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class MarkingState(val name: String) {

	def transitionStates: Set[MarkingState]

	def canTransitionTo(state: MarkingState): Boolean = {
		state == this || transitionStates.contains(state)
	}

	override def toString: String = name
}

object MarkingState {

	// initial state - ready to be distributed to markers
	case object ReleasedForMarking extends MarkingState("ReleasedForMarking"){ def transitionStates = Set(InProgress, MarkingCompleted) }
	// has been downloaded by the marker and is being marked
	case object InProgress extends MarkingState("InProgress"){ def transitionStates =  Set(MarkingCompleted) }
	// the uploaded feedback was rejected by a moderator and must be reviewed
	case object Rejected extends MarkingState("Rejected"){ def transitionStates = Set(ReleasedForMarking, MarkingCompleted) }
	// submission has been marked and feedback has been uploaded
	case object MarkingCompleted extends MarkingState("MarkingCompleted"){ def transitionStates = Set(Rejected) }

	val values: Set[MarkingState] = Set(ReleasedForMarking, InProgress, MarkingCompleted, Rejected)


	def fromCode(code: String): MarkingState =
		if (code == null || code == "Received") null
		else values.find{_.name == code} match {
			case Some(state) => state
			case None => throw new IllegalArgumentException(s"Invalid marking state: $code")
		}
}

class MarkingStateUserType extends AbstractBasicUserType[MarkingState, String]{

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): MarkingState = MarkingState.fromCode(string)
	override def convertToValue(state: MarkingState): String = state.name
}
