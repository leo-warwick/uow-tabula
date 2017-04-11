package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User

object SSOUserType {
	var userLookup: UserLookupService = _
}

/**
 * Stores a User by userid.
 */
class SSOUserType extends AbstractStringUserType[User] {
	lazy val _userLookup: UserLookupService = Wire[UserLookupService]

	private def userLookup =
		if (SSOUserType.userLookup != null) SSOUserType.userLookup
		else _userLookup

	override def convertToValue(obj: User): String = obj.getUserId

	override def convertToObject(input: String): User = userLookup.getUserByUserId(input)
}
