package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class CourseworkMarkingWorkflowTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to manage marking workflow" in as(P.Admin1) {
		def openMarkingWorkflowSettings() = {
			When("I should be able to click on the Manage button")
			click on cssSelector(".dept-settings a.dropdown-toggle")

			Then("I should see the workflows menu option")
			val markingWorkflowsLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText("Marking workflows"))
			eventually {
				markingWorkflowsLink.isDisplayed should be {true}
			}
			click on markingWorkflowsLink
		}

		When("I go the admin page")
		click on linkText("Go to the Test Services admin page")

		Then("I should be able to open workflow settings")
		openMarkingWorkflowSettings()

		And("There should be one at first")
		cssSelector("table.marking-workflows tbody tr").findAllElements.size should be (1)

		When("I click on the create workflows button")
		click on partialLinkText("Create")

		And("I enter the necessary data")
		textField("name").value = "Marking workflow 1"
		singleSel("markingMethod").value = "StudentsChooseMarker"
		textField("firstMarkers").value = P.Marker1.usercode

		Then("Another marker field should magically appear")
		eventually {
			findAll(cssSelector("input.flexi-picker")).toList.count(_.isDisplayed) should be (2)
		}

		When("I enter another marker")
		new TextField(findAll(cssSelector("input.flexi-picker")).toList.apply(1).underlying).value = P.Marker2.usercode
		submit()

		Then("I should be redirected back to the marking workflow page")
		currentUrl should endWith ("/markingworkflows")

		When("I create another marking workflow")
		click on partialLinkText("Create")
		textField("name").value = "Marking workflow 2"
		singleSel("markingMethod").value = "StudentsChooseMarker"
		textField("firstMarkers").value = P.Marker3.usercode
		submit()

		Then("I should have three marking workflows on the page")
		className("marking-workflows").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be (3)

		When("I edit marking workflow 1")
		({
			// brackets/braces isolate vals and prevent block being treated as implicit for When() above
			val tbody = className("marking-workflows").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({
				_.findElement(By.tagName("td")).getText == "Marking workflow 1"
			})
			row should be('defined)

			click on row.get.findElement(By.partialLinkText("Modify"))
		})

		Then("I should see the correct fields")
		textField("name").value should be ("Marking workflow 1")
		textField("firstMarkers").value should be (P.Marker1.usercode)

		When("I cancel that edit")
		click on linkText("Cancel")

		And("I delete marking workflow 2")
		({
			val tbody = className("marking-workflows").webElement.findElement(By.tagName("tbody"))
			val row = tbody.findElements(By.tagName("tr")).asScala.find({
				_.findElement(By.tagName("td")).getText == "Marking workflow 2"
			})
			row should be('defined)

			click on row.get.findElement(By.partialLinkText("Delete"))
		})

		Then("I should get a confirmation modal")
		eventually {
			find("marking-workflow-modal") map { _.isDisplayed } should be (Some(true))
		}

		And("It should have a dangerous button")
		eventuallyAjax {
			Option(id("marking-workflow-modal").webElement.findElement(By.className("btn-danger"))) should be ('defined)
		}

		When("I click the dangerous button")
		click on id("marking-workflow-modal").webElement.findElement(By.className("btn-danger"))

		Then("I should have two marking workflows left")
		eventually {
			className("marking-workflows").webElement.findElement(By.tagName("tbody")).findElements(By.tagName("tr")).size should be (2)
		}
	}
}