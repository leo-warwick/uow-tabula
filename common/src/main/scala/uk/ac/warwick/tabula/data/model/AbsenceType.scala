package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class AbsenceType(val dbValue: String, val description: String)

object AbsenceType {
	case object Academic extends AbsenceType("academic", "Academic")
	case object Medical extends AbsenceType("medical", "Medical")
	case object Personal extends AbsenceType("personal", "Personal")
	case object ChangeOfStudyLocation extends AbsenceType("cosl", "Change of study location")
	case object SelfCertification extends AbsenceType("self", "Self certification")
	case object Cancelled extends AbsenceType("cancel", "Event cancelled")
	case object Unknown extends AbsenceType("unknown", "Unknown")
	case object Other extends AbsenceType("other", "Other")

	def fromCode(code: String): AbsenceType = code match {
		case Academic.dbValue => Academic
		case Medical.dbValue => Medical
		case Personal.dbValue => Personal
		case ChangeOfStudyLocation.dbValue => ChangeOfStudyLocation
		case SelfCertification.dbValue => SelfCertification
		case Cancelled.dbValue => Cancelled
		case Unknown.dbValue => Unknown
		case Other.dbValue => Other
		case null => null
		case _ => throw new IllegalArgumentException()
	}

	val values: Seq[AbsenceType] = Seq(Academic, Medical, Personal, ChangeOfStudyLocation, SelfCertification, Cancelled, Unknown, Other)
}

class AbsenceTypeUserType extends AbstractBasicUserType[AbsenceType, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): AbsenceType = AbsenceType.fromCode(string)

	override def convertToValue(state: AbsenceType): String = state.dbValue

}