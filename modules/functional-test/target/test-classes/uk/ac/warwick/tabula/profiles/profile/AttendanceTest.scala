package uk.ac.warwick.tabula.profiles.profile

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear}
import uk.ac.warwick.tabula.web.FeaturesDriver

class AttendanceTest extends BrowserTest with GivenWhenThen with FeaturesDriver with StudentProfileFixture {

	"A student" should "be able to view their attendance" in {

		Given("Student1 is a member of a monitoring scheme")
		createAttendanceMonitoringScheme(TEST_DEPARTMENT_CODE, 1, FunctionalTestAcademicYear.current.startYear.toString, P.Student1.warwickId)

		And("Student1 is a member of a small group set")
		val setId = createSmallGroupSet(
			moduleCode = "xxx02",
			groupSetName = "Module 2 Tutorial",
			formatName = "tutorial",
			allocationMethodName = "Manual",
			academicYear = FunctionalTestAcademicYear.current.startYear.toString
		)
		addStudentToGroupSet(P.Student1.usercode, setId)
		addStudentToGroup(P.Student1.usercode, setId, "Group 1")
		createSmallGroupEvent(setId, "Event 1")

		When("Student1 views their profile")
		signIn as P.Student1 to Path("/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		And("They view the Attendance page")
		click on linkText("Attendance")
		currentUrl should endWith ("/attendance")

		Then("They see their monitoring points")
		cssSelector("div.monitoring-points-profile div.striped-section-contents div.term table td.point").findAllElements.size should be (1)

		And("They see their small group attendance")
		cssSelector("div#student-groups-view table#group_attendance_Autumn tbody tr").findAllElements.size should be (1)

	}

}
