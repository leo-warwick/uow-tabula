package uk.ac.warwick.tabula.coursework

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.coursework.pages.SetupAssignmentsPage
import com.gargoylesoftware.htmlunit.BrowserVersion

/**
 * Test the setup-assignments form.
 */
class CourseworkAddAssignmentsTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	/**
	 * Sorry, code gods, for this quite long test. Yes, it contains too many interdependent steps.
	 *
	 * Also currently not testing as much as I'd like, since HtmlUnit isn't
	 * happy with this page's modals for some reason. It still tests some important
	 * stuff like the Back and Next buttons operating without any catastrophic
	 * error, which would have caught a bunch of past regressions.
	 */
	"Department admin" should "be able to batch create some assignments" in {
		val page = new SetupAssignmentsPage("xxx")

		Given("I am logged in as admin at the setup-assignments page")
		signIn as(P.Admin1) to (page.url)
		page.shouldBeCurrentPage()

		Then("I should see one item")
		val rows = page.itemRows
		rows should have length 1

		And("that item checkbox should already be selected")
		page.getCheckboxForRow(rows.head).isSelected

		And("I should be able to click Next to reach the next screen")
		page.clickNext()

		And("I should be able to adjust the assignment name")
		val nextRows = page.itemRows
		page.setTitleForRow(nextRows.head, "GOOD NEWS")

		/* FIXME This works with Chrome but not with HtmlUnit. */
//		And("I should be able to set some options")
//		executeScript("jQuery('.modal').removeClass('fade')"); //haaax
//
//		partialLinkText("Set options").webElement.click()

//		cssSelector(".modal-footer .btn-primary").webElement.click()
//
//		eventually {
//			page.getOptionIdForRow(page.itemRows.head) should be (Some("A"))
//		}

		And("I can click back")
		page.clickBack()
		assertNoFreemarkerErrors()

		And("I can click Next again")
		eventuallyAjax { page.clickNext() }

		And("My options are intact with no errors")
//		page.getOptionIdForRow(page.itemRows.head) should be (Some("A"))
		assertNoFreemarkerErrors()
	}


	def assertNoFreemarkerErrors() {
		pageSource should not include("FreeMarker")
	}

}
