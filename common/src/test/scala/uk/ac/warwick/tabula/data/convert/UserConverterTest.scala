package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.userlookup.User

class UserConverterTest extends TestBase with Mockito {

	val converter = new UserConverter
	val userLookup: UserLookupService = mock[UserLookupService]
	converter.userLookup = userLookup

	@Test def validInput {
		val user = new User { setUserId("cuscav"); setFoundUser(true) }

		userLookup.getUserByUserId("cuscav") returns (user)

		converter.convertRight("cuscav") should be (user)
	}

	@Test def validFallback {
		val user = new User { setWarwickId("1170836"); setFoundUser(true) }

		userLookup.getUserByWarwickUniId("1170836") returns (user)

		converter.convertRight("1170836") should be (user)
	}

	@Test def invalidInput {
		userLookup.getUserByUserId("20X6") returns (new AnonymousUser)

		converter.convertRight("20X6") should be (new AnonymousUser)
	}

	@Test def formatting {
		converter.convertLeft(new User("cuscav")) should be ("cuscav")

		val user = new User("cuslaj"){ setWarwickId("1170836") }
		converter.convertLeft(user) should be ("cuslaj")
		converter.convertLeft(null) should be (null)
	}

}