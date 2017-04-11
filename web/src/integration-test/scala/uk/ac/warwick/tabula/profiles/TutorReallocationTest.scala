package uk.ac.warwick.tabula.profiles

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear}

class TutorReallocationTest extends BrowserTest with GivenWhenThen with FeaturesDriver with FixturesDriver {

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

		And("Student2 has a relationship")
		createStaffMember(P.Marker2.usercode, deptCode = TEST_DEPARTMENT_CODE)
		createStudentRelationship(P.Student2, P.Marker2)

	}

	"An admin" should "be able to view allocated students and reallocate them" in {

		When("The departmental administrator goes to the profiles home page")
		signIn as P.Admin1 to Path("/profiles")

		Then("There is a link to administer department xxx")
		find(cssSelector("#profile-dept-admin h5")).get.underlying.getText should be("Test Services")

		When("They view the unallocated students")
		go to Path("/profiles/department/xxx/tutor")

		Then("They see 2 tutors with 1 tutee each")
		cssSelector(".striped-section").findAllElements.size should be (2)
		cssSelector(".striped-section table.related_students tr.student").findAllElements.size should be (2)

		When("They select a student to reallocate")
		click on cssSelector(s"#rel-user${P.Marker2.warwickId}-students-title")
		cssSelector("input[name=preselectStudents]").findAllElements.filter(e =>
			e.attribute("value").get == P.Student2.warwickId
		).foreach(_.underlying.click())
		cssSelector(s"#rel-user${P.Marker2.warwickId}-students button.btn-primary").findElement.get.underlying.click()

		Then("They can allocate that student to a new tutor")
		eventually(currentUrl should include("/profiles/department/xxx/tutor/reallocate"))
		cssSelector(".students table tbody tr").findAllElements.size should be (1)
		cssSelector(".students table tbody tr td.check input").findElement.get.isSelected should be {true}
		cssSelector(".entities table tbody tr").findAllElements.size should be (1)
		cssSelector(s".entities table tbody td.full-name").findElement.get.underlying.getText should be (s"${P.Marker1.usercode} user")
		cssSelector(s".entities table tbody td.counter").findElement.get.underlying.getText should be ("1")

	}

}
