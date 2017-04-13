package uk.ac.warwick.tabula

/**
 * This is not a particular test for anything - just a place
 * where I'm working out how Selenium would work.
 */
class SimpleTest extends BrowserTest {

	"The home page" must "have a good title" in {
		go to (Path("/"))
		pageTitle should be ("Tabula")
	}

	"The profiles home page" must "redirect to login" in {
		go to (Path("/profiles/"))
		pageTitle should include ("Sign in")
		currentUrl should startWith ("https://websignon.warwick.ac.uk/")
	}

}

