package uk.ac.warwick.tabula

import uk.ac.warwick.userlookup.User
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services._
import org.junit.Before

trait LenientUserLookup extends Mockito {

	@Autowired var userLookup:SwappableUserLookupService =_

	@Before def setup {
		userLookup.delegate = lenientUserLookup
	}

	def lenientUserLookup: UserLookupService = {
		val backend = mock[UserLookupService]

		backend.getUserByWarwickUniId(any[String]) answers { id =>
			new User(id.asInstanceOf[String]) {
				setFoundUser(true)
			}
		}

		backend
	}
}