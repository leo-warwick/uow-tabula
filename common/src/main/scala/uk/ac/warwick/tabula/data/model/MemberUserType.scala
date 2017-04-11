package uk.ac.warwick.tabula.data.model

sealed abstract class MemberUserType(val dbValue: String, val description: String)

object MemberUserType {
	case object Other     extends MemberUserType("O", "Other")
	case object Student   extends MemberUserType("S", "Student")
	case object Staff     extends MemberUserType("N", "Staff")
	case object Emeritus  extends MemberUserType("A", "Emeritus Academic")
	case object Applicant extends MemberUserType("P", "Applicant")

	def fromCode(code: String): MemberUserType = code match {
	  	case Student.dbValue => Student
	  	case Staff.dbValue => Staff
	  	case Emeritus.dbValue => Emeritus
			case Applicant.dbValue => Applicant
	  	case Other.dbValue => Other
	  	case null => null
	  	case _ => throw new IllegalArgumentException("Unexpected value: " + code)
	}

	val StudentTargetGroups = Set(
		"FE shared student", "Distance learning student", "Foundation degree students",
		"HEFP students", "HE shared student", "PGCE student", "Postgraduate (research) FT",
		"Postgraduate (research) PT", "Postgraduate (taught) FT", "Postgraduate (taught) PT",
		"Postgraduate extension student", "Pre-sessional student", "Undergraduate - full-time",
		"Undergraduate - part-time", "Exchange student", "Bursary Researcher", "Student - non credit-bearing"
	)

	val StaffTargetGroups = Set(
		"Academic-related staff", "Clerical staff", "Manual staff",
		"Technical staff", "Security", "Casual staff", "Third party contract staff",
		"University Staff", "Visiting academic", "External business' staff", "Honorary Teaching Staff",
		"Academic Partner", "External Partner", "Sessional tutor"
	)

	val AcademicTargetGroups = Set(
		"Academic staff", "Emeritus Academic"
	)

	def fromTargetGroup(targetGroup: String): MemberUserType = targetGroup match {
		case group if StudentTargetGroups.contains(group) => Student
		case group if StaffTargetGroups.contains(group) => Staff
		case group if AcademicTargetGroups.contains(group) => Emeritus
		case "Applicant" => Applicant
		case null => null
		case _ => Other
	}
}

// Hmm, badly named.
class MemberUserTypeUserType extends AbstractStringUserType[MemberUserType] {
	override def convertToObject(string: String): MemberUserType = MemberUserType.fromCode(string)
	override def convertToValue(MemberUserType: MemberUserType): String = MemberUserType.dbValue
}