package uk.ac.warwick.tabula.web.controllers.admin

import javax.servlet.http.HttpServletResponse

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.admin.MasqueradeCommandState
import uk.ac.warwick.tabula.commands.{Appliable, Describable}
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.tabula.web.{Cookie, Routes}
import uk.ac.warwick.tabula.{Mockito, TestBase}

class MasqueradeControllerTest extends TestBase with Mockito {

	val controller = new MasqueradeController

	@Test def createCommand = withUser("cuscav") {
		val command = controller.command()

		command should be (anInstanceOf[Appliable[Option[Cookie]]])
		command should be (anInstanceOf[MasqueradeCommandState])
		command should be (anInstanceOf[Describable[Option[Cookie]]])
	}

	@Test def form {
		controller.form(mock[Appliable[Option[Cookie]]]).viewName should be ("admin/masquerade/form")
	}

	@Test def submit {
		val cookie = mock[Cookie]
		val command = mock[Appliable[Option[Cookie]]]
		command.apply() returns (Some(cookie))

		val response = mock[HttpServletResponse]

		controller.submit(command, new BindException(command, "command"), response).viewName should be (s"redirect:${Routes.admin.masquerade}")
		verify(response, times(1)).addCookie(cookie)
	}

}
