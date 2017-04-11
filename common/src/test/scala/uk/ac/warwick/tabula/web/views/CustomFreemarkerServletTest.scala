package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.RequestInfo
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import freemarker.template.{ObjectWrapper, Template, TemplateModel, SimpleHash}
import java.io.StringReader
import freemarker.template.utility.DeepUnwrap

class CustomFreemarkerServletTest extends TestBase with Mockito {

	val servlet = new CustomFreemarkerServlet

	@Test def preTemplateProcess = withUser("cuscav") {
		val req = new MockHttpServletRequest
		val res = new MockHttpServletResponse

		val template = new Template("mytemplate", new StringReader(""), null)
		val model = new SimpleHash(null.asInstanceOf[ObjectWrapper])
		model.put("contentType", "text/plain")

		servlet.preTemplateProcess(req, res, template, model)

		DeepUnwrap.unwrap(model.get("user")) should be (currentUser)
		DeepUnwrap.unwrap(model.get("info")) should be (RequestInfo.fromThread.get)

		res.getContentType() should be ("text/plain")
	}

}