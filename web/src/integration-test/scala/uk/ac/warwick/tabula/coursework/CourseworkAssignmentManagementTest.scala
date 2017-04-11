package uk.ac.warwick.tabula.coursework

import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By
import org.joda.time.DateTime
import org.scalatest.GivenWhenThen

class CourseworkAssignmentManagementTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	"Department admin" should "be able to set up some assignments" in {
		withAssignment("xxx01", "Fully featured assignment") { id =>
			// withAssignment() leads to the dept admin page while logged in as an admin, so we don't need to do any more login

			Given("I can see my new assignment is there")
			id should not be ('empty)

			And("The new assignment looks right")
			click on getAssignmentInfo("xxx01", "Fully featured assignment").findElement(By.partialLinkText("0 submissions"))
			pageSource contains "Fully featured assignment (XXX01)" should be (true)

			When("I go back to the admin page")
			click on linkText("Test Services")

			Then("The edit properties page is now there")
			val assInfo = getAssignmentInfo("xxx01", "Fully featured assignment")
			click on (assInfo.findElement(By.partialLinkText("Actions")))
			val editAssignment = assInfo.findElement(By.partialLinkText("Edit properties"))
			eventually {
				editAssignment.isDisplayed should be (true)
			}
			click on (editAssignment)

			And("The edit properties page opens")
			textField("name").value should be ("Fully featured assignment")

			And("The edit properties page has a delete button")
			linkText("delete").webElement.isDisplayed should be (true)

			And("The edit properties page can be cancelled")
			click on linkText("Cancel")
		}
	}


	"Department admin" should "be able to see pending extension requests on the department page" in {
		withAssignment("xxx02", "Assignment with pending extension requests") { id =>
			// use FixturesDriver for scenario setup to maintain state within the test
			Given("Extensions are allowed at department level")
			updateExtensionSettings("xxx", true, P.ExtensionManager1.usercode)

			And("I have an unapproved extension for student1")
			createExtension(P.Student4.usercode, id, false)

			When("I refresh (to see result of fixtures)")
			webDriver.navigate.refresh

			Then("I will see one outstanding extension")
			val assInfo = getAssignmentInfo("xxx02", "Assignment with pending extension requests")
			assInfo.findElement(By.className("has-unapproved-extensions"))
			assInfo.getText contains "1 extension needs granting" should be (true)
		}
	}


	private def setupUnapprovedExtension(name: String, id: String) = {
		// use FixturesDriver for scenario setup to maintain state within the test
		Given("Extensions are allowed at department level")
		updateExtensionSettings("xxx", true, P.ExtensionManager1.usercode)

		And("I have an unapproved extension for student1")
		createExtension(P.Student4.usercode, id, false)

		When("I edit the assignment")
		val assInfo = getAssignmentInfo("xxx02", name)
		click on (assInfo.findElement(By.partialLinkText("Actions")))
		val editAssignment = assInfo.findElement(By.partialLinkText("Edit properties"))
		eventually {
			editAssignment.isDisplayed should be (true)
		}
		click on (editAssignment)

		Then("I am at the right place")
		textField("name").value should be (name)
	}


	"Department admin" should "be warned when disallowing extensions with requests awaiting review" in {
		val assignmentName = "Assignment should warn before disallowing extensions"
		withAssignment("xxx02", assignmentName) { assignmentId =>
			Given("I have set up an unapproved extension")
			setupUnapprovedExtension(assignmentName, assignmentId)

			When("I disallow extensions")
			disableJQueryAnimationsOnHtmlUnit()
			checkbox("allowExtensions").clear()
			submit()

			Then("A modal should popup")
			val modal = find(cssSelector(".modal.in"))
			eventually {
				modal.isDefined should be (true)
				modal.get.isDisplayed should be (true)
			}

			And("It should show an appropriate warning")
			modal.get.text should include ("1 extension request is awaiting review for this assignment. If you turn off extensions, all extension requests awaiting review will be rejected.")

			And("It should return to the form if cancelled")
			click on cssSelector(".cancel.confirmModal")
			textField("name").value should be (assignmentName)
		}
	}


	"Department admin" should "implicitly deny pending extension requests when disallowing extensions" in {
		val assignmentName = "Assignment should not allow extensions"
		withAssignment("xxx02", assignmentName) { assignmentId =>
			Given("I have set up an unapproved extension")
			setupUnapprovedExtension(assignmentName, assignmentId)

			And("I disallow extensions and await a modal confirmation")
			disableJQueryAnimationsOnHtmlUnit()
			checkbox("allowExtensions").clear()
			submit()

			val modal = find(cssSelector(".modal.in"))
			eventually {
				modal.isDefined should be (true)
				modal.get.isDisplayed should be (true)
			}

			When("I confirm the modal")
			click on cssSelector(".confirm.confirmModal")

			Then("There should be no outstanding extensions")
			val assInfoAgain = getAssignmentInfo("xxx02", assignmentName)
			assInfoAgain.findElements(By.className("has-unapproved-extensions")).size should be (0)
		}
	}


	"Student" should "be able to request extensions" in {
		withAssignment("xxx01", "Assignment for extension") {assignmentId =>
			// use FixturesDriver for scenario setup to maintain state within the test
			Given("Extensions are allowed at department level")
			updateExtensionSettings("xxx", true, P.ExtensionManager1.usercode)

			When("An enrolled student requests an extension")
			requestExtension(P.Student1, "xxx01", "Assignment for extension", assignmentId, new DateTime().plusMonths(1), true)

			Then("The request should be acknowledged")
			pageSource contains "You have requested an extension" should be (true)
		}
	}


	"Department admin" should "be able to archive an assignment" in {
		withAssignment("xxx01", "Fully featured assignment for archiving") { assignmentId =>
			val assInfo = getAssignmentInfo("xxx01", "Fully featured assignment for archiving")

			click on (assInfo.findElement(By.partialLinkText("Actions")))
			val archiveAssignment = assInfo.findElement(By.partialLinkText("Archive assignment"))
			eventually {
				archiveAssignment.isDisplayed should be (true)
			}

			click on (archiveAssignment)

			// Wait for our Ajax popup to load
			eventuallyAjax {
				id("command").webElement.findElement(By.className("btn")).isDisplayed() should be (true)
			}

			click on (id("command").webElement.findElement(By.className("btn")))

			// This works, but it doesn't reload the page automatically properly. Do it manually
			eventuallyAjax {
				find(className("ajax-response")) map { _.underlying.isDisplayed() } should be (Some(true))
			}

			reloadPage

			// Wait for the page reload...
			eventuallyAjax {
				linkText("Show").findElement should be ('defined)
				click on linkText("Show") // Modules with no non-archived assignments are hidden

				val minfo = getModuleInfo("xxx01")
				click on (minfo.findElement(By.partialLinkText("Manage")))

				eventually {
					minfo.findElement(By.partialLinkText("Show archived assignments")).isDisplayed should be (true)
				}

				click on (minfo.findElement(By.partialLinkText("Show archived assignments")))
			}

			getAssignmentInfo("xxx01", "Fully featured assignment for archiving (Archived)").isDisplayed() should be (true)
		}
	}
}