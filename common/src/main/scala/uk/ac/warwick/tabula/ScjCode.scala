package uk.ac.warwick.tabula

/**
 * SCJ (student course join) code is used in SITS to identify a student on a particular course of theirs.
 * it has the format UNIID/COURSENUM. Coursenum starts at 1 and is incremented whenever the student
 * starts a new course.
 */
object ScjCode {
	def getUniversityId(sprCode: String): String = {
		val slash = sprCode.indexOf("/")
		if (slash == -1) {
			sprCode
		} else {
			sprCode.substring(0, slash)
		}
	}
}