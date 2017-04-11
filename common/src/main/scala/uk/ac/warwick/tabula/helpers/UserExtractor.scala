package uk.ac.warwick.tabula.helpers
import uk.ac.warwick.userlookup.User

/**
 * A pattern which you can use in a match statement to match a User
 * object that exists and isn't an anonymous user.
 *
 * userLookup.getUserByUserId("jimmy") match {
 *   case FoundUser(user) => // do stuff with this user
 *   case NoUser(user) => // do stuff when jimmy doesn't exist
 * }
 *
 */
object FoundUser {
	def unapply(user: User): Option[User] = if (user.isFoundUser()) Some(user) else None
}

object NoUser {
	def unapply(user: User): Option[User] = if (user.isFoundUser()) None else Some(user)
}