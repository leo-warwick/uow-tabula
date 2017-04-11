package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BreadcrumbsMatcher
import uk.ac.warwick.tabula.groups.pages.{GroupsHomePage, BatchOpenPage, SmallGroupTeachingPage}

class GroupsHomePageTest extends SmallGroupsFixture with GivenWhenThen with BreadcrumbsMatcher {

	val TEST_MODULE_CODE = "xxx999"
	val TEST_GROUPSET_NAME="Test Tutorial"

	"A student" should "not be able to see unreleased groups" in {
		Given("A small groupset exists with 2 small groups, an allocation method of Manual, but is not released to students")
		createModule("xxx",TEST_MODULE_CODE,"Manually-allocated Module")
		val setId = createSmallGroupSet(
			TEST_MODULE_CODE,
			TEST_GROUPSET_NAME,
			allocationMethodName = "Manual",
			groupCount = 2,
			releasedToStudents = false,
			academicYear = academicYearString
		)

		And("The student is a member of the groupset")
		addStudentToGroupSet(P.Student1.usercode,setId)

		And("The student is allocated to group 1")
		addStudentToGroup(P.Student1.usercode,setId,"Group 1")

		When("I Log in as the student and view the groups page")
		signIn as P.Student1 to Path("/groups/")
		// Ensure correct academic year
		click on linkText(academicYear.toString)

		Then("I should not see the unreleased groupset")
		val groupsPage = new GroupsHomePage
		groupsPage.isCurrentPage

		groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME) should not be 'defined
	}

	"A student" should "be able to see released groups" in {
		Given("A small groupset exists with 2 small groups, an allocation method of Manual, and is released to students")
		createModule("xxx",TEST_MODULE_CODE,"Manually-allocated Module")
		val setId = createSmallGroupSet(TEST_MODULE_CODE,TEST_GROUPSET_NAME,allocationMethodName = "Manual", groupCount=2, releasedToStudents = true, academicYear = "2014")

		And("The student is a member of the groupset")
		addStudentToGroupSet(P.Student1.usercode,setId)

		And("The student is allocated to group 1")
		addStudentToGroup(P.Student1.usercode,setId,"Group 1")

		When("I Log in as the student and view the groups page")
		signIn as P.Student1  to Path("/groups/")
		// Ensure correct academic year
		click on linkText("14/15")

		Then("I should see the released groupset")
		val groupsPage = new GroupsHomePage
		groupsPage.isCurrentPage should be {true}

		groupsPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME) should be ('defined)
	}

	"Department Admin" should "be offered a link to the department's group pages" in {
		Given("the administrator is logged in and viewing the groups home page")
			signIn as P.Admin1  to Path("/groups/")
			pageTitle should be ("Tabula - Small Group Teaching")

	  When("the administrator clicks to view the admin page")
				click on linkText("Go to the Test Services admin page")

		Then("The page should be the small group teaching page")
				currentUrl should include("/groups/admin/department/xxx/")

		// wait for sets to load ajaxically
		eventuallyAjax {
			And("The page should display at least one set")
			findAll(className("set-info")).toList should not be Nil
		}
	}

	it should "be able to open batches of groups" in {
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx", academicYearString)
		val TEST_MODULE_CODE = "xxx999"
		val TEST_GROUPSET_NAME="Test Tutorial"

		Given("The smallGroupTeachingStudentSignUp feature is enabled")
   		enableFeature("smallGroupTeachingStudentSignUp")

		And("There is a a groupset with an allocation method of StudentSignUp")
			createModule("xxx",TEST_MODULE_CODE,"Batch Opening Groupsets test")
		  createSmallGroupSet(
				TEST_MODULE_CODE,
				TEST_GROUPSET_NAME,
				allocationMethodName = "StudentSignUp",
				openForSignups = false,
				academicYear = academicYearString
			)

		And("The administrator is logged in and viewing the groups home page")
		  signIn as P.Admin1  to groupsetSummaryPage.url

		  groupsetSummaryPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME) should be ('defined)
		  val setInfo = groupsetSummaryPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get

		Then("The Bulk Open Groups menu button is enabled")
		  groupsetSummaryPage.getBatchOpenButton should be ('enabled)

		And("The open individual group button is enabled for the specified groupset")
      setInfo.getOpenButton should be ('enabled)

		When("I click the batch open button")
		  groupsetSummaryPage.getBatchOpenButton.click()

		Then("The open page is displayed")
      val batchOpen = new BatchOpenPage("xxx", academicYear)
		  batchOpen.isCurrentPage should be {true}

		When("I check the checkbox next to the groupset")
		  batchOpen.checkboxForGroupSet(setInfo) should be('enabled)
		  batchOpen.checkboxForGroupSet(setInfo).click()

		And("I click 'Open'")
			batchOpen.submit()

		Then("The open page is displayed again")
  		batchOpen should be ('currentPage)

		And("The checkbox to open the groupset is disabled")
  		batchOpen.checkboxForGroupSet(setInfo) should not be 'enabled

		When("I go back to the groups home page")
		val updatedSummary = new SmallGroupTeachingPage("xxx", academicYearString)
		  go to updatedSummary.url

		Then("The option to open the groupset is absent")
			updatedSummary.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get should not be 'showingOpenButton
	}

}
