package uk.ac.warwick.tabula.profiles

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.web.FixturesDriver

trait SubDepartmentFixture  extends BrowserTest with GivenWhenThen with FixturesDriver{
	val TEST_UG_ROUTE_CODE="xx123"
	val TEST_PG_ROUTE_CODE="xp123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_UNDERGRAD_COURSE_CODE="Ux123"
	val TEST_POSTGRAD_COURSE_CODE="Rx123"

	def beforeEverything(){
		Given("A department xxx,a sub-department xxx-ug, and a sub-sub department xxx-ug1 exist")
		And("Admin1 is a departmental admin in xxx")
		And("Admin3 is a departmental admin in xxx-ug")
		And("Admin4 is a departmental admin in xxx-ug1")
		go to Path("/fixtures/setup") // all set up in FixturesCommand
		pageSource should include("Fixture setup successful")

		And("student1 and student2 have a membership record with an undergraduate course")
		createRoute(TEST_UG_ROUTE_CODE, TEST_DEPARTMENT_CODE, "UG Route")
		createCourse(TEST_UNDERGRAD_COURSE_CODE,"Test UG Course")
		createStudentMember(P.Student1.usercode,routeCode=TEST_UG_ROUTE_CODE, courseCode=TEST_UNDERGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE, yearOfStudy = 1)
		createStudentMember(P.Student2.usercode,routeCode=TEST_UG_ROUTE_CODE, courseCode=TEST_UNDERGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE, yearOfStudy = 2)

		And("student3 has a membership record with an undergraduate course")
		createCourse(TEST_POSTGRAD_COURSE_CODE,"Test PG Course")
		createRoute(TEST_PG_ROUTE_CODE, TEST_DEPARTMENT_CODE, "PG Route","PG")
		createStudentMember(P.Student3.usercode,routeCode=TEST_PG_ROUTE_CODE, courseCode=TEST_POSTGRAD_COURSE_CODE,deptCode = TEST_DEPARTMENT_CODE)

		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
	}

}
