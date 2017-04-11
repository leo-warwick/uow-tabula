package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.Member

class SearchProfilesCommandTest extends TestBase with Mockito {

	@Test def validQuery() {

		val member = smartMock[Member]
		val user = smartMock[CurrentUser]

		val cmd = SearchProfilesCommand(member, user)

		// query needs to be at least 3 characters
		cmd.query = "xx"
		cmd.validQuery should be(false)

		// and the minimum term length is two
		cmd.query = "x x x x"
		cmd.validQuery should be(false)

		cmd.query = "select foo from bar"
		cmd.validQuery should be(true)
	}
}
