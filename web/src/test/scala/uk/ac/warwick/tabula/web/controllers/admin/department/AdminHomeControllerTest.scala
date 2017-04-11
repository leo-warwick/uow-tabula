package uk.ac.warwick.tabula.web.controllers.admin.department

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.web.Routes

class AdminHomeControllerTest extends TestBase {

	val controller = new AdminDepartmentHomeRedirectController

	@Test def redirect = withUser("cuscav") {
		controller.homeScreen(currentUser).viewName should be (s"redirect:${Routes.admin.home}")
	}

}
