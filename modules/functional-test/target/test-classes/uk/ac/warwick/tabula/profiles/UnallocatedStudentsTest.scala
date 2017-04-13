package uk.ac.warwick.tabula.profiles

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear}

class UnallocatedStudentsTest extends BrowserTest with GivenWhenThen with FeaturesDriver with FixturesDriver {

	val academicYearString: String = FunctionalTestAcademicYear.current.startYear.toString
	val TEST_ROUTE_CODE="xx123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_COURSE_CODE="Ux123"

	before {
		Given("The test department exists")
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")

		createCourse(TEST_COURSE_CODE, "Test UG Course")

		And("5 students have membership records")
		createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "Test Route")
		createCourse(TEST_COURSE_CODE, "Test Course")
		createStudentMember(
			P.Student1.usercode,
			routeCode = TEST_ROUTE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			courseCode = TEST_COURSE_CODE,
			academicYear = academicYearString
		)
		createStudentMember(
			P.Student2.usercode,
			routeCode = TEST_ROUTE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			courseCode = TEST_COURSE_CODE,
			academicYear = academicYearString
		)
		createStudentMember(
			P.Student3.usercode,
			routeCode = TEST_ROUTE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			courseCode = TEST_COURSE_CODE,
			academicYear = academicYearString
		)
		createStudentMember(
			P.Student4.usercode,
			routeCode = TEST_ROUTE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			courseCode = TEST_COURSE_CODE,
			academicYear = academicYearString
		)
		createStudentMember(
			P.Student5.usercode,
			routeCode = TEST_ROUTE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			courseCode = TEST_COURSE_CODE,
			academicYear = academicYearString
		)

		And("Student1 has a relationship")
		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
		createStudentRelationship(P.Student1, P.Marker1)

	}

	"An admin" should "be able to view and allocate students" in {

		When("The departmental administrator goes to the profiles home page")
		signIn as P.Admin1 to Path("/profiles")

		Then("There is a link to administer department xxx")
		find(cssSelector("#profile-dept-admin h5")).get.underlying.getText should be("Test Services")

		When("They view the unallocated students")
		go to Path("/profiles/department/xxx/tutor/missing")

		Then("They see 4 unallocated students")
		cssSelector("form table.related_students tbody tr.student").findAllElements.size should be (4)

		When("They choose 2 students to allocate")
		cssSelector("input[name=preselectStudents]").findAllElements.filter(e =>
			e.attribute("value").get == P.Student2.warwickId || e.attribute("value").get == P.Student4.warwickId
		).foreach(_.underlying.click())
		cssSelector("button.btn-primary").findElement.get.underlying.click()

		Then("They can allocate those 2 students")
		eventually(currentUrl should include("/profiles/department/xxx/tutor/allocate"))
		cssSelector("#allocatestudents-tab1 div.students input[name=allocate]").findAllElements.filter(e =>
			e.attribute("value").get == P.Student2.warwickId || e.attribute("value").get == P.Student4.warwickId
		).forall(_.isSelected) should be {true}

	}

}
