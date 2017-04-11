package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{UserLookupComponent, UserLookupService}
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.UniversityId

/**
 *
 * TODO - Work out a better way of doing this!
 *
 * We need to convert users from both usercodes and ids. Convert right will attempt to convert as if the supplied
 * string was a university ID. If this fails then we will try to convert based on usercodes instead.
 *
 * Convert left will always transform a User to a usercode.
 */
class UserConverter extends TwoWayConverter[String, User] with FetchByUniIdOrUsercode with UserLookupComponent {

	var userLookup: UserLookupService = Wire[UserLookupService]

	override def convertRight(userId: String): User = fetchUser(userId)

	override def convertLeft(user: User): String = (Option(user) map { _.getUserId }).orNull

}

trait FetchByUniIdOrUsercode {

	this : UserLookupComponent =>

	def fetchUser(identifier: String): User = {
		if (UniversityId.isValid(identifier)) {
			Option(userLookup.getUserByWarwickUniId(identifier))
				.filter { _.isFoundUser } // We don't consider not-found users
				.getOrElse(userLookup.getUserByUserId(identifier))
		} else {
			userLookup.getUserByUserId(identifier)
		}
	}
}