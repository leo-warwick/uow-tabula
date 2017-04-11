package uk.ac.warwick.tabula.web.views

import java.util.HashMap

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.support.GenericWebApplicationContext
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.support.RequestContext
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.LazyLists

class MyCommand {
	var attached: JList[FileAttachment] = LazyLists.create()
}

class BindStatusTest extends TestBase {

	@Test def bindList {
		val ctx = new GenericWebApplicationContext

		val req = new MockHttpServletRequest
		req.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, ctx)
		val request = new RequestContext(req, new HashMap[String,Object])

		val command = new MyCommand
		val attachment = new FileAttachment
		command.attached.add(attachment)
		request.getModel().put("myCommand", command)
		val status = request.getBindStatus("myCommand.attached")
		status.getValue() should be (command.attached)
	}
}