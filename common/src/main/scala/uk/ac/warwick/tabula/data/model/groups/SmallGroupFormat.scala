package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class SmallGroupFormat(val code: String, val description: String) {
	def plural: String = description + "s"

	// For Spring, the silly bum
	def getCode: String = code
	def getDescription: String = description

	override def toString: String = description
}

object SmallGroupFormat {
	case object Seminar extends SmallGroupFormat("seminar", "Seminar")
	case object Lab extends SmallGroupFormat("lab", "Lab")
	case object Tutorial extends SmallGroupFormat("tutorial", "Tutorial")
	case object Project extends SmallGroupFormat("project", "Project group")
	case object Example extends SmallGroupFormat("example", "Example Class") { override def plural = "Example Classes" }
	case object Workshop extends SmallGroupFormat("workshop", "Workshop")
	case object Lecture extends SmallGroupFormat("lecture", "Lecture")
	case object Meeting extends SmallGroupFormat("meeting", "Meeting")
	case object Exam extends SmallGroupFormat("exam", "Exam")

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Seminar, Lab, Tutorial, Project, Example, Workshop, Lecture, Exam, Meeting)

	def fromCode(code: String): SmallGroupFormat =
		if (code == null) null
		else members.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}

	def fromDescription(description: String): SmallGroupFormat =
		if (description == null) null
		else members.find{_.description == description} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class SmallGroupFormatUserType extends AbstractBasicUserType[SmallGroupFormat, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): SmallGroupFormat = SmallGroupFormat.fromCode(string)
	override def convertToValue(format: SmallGroupFormat): String = format.code
}