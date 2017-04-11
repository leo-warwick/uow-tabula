package uk.ac.warwick.tabula.coursework

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import org.openqa.selenium.By

class CourseworkModuleManagerTest extends BrowserTest with CourseworkFixtures with GivenWhenThen {

	private var lastUsers: Set[String] = Set.empty[String]

	private def changedUsers(implicit currentElement: String): Set[String] = {
		// get the currrently saved set of users with permissions on the role
		val currentUsers = findAll(cssSelector(s"$currentElement .row .very-subtle")).toList.map(u => u.underlying.getText.trim).toSet

		// see what's changed, reset state and return the changes
		val changes = currentUsers.union(lastUsers).filterNot(currentUsers.intersect(lastUsers))
		lastUsers = currentUsers
		changes
	}

	private def getToPermissionsPage() = {
		When("I go the admin page, and expand the module list")
		click on linkText("Go to the Test Services admin page")
		click on linkText("Show")
		findAll(className("module-info")).size should be (3)

		Then("I should be able to click on the Manage button")
		val modInfo = findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == "XXX01").next().underlying
		click on modInfo.findElement(By.partialLinkText("Manage"))

		And("I should see the permissions menu option")
		val managersLink = modInfo.findElement(By.partialLinkText("Edit module permissions"))
		eventually {
			managersLink.isDisplayed should be {true}
		}

		When("I click the permissions link")
		click on managersLink

		Then("I should reach the permissions page")
		currentUrl should include("/permissions")
	}

	def withRoleInElement[T](moduleCode: String, parentElement: String, usersToBeAdded: Seq[String])(fn: => T): T = as(P.Admin1) {
		implicit val currentElement = parentElement

		When("I try to go to the permissions page")
			getToPermissionsPage()

		Then("I should be able to record the initial users with the role")
			changedUsers

		When("I enter a usercode in the tutor picker")
			click on cssSelector(s"$parentElement .pickedUser")
			enter(usersToBeAdded.head)

		Then("I should get a result back")
			val typeahead = cssSelector(".typeahead .active a")
			eventuallyAjax {
				find(typeahead) should not be None
			}

		And("The picker result should match the entry")
			textField(cssSelector(s"$parentElement .pickedUser")).value should be (usersToBeAdded.head)

		When("I pick the matching user")
			click on typeahead

		Then("It should stay in the picker (confirming HTMLUnit hasn't introduced a regression)")
			textField(cssSelector(s"$parentElement .pickedUser")).value should be (usersToBeAdded.head)

		And("The usercode should be injected into the form correctly")
		({
			val user = cssSelector(s"$parentElement .add-permissions [name=usercodes]")
			find(user) should not be (None)
			find(user).get.underlying.getAttribute("value").trim should be (usersToBeAdded.head)
		})

		When("I submit the form")
			find(cssSelector(s"$parentElement form.add-permissions")).get.underlying.submit()

		Then("I should see the new entry")
			withClue(pageSource) { changedUsers should be (Set(usersToBeAdded.head)) }

		When("I add another entry")
		({
			click on cssSelector(s"$parentElement .pickedUser")
			enter(usersToBeAdded.last)
			val typeahead = cssSelector(".typeahead .active a")
			eventuallyAjax {
				find(typeahead) should not be (None)
			}
			click on typeahead
			find(cssSelector(s"$parentElement form.add-permissions")).get.underlying.submit()
		})

		Then("I should see both users")
			changedUsers should be (Set(usersToBeAdded.last))
			lastUsers.size should be (2)

		fn
	}

	"Department admin" should "be able to add module managers" in {
		withRoleInElement("xxx01", ".modulemanager-table", Seq(P.ModuleManager1.usercode, P.ModuleManager2.usercode)) {
			// Nothing to do, the with() tests enough
		}
	}

	"Department admin" should "be able to remove a module manager" in {
		implicit val currentElement = ".modulemanager-table"
		withRoleInElement("xxx01", currentElement, Seq(P.ModuleManager1.usercode, P.ModuleManager2.usercode)) {

			When("I should see at least one user that I can remove")
			changedUsers
			lastUsers.size should be >= 1

			When("I remove the first entry")
			({
				val removable = find(cssSelector(s".modulemanager-table .remove-permissions [name=usercodes][value=${P.ModuleManager1.usercode}]"))
				removable should not be (None)
				removable.get.underlying.submit()
			})

			Then("I should see it's gone")
			changedUsers should be (Set(P.ModuleManager1.usercode))

			And("I should see one left")
			lastUsers.size should be (1)
		}
	}

	"Module manager" should "be able to see only modules they can manage" in {
		withRoleInElement("xxx01", ".modulemanager-table", Seq(P.ModuleManager1.usercode, P.ModuleManager2.usercode)) {
			as(P.ModuleManager1) {
				When("I go to the admin page")
				click on linkText("Go to the Test Services admin page")

				Then("I should only see one of the test modules, as I'm not a departmental admin")
				findAll(className("module-info")).size should be (1)

				When("I click on the Manage button")
				val modInfo = findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == "XXX01").next().underlying
				click on modInfo.findElement(By.partialLinkText("Manage"))

				Then("I should see the permissions menu option")
				val managersLink = modInfo.findElement(By.partialLinkText("Edit module permissions"))
				eventually {
					managersLink.isDisplayed should be {true}
				}
			}
		}
	}


	"Module manager" should "be able to add module assistants" in {
		withRoleInElement("xxx01", ".moduleassistant-table", Seq(P.Marker1.usercode, P.Marker2.usercode)) {
			// Nothing to do, the with() tests enough
		}
	}

	"Module manager" should "be able to remove a module assistant" in {
		implicit val currentElement = ".moduleassistant-table"
		withRoleInElement("xxx01", currentElement, Seq(P.Marker1.usercode, P.Marker2.usercode)) {

			When("I should see at least one user that I can remove")
			changedUsers
			lastUsers.size should be >= 1

			When("I remove the first entry")
			({
				val removable = find(cssSelector(s".moduleassistant-table .remove-permissions [name=usercodes][value=${P.Marker1.usercode}]"))
				removable should not be (None)
				removable.get.underlying.submit()
			})

			Then("I should see it's gone")
			changedUsers should be (Set(P.Marker1.usercode))

			And("I should see one left")
			lastUsers.size should be (1)
		}
	}
}