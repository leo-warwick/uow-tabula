package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.userlookup.User

class UserCodeConverterTest extends TestBase with Mockito {
	
	val converter = new UserCodeConverter
	val userLookup = mock[UserLookupService]
	converter.userLookup = userLookup
	
	@Test def validInput {
		val user = new User
		user.setUserId("cuscav")
			
		userLookup.getUserByUserId("cuscav") returns (user)
		
		converter.convertRight("cuscav") should be (user)
	}
	
	@Test def invalidInput {
		userLookup.getUserByUserId("20X6") returns (new AnonymousUser)
		
		converter.convertRight("20X6") should be (new AnonymousUser)
	}
	
	@Test def formatting {
		converter.convertLeft(new User("cuscav")) should be ("cuscav")
		converter.convertLeft(null) should be (null)
	}

}