package uk.ac.warwick.tabula.cm2
import org.openqa.selenium.By
import uk.ac.warwick.tabula.BrowserTest
import scala.collection.JavaConverters._
class CreateAssignmentFromSITSTest extends BrowserTest with CourseworkFixtures {
	private def openAssignmentsScreen(): Unit = {
		When("I go the admin page")
		click on linkText("Test Services")
		Then("I should be able to click on the Assignments dropdown")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Assignments"))
		And("I should see the create assignments from SITS in the assignments menu option")
		val createAssignmentsLink = toolbar.findElement(By.partialLinkText("Create assignments from SITS"))
		eventually(timeout(45.seconds), interval(300.millis)) ({
			createAssignmentsLink.isDisplayed should be {
				true
			}
		})
		When("I click the create assignments from SITS link")
		click on createAssignmentsLink
		eventually(timeout(45.seconds), interval(300.millis)) ({
			Then("I should reach the create assignments from previous page")
			currentUrl should include("/2018/setup-assignments")
		})
	}
	private def createSITSAssignment(): Unit = {
		When("I select the assignment checkbox")
		val tbody = id("main").webElement.findElement(By.tagName("tbody"))
		val row = tbody.findElements(By.tagName("tr")).asScala.find({_.findElements(By.tagName("td")).size > 0}).find({_.findElement(By.xpath("//*[contains(text(),'XXX01-16')]")).isDisplayed})
		row should be (defined)
		val assignmentCheckbox = row.get.findElement(By.id("sitsAssignmentItems0.include1"))
		if(!assignmentCheckbox.isSelected){
			assignmentCheckbox.click()
		}
		Then("The assignment checkbox and the all assignments checkboxes should be selected")
		assignmentCheckbox.isSelected should be(true)
		val allAssignmentsCheckbox = id("main").webElement.findElement(By.className("collection-check-all"))
		allAssignmentsCheckbox.isSelected should be (true)
		//TODO: Testing that you should be able to change the component name but pencil icon not showing - need to investigate
		val pencil = id("main").webElement.findElement(By.cssSelector("td.selectable a.name-edit-link"))
		pencil.isDisplayed should be (true)
		When("I Change the component name")
		click on pencil
		Then("The component name text should be turned into an editable field")
		val hideOrigText = row.get.findElements(By.tagName("span")).get(0)
		val showVisibleText = row.get.findElements(By.xpath("//input[@type='text']")).get(0)
		hideOrigText.isDisplayed should be (false)
		showVisibleText.isDisplayed should be(true)
		And("When I change the text")
		click on id("main").webElement.findElement(By.className("editable-clear-x"))
		id("main").webElement.findElement(By.className("input-sm")).sendKeys("Super essay")
		click on id("main").webElement.findElement(By.className("fa-check"))
		Then("The component name text should be be the new text")
		id("editable-name-0").webElement.getText should be ("Super essay")
		When("The I click on the Next button")
		val nextButton = id("main").webElement.findElement(By.tagName("button"))
		click on nextButton
		eventually {
			val options = id("main").webElement.findElements(By.id("options-buttons")).size()
			options should be (1)
		}
		When("The I click on the Set options button")
		val optionsButton = id("set-options-button").webElement
		optionsButton.click()
		eventually {
			Then("The modal screen for Set options opens")
			id("sharedAssignmentPropertiesForm").webElement.isDisplayed should be (true)
		}
		When("I select the automatically submission release checkbox")
		val automaticallyReleaseToMarkersCheckbox = id("automaticallyReleaseToMarkers").webElement
		eventually({
			automaticallyReleaseToMarkersCheckbox.isDisplayed should be (true)
		})
		automaticallyReleaseToMarkersCheckbox.click()
		Then("The Automatically Release To Markers Checkbox should be checked")
		automaticallyReleaseToMarkersCheckbox.isSelected should be (true)
		When("I change the credit bearing radio button to Formative")
		val formativeRadioBtn = id("summative2").webElement
		formativeRadioBtn.click()
		Then("The credit bearing radio button should be selected")
		formativeRadioBtn.isSelected should be (true)
		When("I select the automatically submission release checkbox")
		val dissertationCheckbox = id("dissertation").webElement
		dissertationCheckbox.click()
		Then("The Automatically Release To Markers Checkbox should be checked")
		dissertationCheckbox.isSelected should be (true)
		When("I change maximum attachments dropdown")
		val fileAttachmentDropbox = id("fileAttachmentLimit").webElement
		fileAttachmentDropbox.click()
		fileAttachmentDropbox.findElement(By.cssSelector("option[value='2']")).click()
		Then("The maximum attachments dropdown is set to 2")
		fileAttachmentDropbox.findElement(By.cssSelector("option[value='2']")).isSelected
		When("The I click on the Save options button")
		val saveButton = id("main").webElement.findElement(By.cssSelector("div.submit-buttons button.btn-primary"))
		click on saveButton
		/*val closeButton = id("main").webElement.findElement(By.cssSelector("button.close"))
		eventually {
			closeButton.isDisplayed should be (true)
			click on closeButton
		}
		Then("The modal screen should close")
		eventually {
			id("sharedAssignmentPropertiesForm").webElement.isDisplayed should be (false)
		}*/
		When("The I click on the Set dates button")
		val datesButton = id("set-dates-button").webElement
		click on datesButton
		var openDate = id("main").webElement.findElement(By.id("modal-open-date"))
		eventually {
			Then("The modal screen for Set dates opens")
			openDate.isDisplayed should be (true)
		}
		When("I change the open date")
		openDate.clear()
		openDate.sendKeys("02-Aug-2017 18:00:00")
		Then("The open date should be the new value")
		openDate.getAttribute("value") should be ("02-Aug-2017 18:00:00")
		var closeDate = id("main").webElement.findElement(By.id("modal-close-date"))
		When("I change the close date")
		closeDate.clear()
		closeDate.sendKeys("08-Aug-2017 09:30:00")
		Then("The close date should be the new value")
		closeDate.getAttribute("value") should be ("08-Aug-2017 09:30:00")
		When("I click on the Set dates button")
		val setDatesButton = id("main").webElement.findElement(By.cssSelector(".btn-primary"))
		click on setDatesButton
		eventually {
			Then("The modal screen for Set dates closes")
			id("sharedAssignmentPropertiesForm").webElement.isDisplayed should be (false)
		}
		And("There should still be one item selected")
		id("selected-count").webElement.getText should be ("1 selected")
		When("I click on the submit button")
		val submitButton = id("main").webElement.findElement(By.cssSelector(".btn-primary"))
		click on submitButton
		eventually {
			Then("The page should go to the assignments page")
			currentUrl should include("/department/xxx/20")
		}
	}
	"Department admin" should "be able to create assignment" in as(P.Admin1) {
		openAssignmentsScreen()
		createSITSAssignment()
	}
}