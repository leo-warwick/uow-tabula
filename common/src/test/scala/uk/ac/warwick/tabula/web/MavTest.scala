package uk.ac.warwick.tabula.web

import uk.ac.warwick.tabula.TestBase
import org.springframework.web.servlet.view.RedirectView

class MavTest extends TestBase {

	@Test def itWorks {
		val mav = Mav("my/view",
			"steve" -> "loves you",
			"yes" -> false
		).crumbs(
			BreadCrumb("title", "/url"),
			BreadCrumb("title2", "/url2", "tooltip")
		).bodyClasses("body", "rocking").withTitle("my title").xml().toModelAndView

		mav.getViewName should be ("my/view")
		mav.getModel.get("steve") should be ("loves you")
		mav.getModel.get("yes").toString should be (false.toString)
		mav.getModel.get("renderLayout") should be ("none")
		mav.getModel.get("contentType") should be ("text/xml")
		mav.getModel.get("breadcrumbs") should not be (null)
		mav.getModel.get("pageTitle") should be ("my title")
		mav.getModel.get("bodyClasses") should be ("body rocking")
	}

	@Test def viewObject() {
		val view = new RedirectView("google.com")
		val mav = Mav(view)
		mav.toModelAndView.getView should be (view)
	}

}