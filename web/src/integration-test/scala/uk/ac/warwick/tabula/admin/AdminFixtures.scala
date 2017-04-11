package uk.ac.warwick.tabula.admin

import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.LoginDetails

trait AdminFixtures extends BrowserTest {

	before {
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")
	}

	def as[T](user: LoginDetails)(fn: => T): T = {
		currentUser = user
		signIn as user to Path("/admin")

		fn
	}
}