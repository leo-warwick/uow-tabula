package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class Gender(val dbValue: String, val description: String)

object Gender {
	case object Male extends Gender("M", "Male")
	case object Female extends Gender("F", "Female")
	case object Other extends Gender("N", "Other")
	case object Unspecified extends Gender("P", "Prefer not to say")

	def fromCode(code: String): Gender = code match {
	  	case Male.dbValue => Male
	  	case Female.dbValue => Female
	  	case Other.dbValue => Other
	  	case Unspecified.dbValue => Unspecified
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}

class GenderUserType extends AbstractBasicUserType[Gender, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): Gender = Gender.fromCode(string)

	override def convertToValue(gender: Gender): String = gender.dbValue

}