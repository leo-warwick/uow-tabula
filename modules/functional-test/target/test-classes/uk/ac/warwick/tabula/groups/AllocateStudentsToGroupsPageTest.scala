package uk.ac.warwick.tabula.groups

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.groups.pages.SmallGroupTeachingPage

class AllocateStudentsToGroupsPageTest extends SmallGroupsFixture with GivenWhenThen {

	val TEST_MODULE_CODE = "xxx987"
	val TEST_GROUPSET_NAME = "Allocation Test Groupset"

	"Department admin" should "Be able to filter students" in {
		Given("A small group set exists with 1 small group")
		createModule("xxx", TEST_MODULE_CODE, "Allocation Module")
		val setId = createSmallGroupSet(TEST_MODULE_CODE, TEST_GROUPSET_NAME, academicYear = academicYearString)

		And("Two routes exist")
		createCourse("Ux123","AllocCourse")
		createRoute("xx123", "xxx", "Allocation Test Route")
		createRoute("xx456", "xxx", "Allocation Test Route 2")

		And("Five students, of varying gender, year and route, are members of the groupset")

		for ((studentId, gender, year, route) <- Seq(
			(P.Student1.usercode, "F", 1, "xx123"),
			(P.Student2.usercode, "M", 1, "xx123"),
			(P.Student3.usercode, "F", 1, "xx456"),
			(P.Student4.usercode, "M", 2, "xx123"),
			(P.Student5.usercode, "F", 3, "xx123"))) {
			createStudentMember(studentId, gender, route, year,"Ux123",academicYear = academicYearString)
			addStudentToGroupSet(studentId, setId)
		}

		And("The smallGroupAllocationFiltering feature flag is set")
		enableFeature("smallGroupAllocationFiltering")

		When("I log in as a departmental administrator and visit the department groups page")
		signIn as P.Admin1 to Path("/groups/")

		And(" I view the 'allocate students' page for xxx987/Allocation Test Groupset")
		val groupsetSummaryPage = new SmallGroupTeachingPage("xxx", academicYearString)
		go to groupsetSummaryPage.url
		val allocatePage = groupsetSummaryPage.getGroupsetInfo(TEST_MODULE_CODE, TEST_GROUPSET_NAME).get.goToAllocate

		Then("I can see the list of students with all 5 students visible")

		allocatePage.findAllUnallocatedStudents.count(_.underlying.isDisplayed) should be(5)
		And("I can see the dropdowns to filter by gender, year, and course")

		// the select options are determined by the test data we created when we set up the users
		allocatePage.findFilterDropdown("fGender") should be('defined)
		allocatePage.findFilterDropdown("fGender").get.getOptions.size() should be(3) //M,F, All

		allocatePage.findFilterDropdown("fYear") should be('defined)
		allocatePage.findFilterDropdown("fYear").get.getOptions.size() should be(4) //1,2,3, All

		allocatePage.findFilterDropdown("fRoute") should be('defined)
		allocatePage.findFilterDropdown("fRoute").get.getOptions.size() should be(3) //xx123,xx456 All

		When("I select the 'Female' option")
		val genderSelect = allocatePage.findFilterDropdown("fGender").get
		genderSelect.selectByVisibleText("Female")

		Then("Only 3 students should be shown")
		allocatePage.findAllUnallocatedStudents.count(_.underlying.isDisplayed) should be(3)

		When("I select the 'year 3' option")
		val yearSelect = allocatePage.findFilterDropdown("fYear").get
		yearSelect.selectByVisibleText("Year 3")

		Then("Only 1 student1 should be shown")
		allocatePage.findAllUnallocatedStudents.count(_.underlying.isDisplayed) should be(1)

	}
}
