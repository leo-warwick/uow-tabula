package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.web.Uri

class ControllerViewsTest extends TestBase {

	@Test def redirect {
		val views = new ControllerViews {
			def requestInfo: Some[RequestInfo] = {
				val u = new User("cuscav")
				u.setIsLoggedIn(true)
				u.setFoundUser(true)

				val currentUser = new CurrentUser(u, u)

				Some(new RequestInfo(currentUser, Uri.parse("http://www.example.com/page"), Map()))
			}
		}

		views.Redirect("/yes").viewName should be ("redirect:/yes")
	}

	@Test def redirectWithNoRequestInfo {
		val views = new ControllerViews {
			def requestInfo = None
		}

		views.Redirect("/yes").viewName should be ("redirect:/yes")
	}

	@Test def redirectWithReturnTo {
		val views = new ControllerViews {
			def requestInfo: Some[RequestInfo] = {
				val u = new User("cuscav")
				u.setIsLoggedIn(true)
				u.setFoundUser(true)

				val currentUser = new CurrentUser(u, u)

				Some(new RequestInfo(currentUser, Uri.parse("http://www.example.com/page"), Map("returnTo" -> List("/woah"))))
			}
		}

		views.Redirect("/yes").viewName should be ("redirect:/woah")
	}

	@Test def redirectToSignin = withSSOConfig() {
		val views = new ControllerViews {
			def requestInfo: Some[RequestInfo] = {
				val u = new User("cuscav")
				u.setIsLoggedIn(true)
				u.setFoundUser(true)

				val currentUser = new CurrentUser(u, u)

				Some(new RequestInfo(currentUser, Uri.parse("http://www.example.com/page"), Map()))
			}
		}

		views.RedirectToSignin().viewName should startWith ("redirect:https://xebsignon.warwick.ac.uk/")
	}

}