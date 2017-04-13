package uk.ac.warwick.tabula.attendance.home

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.attendance.AttendanceFixture
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.web.{FixturesDriver, FeaturesDriver}

class AttendanceDeptViewNoSetsTest  extends BrowserTest with FeaturesDriver with FixturesDriver with GivenWhenThen{

	val TEST_UG_ROUTE_CODE="xx123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_UNDERGRAD_COURSE_CODE="Ux123"

	before {
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")

		createRoute(TEST_UG_ROUTE_CODE, TEST_DEPARTMENT_CODE, "UG Route")
		createCourse(TEST_UNDERGRAD_COURSE_CODE,"Test UG Course")
		createStudentMember(P.Student1.usercode,routeCode=TEST_UG_ROUTE_CODE, courseCode=TEST_UNDERGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE, yearOfStudy = 1)
		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
	}

	"A Member of staff" should "see the department View page with no links" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		And("There are no monitoring point sets")

		When("I go to /attendance/xxx")
		go to Path("/attendance/xxx")

		Then("I see no attendance view links")
		pageSource should not include "View by student"
		pageSource should not include "View by point"
		pageSource should not include "View by personal tutor"
		pageSource should not include "View by supervisor"
	}

}
