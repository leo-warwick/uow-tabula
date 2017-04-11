package uk.ac.warwick.tabula.coursework

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class CourseworkDisplaySettingsTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to set display settings for a department" in as(P.Admin1) {
		click on linkText("Go to the Test Services admin page")
		openDisplaySettings()

		checkbox("showStudentName").select()

		cssSelector("#displaySettingsCommand input.btn-primary").webElement.click()

		// Ensure that we've been redirected back to the dept admin page
		currentUrl should endWith ("/department/xxx/")

		// Check that when we go back to the page, all of the settings have been populated
		openDisplaySettings()

		checkbox("showStudentName").isSelected should be {true}
	}

	"Department admin" should "be able to set default group signup method for a department" in {

		Given("The smallGroupTeachingStudentSignUp feature is enabled")
		enableFeature("smallGroupTeachingStudentSignUp")

		as(P.Admin1) {
		When("an administrator goes to the department settings page")
			click on linkText("Go to the Test Services admin page")
 			openDisplaySettings()

	  Then("the administrator can see the option to set a default signup method")
			radioButtonGroup("defaultGroupAllocationMethod") should not be null


		When("the administrator selects 'Manual Allocation' and submits")
			radioButtonGroup("defaultGroupAllocationMethod").value= "Manual"
			submit()

		Then("The settings page should show 'Manual Allocation' as the selected method")
			openDisplaySettings()
			radioButtonGroup("defaultGroupAllocationMethod").selection should be(Some("Manual"))

		When("the administrator selects 'Student Sign-up' and submits")
			radioButtonGroup("defaultGroupAllocationMethod").value= "StudentSignUp"
			submit()

		Then("The settings page should show 'Student Sign-up' as the selected method")
			openDisplaySettings()
			radioButtonGroup("defaultGroupAllocationMethod").selection should be(Some("StudentSignUp"))


		}}

	def openDisplaySettings(): Unit = {
		eventually {
			find(cssSelector(".dept-settings a.dropdown-toggle")) should be('defined)
		}
		click on cssSelector(".dept-settings a.dropdown-toggle")
		val displayLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText("Department settings"))
		eventually {
			displayLink.isDisplayed should be {true}
		}
		click on displayLink
	}

}