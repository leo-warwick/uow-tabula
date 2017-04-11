package uk.ac.warwick.tabula.commands.sysadmin

import uk.ac.warwick.tabula.{CurrentUser, TestBase}

class GodModeCommandTest extends TestBase {

	@Test def set {
		val cmd = new GodModeCommand

		val cookie = cmd.applyInternal
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.godModeCookie)
			cookie.cookie.getValue() should be ("true")
			cookie.cookie.getPath() should be ("/")
		}
	}

	@Test def remove {
		val cmd = new GodModeCommand
		cmd.action = "remove"

		val cookie = cmd.applyInternal
		cookie should be ('defined)
		cookie.map { cookie =>
			cookie.cookie.getName() should be (CurrentUser.godModeCookie)
			cookie.cookie.getValue() should be ("false") // removal
			cookie.cookie.getPath() should be ("/")
		}
	}

}