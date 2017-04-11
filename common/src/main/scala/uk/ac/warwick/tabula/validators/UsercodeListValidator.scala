package uk.ac.warwick.tabula.validators

import scala.collection.JavaConversions._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.StringUtils._

/**
 * Reusable validator for checking the usual stuff about a list of usercodes on a command:
 * is it empty, are all the usercodes actually valid.
 *
 * Could be extended with options such as allowing it to be empty.
 */
class UsercodeListValidator(usercodes: JList[String], pathName: String, universityIdRequired: Boolean = false) {

	var userLookup: UserLookupService = Wire.auto[UserLookupService]

	def validate(errors: Errors) {
		val trimmedCodes = usercodes.filter(_.hasText).map(_.trim)
		if (usercodesEmpty) {
			errors.rejectValue(pathName, "NotEmpty")
		} else if (alreadyHasCode) {
			errors.rejectValue(pathName, "userId.duplicate")
		} else {
			val users = userLookup.getUsersByUserIds(trimmedCodes).values
			// Uses find() so we'll only show one missing user at any one time. Could change this to
			// use filter() and combine the result into one error message listing them all.
			val anonUsers = users.find { !_.isFoundUser }
			for (user <- anonUsers) {
				errors.rejectValue(pathName, "userId.notfound.specified", Array(user.getUserId), "")
			}
			if (universityIdRequired) {
				val noUniIdUsers = users.find { !_.getWarwickId.hasText }
				for (user <- noUniIdUsers) {
					errors.rejectValue(pathName, "userId.missingUniId", Array(user.getUserId), "")
				}
			}
		}
	}

	// can override for custom check for pre-existing usercode.
	def alreadyHasCode = false

	private def usercodesEmpty = !usercodes.exists(_.hasText)
}