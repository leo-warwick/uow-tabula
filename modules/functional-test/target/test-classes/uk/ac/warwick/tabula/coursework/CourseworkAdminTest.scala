package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.BrowserTest

class CourseworkAdminTest extends BrowserTest with CourseworkFixtures {

	"Department admin" should "be offered a link to their department" in as(P.Admin1) {
		pageTitle should be ("Tabula - Coursework Management")
		click on linkText("Go to the Test Services admin page")

		// check that we can see some modules on the page.
		findAll(className("module-info")).toList should not be (Nil)

		// But check that some are hidden
		val allDisplayed = findAll(className("module-info")).forall(_.isDisplayed)
		allDisplayed should be (false)

		click on (linkText("Show"))

		// Now all modules should be displayed
		for (info <- findAll(className("module-info")))
			info.isDisplayed should be (true)
	}

}