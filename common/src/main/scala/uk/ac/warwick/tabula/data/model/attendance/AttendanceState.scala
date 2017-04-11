package uk.ac.warwick.tabula.data.model.attendance

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types
import uk.ac.warwick.tabula.data.model.AbstractBasicUserType

sealed abstract class AttendanceState(val dbValue: String, val description: String)

object AttendanceState {
	case object Attended extends AttendanceState("attended", "Attended")
	case object MissedAuthorised extends AttendanceState("authorised", "Missed (authorised)")
	case object MissedUnauthorised extends AttendanceState("unauthorised", "Missed (unauthorised)")
	case object NotRecorded extends AttendanceState("not-recorded", "Unrecorded") // Equivalent to null

	def fromCode(code: String): AttendanceState = code match {
	  	case Attended.dbValue => Attended
	  	case MissedAuthorised.dbValue => MissedAuthorised
	  	case MissedUnauthorised.dbValue => MissedUnauthorised
			case NotRecorded.dbValue => NotRecorded
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}

	val values: Seq[AttendanceState] = Seq(MissedUnauthorised, MissedAuthorised, Attended, NotRecorded)
}

class AttendanceStateUserType extends AbstractBasicUserType[AttendanceState, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): AttendanceState = AttendanceState.fromCode(string)

	override def convertToValue(state: AttendanceState): String = state.dbValue

}