package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.groups.pages.SmallGroupTeachingPage

class EditGroupSetPageTest  extends SmallGroupsFixture  with GivenWhenThen{

	"Department Admin" should "be able to view the Edit page for an existing group" in {
		Given("I am logged in as admin")
			signIn as P.Admin1  to Path("/groups/")
		And("I view the small group page for test services")
			val groupsPage = new SmallGroupTeachingPage("xxx", academicYearString)
			go to groupsPage.url

		When("I click on the 'Actions' dropdown for module xxx01/Test Lab")
			val editPage = groupsPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditProperties

		Then("The page is the edit properties page")
			editPage.isCurrentPage("xxx01")

	}

	"Department Admin" should "be able to view and set information visibility options" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx", academicYearString)

		Given("The smallGroupTeachingStudentSignUp feature is enabled")
			enableFeature("smallGroupTeachingStudentSignUp")

		When("I log in as admin")
			signIn as P.Admin1  to Path("/groups/")

		And(" I view the edit properties page for xxx01/Test Lab")
			go to groupsetSummaryPage.url
			val editProperties = groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditProperties

		Then("I should see the options to show/hide tutor name")
			checkbox("studentsCanSeeTutorName") should not be null
			checkbox("studentsCanSeeOtherMembers") should not be null

		When("I check the checkboxes and click Save")
			checkbox("studentsCanSeeTutorName").select()
			checkbox("studentsCanSeeOtherMembers").select()
			editProperties.submitAndExit()

		Then("The page is the groupset summary page")
			groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit properties page again")
			groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditProperties

		Then("The checkboxes should still be checked")
			checkbox("studentsCanSeeTutorName").value should be("true")
			checkbox("studentsCanSeeOtherMembers").value should be("true")

	}

	"Department Admin" should "be able to set a maximum group size" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx", academicYearString)

		When("I log in as admin")
			signIn as P.Admin1  to Path("/groups/")

		And("I view the edit groups page for xxx01/Test Lab")
			go to groupsetSummaryPage.url
			val editGroups = groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditGroups

		Then("I should see the options to set maximum group size")

		When("I set the first group size to be limited, and click Save")
				id("1-maxGroupSize").webElement.clear()
				id("1-maxGroupSize").webElement.sendKeys("12")
				editGroups.submitAndExit()

		Then("The page is the groupset summary page")
			groupsetSummaryPage.isCurrentPage should be {true}

		When("I navigate to the edit groups page again")
		  groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditGroups

		And("The first group value should have been saved")
			id("1-maxGroupSize").webElement.getAttribute("value") should be ("12")

		When("I set group size to be unlimited")
		id("1-maxGroupSize").webElement.clear()
		editGroups.submitAndExit()

		Then("The page is the groupset summary page")
			groupsetSummaryPage should be('currentPage)

		When("I navigate to the edit groups page again")
			groupsetSummaryPage.getGroupsetInfo("xxx01", "Test Lab").get.goToEditGroups

		And("The group value should be unlimited")
			id("1-maxGroupSize").webElement.getAttribute("value") should be ("")

	}

}