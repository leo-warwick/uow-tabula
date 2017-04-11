package uk.ac.warwick.tabula.admin

import uk.ac.warwick.tabula.{LoginDetails, BrowserTest}
import org.openqa.selenium.By
import org.scalatest.GivenWhenThen

// n.b. this test doesn't work with the FirefoxDriver because the UI pops up modal dialogs which the
// test isn't expecting. HTMLUnit ignores them.
class  DepartmentPermissionsTest extends BrowserTest with AdminFixtures with GivenWhenThen {

	private def usercodes(parentElement: String) = findAll(cssSelector(s"$parentElement .row .very-subtle")).toList.map(_.underlying.getText.trim)

	private def noNewUsersListed(parentElement: String, expectedCount: Int) {
		usercodes(parentElement).size should be (expectedCount)
	}

	private def nowhereElse(parentElement: String) = {
		// doesn't like CSS :not() selector, so have to get all permission-lists and filter out the current one by scala text-mungery
		val allLists = findAll(cssSelector("#tutors-supervisors-row .permission-list")).toList.filterNot(_.underlying.getAttribute("class").contains(parentElement.replace(".","")))
		// then delve further to get the usercodes included
		val filteredUsercodes = allLists map (list => list.underlying.findElements(By.cssSelector(".row .very-subtle")))
		filteredUsercodes.foreach(_.size should be(0))
	}

	private def gotoPermissionsScreen(parentElement: String, preExistingCount: Int) {
		When("I go the admin page")
		if (!currentUrl.contains("/department/xxx")) {
			click on linkText("Go to the Test Services admin page")
		}

		Then("I should be able to click on the Manage button")
		val toolbar = findAll(className("dept-toolbar")).next().underlying
		click on toolbar.findElement(By.partialLinkText("Manage"))

		And("I should see the permissions menu option")
		val managersLink = toolbar.findElement(By.partialLinkText("Edit departmental permissions"))
		eventually {
			managersLink.isDisplayed should be {true}
		}

		When("I click the permissions link")
		click on managersLink

		Then("I should reach the permissions page")
		currentUrl should include("/permissions")

		And("I should see no users with the role")
		noNewUsersListed(parentElement, preExistingCount)

		And("I should not see anyone else with any other roles")
		nowhereElse(parentElement)
	}

	private def gotoPermissionsScreenAndPickUser(parentElement: String, permittedUser: LoginDetails, preExistingCount: Int) {
		gotoPermissionsScreen(parentElement, preExistingCount)

		When("I enter a usercode in the picker")
		click on cssSelector(s"$parentElement .pickedUser")
		enter(permittedUser.usercode)

		Then("I should get a result back")
		val typeahead = cssSelector(".typeahead .active a")
		eventuallyAjax {
			find(typeahead) should not be None
		}

		And("The picker result should match the entry")
		textField(cssSelector(s"$parentElement .pickedUser")).value should be (permittedUser.usercode)

		When("I pick the matching user")
		click on typeahead

		Then("It should stay in the picker (confirming HTMLUnit hasn't introduced a regression)")
		textField(cssSelector(s"$parentElement .pickedUser")).value should be (permittedUser.usercode)

		And("The usercode should be injected into the form correctly")
		val injected = find(cssSelector(s"$parentElement .add-permissions [name=usercodes]"))
		injected should not be None
		injected.get.underlying.getAttribute("value").trim should be (permittedUser.usercode)
	}

	def addAndRemoveWithRoleInElement[T](performingUser: LoginDetails, permittedUser: LoginDetails, parentElement: String, preExistingCount: Int = 0)(fn: => T): T =
		as(performingUser) {
			gotoPermissionsScreenAndPickUser(parentElement, permittedUser, preExistingCount)

			When("I submit the form")
			find(cssSelector(s"$parentElement form.add-permissions")).get.underlying.submit()

			Then("I should see the new entry")
			({
				usercodes(parentElement).size should be (preExistingCount+1)
				usercodes(parentElement) should contain (permittedUser.usercode)
			})

			And("I should not see anyone else with any other roles")
			nowhereElse(parentElement)

			When("I remove the new entry")
			val removable = find(cssSelector(s"$parentElement .remove-permissions [name=usercodes][value=${permittedUser.usercode}]"))
			removable should not be None
			removable.get.underlying.submit()

			Then("There should be no users listed")
			noNewUsersListed(parentElement, preExistingCount)

			And("I should not see anyone else with any other roles")
			nowhereElse(parentElement)

			fn
		}

	def permissionDeniedWithRoleInElement[T](performingUser: LoginDetails, parentElement: String, preExistingCount:Int = 0)(fn: => T): T =
		as(performingUser) {
			gotoPermissionsScreen(parentElement, preExistingCount)

			Then("The button should be disabled")
			find(cssSelector(s"$parentElement button")).get.underlying.getAttribute("class").indexOf("disabled") should not be (-1)

			fn
		}

	"User Access Manager" should "be able to add and remove departmental admins" in {
		addAndRemoveWithRoleInElement(P.Admin1, P.Marker1, ".admin-table", 1) {
			// Nothing more to do, the with...() tests enough
		}
	}

	"User Access Manager" should "be able to add and remove senior tutors" in {
		addAndRemoveWithRoleInElement(P.Admin1, P.Marker1, ".tutor-table") {
			// Nothing more to do, the with...() tests enough
		}
	}

	"User Access Manager" should "be able to add and remove senior supervisors" in {
		addAndRemoveWithRoleInElement(P.Admin1, P.Marker1, ".supervisor-table") {
			// Nothing more to do, the with...() tests enough
		}
	}

	"Departmental Admin" should "be able to add and remove departmental admins" in {
		addAndRemoveWithRoleInElement(P.Admin2, P.Marker1, ".admin-table", 1) {
			// Nothing more to do, the with...() tests enough
		}
	}

	"Departmental Admin" should "not be able to add and remove senior tutors" in {
		permissionDeniedWithRoleInElement(P.Admin2, ".tutor-table") {
			// Nothing more to do, the with...() tests enough
		}
	}

	"Departmental Admin" should "not be able to add and remove senior supervisors" in {
		permissionDeniedWithRoleInElement(P.Admin2, ".supervisor-table") {
			// Nothing more to do, the with...() tests enough
		}
	}

	"Random module manager" should "not be able to add senior supervisors or tutors" in {
		as(P.ModuleManager1) {
			// no link
			findAll(linkText("Go to the Test Services admin page")).size should be (0)

			// no direct access
			go to Path("/admin/department/xxx/permissions")
			pageSource.contains(s"Sorry ${P.ModuleManager1.usercode}, you don't have permission to see that.") should be {true}
		}

	}
}