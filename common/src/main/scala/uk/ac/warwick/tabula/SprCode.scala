package uk.ac.warwick.tabula

/**
 * SPR (student programme route) code is used in SITS to identify a student on a particular course of theirs.
 * it has the format UNIID/ROUTENUM. Routenum starts at 1 and is incremented whenever the student
 * starts a new course. We don't really care about this number so we can strip it off and just store
 * the Uni ID.
 */
object SprCode {
	def getUniversityId(sprCode: String): String = {
		val slash = sprCode.indexOf("/")
		if (slash == -1) {
			sprCode
		} else {
			sprCode.substring(0, slash)
		}
	}
}