package uk.ac.warwick.tabula.attendance.agent

import org.scalatest.GivenWhenThen
import org.openqa.selenium.By
import uk.ac.warwick.tabula.attendance.AttendanceFixture
import org.scalatest.selenium.WebBrowser.go

class AttendanceAgentHomeTest extends AttendanceFixture with GivenWhenThen {

	"A Member of staff who is an agent" should "see their student's attendance" in {
		Given("I am logged in as Marker1")
		signIn as P.Marker1 to Path("/")

		And("Marker 1 is tutor to Student 1")
		createStudentRelationship(P.Student1,P.Marker1)

		When("I go to /attendance/agent/tutor/2013")
		go to Path("/attendance/agent/tutor/2013")

		Then("I see a list of my tutees")
		eventually(currentUrl should include("/attendance/agent/tutor/2013"))
		pageSource should include("My personal tutees")
		className("scrollable-points-table").webElement.findElements(By.cssSelector("tr.student")).size() should be > 0
		className("monitoring-points").webElement.findElements(By.className("point")).size() should be > 0
	}

}
