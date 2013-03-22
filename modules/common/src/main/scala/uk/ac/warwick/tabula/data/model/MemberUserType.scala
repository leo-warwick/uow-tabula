package uk.ac.warwick.tabula.data.model

import scala.beans.BeanProperty
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class MemberUserType(val dbValue: String, @BeanProperty val description: String)

object MemberUserType {
	case object Student extends MemberUserType("S", "Student")
	case object Staff extends MemberUserType("N", "Staff")
	case object Emeritus extends MemberUserType("A", "Emeritus Academic")
	case object Other extends MemberUserType("O", "Other")
	
	def fromCode(code: String) = code match {
	  	case Student.dbValue => Student
	  	case Staff.dbValue => Staff
	  	case Emeritus.dbValue => Emeritus
	  	case Other.dbValue => Other
	  	case null => null
	  	case _ => throw new IllegalArgumentException("Unexpected value: " + code)
	}
	
	val StudentTargetGroups = Set(
		"FE shared student", "Distance learning student", "Foundation degree students", 
		"HEFP students", "HE shared student", "PGCE student", "Postgraduate (research) FT", 
		"Postgraduate (research) PT", "Postgraduate (taught) FT", "Postgraduate (taught) PT", 
		"Postgraduate extension student", "Pre-sessional student", "Undergraduate - full-time", 
		"Undergraduate - part-time", "Exchange student", "HE shared student"
	)
	
	val StaffTargetGroups = Set(
		"Academic-related staff", "Clerical staff", "Manual staff", 
		"Technical staff", "Security", "Casual staff", "Third party contract staff",
		"University staff", "Visiting academic", "External business' staff", "Honorary Teaching Staff"
	)
	
	val AcademicTargetGroups = Set(
		"Academic staff", "Emeritus Academic"
	)
	
	def fromTargetGroup(targetGroup: String) = targetGroup match {
		case group if StudentTargetGroups.contains(group) => Student
		case group if StaffTargetGroups.contains(group) => Staff
		case group if AcademicTargetGroups.contains(group) => Emeritus
		case null => null
		case _ => Other
	}
}

// Hmm, badly named.
class MemberUserTypeUserType extends AbstractBasicUserType[MemberUserType, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = MemberUserType.fromCode(string)
	
	override def convertToValue(MemberUserType: MemberUserType) = MemberUserType.dbValue

}