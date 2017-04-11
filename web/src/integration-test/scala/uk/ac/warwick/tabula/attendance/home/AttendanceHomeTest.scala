package uk.ac.warwick.tabula.attendance.home

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceHomeTest extends AttendanceFixture with GivenWhenThen{

	val year: Int = FunctionalTestAcademicYear.current.startYear

	"A student" should "see monitoring points for the current year" in {
		Given("I am logged in as Student1")
		signIn as P.Student1 to Path("/")

		When("I go to /attendance")
		go to Path("/attendance")

		Then("I am redirected to my profile")
		eventually(currentUrl should include(s"/attendance/profile/${P.Student1.warwickId}"))
		pageSource should include("My Monitoring Points")
	}

	"A Member of staff" should "see the monitoring points home page" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When("I go to /attendance")
		go to Path("/attendance")
		click on cssSelector(".navbar-tertiary").webElement.findElement(By.partialLinkText("14/15"))
		click on cssSelector(".navbar-tertiary").webElement.findElement(By.partialLinkText(s"${FunctionalTestAcademicYear.currentSITS.toString}"))

		Then("I see the attendance admin sections")
		currentUrl should endWith(FunctionalTestAcademicYear.currentSITS.startYear.toString)
		pageSource should include("View and record monitoring points")
		pageSource should include("Create and edit monitoring schemes")

		findAll(id(s"view-department-$TEST_DEPARTMENT_CODE")).toList.size should be (1)
		findAll(id(s"manage-department-$TEST_DEPARTMENT_CODE")).toList.size should be (1)
	}

	"A member of staff with a relationship" should "see the monitoring points home page" in {
		Given("I am logged in as Marker1")
		signIn as P.Marker1 to Path("/")

		And("Marker 1 is tutor to Student 1")
		createStudentRelationship(P.Student1, P.Marker1)

		When("I go to /attendance")
		go to Path("/attendance")

		Then("I see the personal tutor section")

		pageSource should include("My students")

		findAll(id(s"relationship-tutor")).toList.size should be (1)

		pageSource should not include "View and record monitoring points"
		pageSource should not include "Create and edit monitoring schemes"

		findAll(id(s"view-department-$TEST_DEPARTMENT_CODE")).toList.size should be (0)
		findAll(id(s"manage-department-$TEST_DEPARTMENT_CODE")).toList.size should be (0)
	}

}
