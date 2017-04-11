package uk.ac.warwick.tabula.system

import uk.ac.warwick.tabula.TestBase
import org.springframework.stereotype._
import org.springframework.mock.web.MockHttpServletRequest

class AllowedFieldsBindingTest extends TestBase {

	@Test
	def annotatedClasses() {
		val cmd = new TestCommand
		val binder = new CustomDataBinder(cmd, "command") with AllowedFieldsBinding

		binder.usesDisallowedAnnotation(classOf[TestService]) should be {true}

		val request = new MockHttpServletRequest
		request.addParameter("value", "top")
		request.addParameter("bean.value", "beany")
		request.addParameter("repo.value", "repoval")
		request.addParameter("service.value", "sval")
		request.addParameter("component.value", "compval")
		request.addParameter("controller.value", "conval")
		request.addParameter("secretValue", "I have changed your value")
		request.addParameter("dept", "this doesn't exist, ignored")
		binder.bind(request)

		// These should bind okay
		cmd.value should be ("top")
		cmd.bean.value should be ("beany")

		// These should not get bound
		cmd.repo.value should be ("")
		cmd.service.value should be ("")
		cmd.component.value should be ("")
		cmd.controller.value should be ("")
		cmd.secretValue should be ("secrits")
	}

	trait HasValue { var value = "" }

	class TestCommand extends HasValue {
		var repo = new TestRepo
		var service = new TestService
		var component = new TestComponent
		var controller = new TestController
		var bean = new RegularBean
		@NoBind var secretValue = "secrits"
	}

	@Repository class TestRepo extends HasValue
	@Service class TestService extends HasValue
	@Component class TestComponent extends HasValue
	@Controller class TestController extends HasValue
	class RegularBean extends HasValue



}