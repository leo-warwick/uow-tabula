package uk.ac.warwick.tabula.attendance.manage

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceViewSchemesTest extends AttendanceFixture with GivenWhenThen {


	"A Member of staff" should "see the schemes for their department" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When(s"I go to /attendance/manage/xxx/$thisAcademicYearString")
		go to Path(s"/attendance/manage/xxx/$thisAcademicYearString")

		Then("I see the schemes in this department")
		pageSource should include("There is 1 monitoring scheme in your department")
		pageSource should include("Untitled scheme")

	}
}
