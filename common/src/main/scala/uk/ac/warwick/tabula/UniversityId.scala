package uk.ac.warwick.tabula
import scala.util.matching.Regex

object UniversityId {

	val ExpectedLength = 7

	/**
	 * A regex for the Warwick University ID format, which is
	 * always a 7 digit character.
	 */
	val universityIdPattern = new Regex("^[0-9]{%d}$".format(ExpectedLength))

	/** Returns whether this string matches universityIdPattern. */
	def isValid(id: String): Boolean = id match {
		case universityIdPattern() => true
		case _ => false
	}

	def zeroPad(id: String): String =
		if (id.length < ExpectedLength) "0"*(ExpectedLength-id.length) + id
		else id

}