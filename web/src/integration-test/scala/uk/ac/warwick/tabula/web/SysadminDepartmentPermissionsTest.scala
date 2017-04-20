package uk.ac.warwick.tabula.web

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest

class SysadminDepartmentPermissionsTest extends BrowserTest with SysadminFixtures with GivenWhenThen {

	def withRoleInElement[T](permittedUser: String, parentElement:String, fixtureAdmins:Seq[String])(fn: => T): T =
		as(P.Sysadmin) { withGodModeEnabled {

			def usercodes = findAll(cssSelector(s"$parentElement .row .very-subtle")).toList.map(_.underlying.getText.trim)

			def normalAdmins() = {
				fixtureAdmins.foreach(usercode=>usercodes should contain (usercode))
			}

			def onlyNormalAdmins() = {
				usercodes.size should be (fixtureAdmins.size)
				normalAdmins()
			}

			When("I go the department listing")
				click on linkText("List all departments in the system")

			Then("I should see the test department")
				val testDeptLink = find(linkText("Test Services"))
				testDeptLink should not be None

			When("I click on the test department")
				click on testDeptLink.get

			Then("I should see a link to admins")
				val adminsLink = find(linkText("View department admins"))
				adminsLink should not be None

			When("I click the permissions link")
				click on adminsLink.get

			Then("I should reach the permissions page")
				currentUrl should include("/permissions")

			And("I should see only the two default admin users")
				onlyNormalAdmins()

			When("I enter a usercode in the picker")
				click on cssSelector(s"$parentElement .pickedUser")
				enter(permittedUser)

			Then("I should get a result back")
				val typeahead = cssSelector(".typeahead .active a")
				eventuallyAjax {
					find(typeahead) should not be None
				}

			And("The picker result should match the entry")
				textField(cssSelector(s"$parentElement .pickedUser")).value should be (permittedUser)

			When("I pick the matching user")
				click on typeahead

			Then("It should stay in the picker (confirming HTMLUnit hasn't introduced a regression)")
				textField(cssSelector(s"$parentElement .pickedUser")).value should be (permittedUser)

			And("The usercode should be injected into the form correctly")
				val injected = find(cssSelector(s"$parentElement .add-permissions [name=usercodes]"))
				injected should not be None
				injected.get.underlying.getAttribute("value").trim should be (permittedUser)

			When("I submit the form")
				find(cssSelector(s"$parentElement form.add-permissions")).get.underlying.submit()

			Then("I should see the old and new entries")
				usercodes.size should be (fixtureAdmins.size + 1)
				normalAdmins()
				usercodes should contain (permittedUser)

			// PhantomJS doesn't support confirm()
			ifPhantomJSDriver { _ =>
				executeScript("window.confirm = function(msg) { return true; };")
			}

			When("I remove the new entry")
				val removable = find(cssSelector(s"$parentElement .remove-permissions [name=usercodes][value=$permittedUser]"))
				removable should not be None
				removable.get.underlying.submit()

			Then("There should only be the normal admins left")
				onlyNormalAdmins()

			fn
		}}

	"Sysadmin" should "be able to add departmental admins" in {
		withRoleInElement(P.Marker1.usercode,".deptadmin-table", fixtureAdmins = Seq(P.Admin2.usercode)) {
			// Nothing more to do, the with...() tests enough
		}
	}

	"God user" should "be able to add user access managers" in {
		withRoleInElement(P.Marker1.usercode,".deptuam-table",fixtureAdmins = Seq(P.Admin1.usercode)) {
			// Nothing more to do, the with...() tests enough
		}
	}

}