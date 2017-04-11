package uk.ac.warwick.tabula.web

import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.LoginDetails

trait SysadminFixtures extends BrowserTest {

	before {
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")
	}

	def as[T](user: LoginDetails)(fn: => T): T = {
		currentUser = user
		signIn as user to Path("/sysadmin")
		fn
	}

	def withGodModeEnabled[T](fn: =>T): T ={
		go to Path("/sysadmin")
		find("enable-godmode-button").foreach(e=>{
			click on e
		})
		// if it wasn't found, perhaps god-mode is already enabled. Either way, check that it's enabled now.
		eventually(pageSource.contains("God mode enabled") should be (true))
		fn
	}
}