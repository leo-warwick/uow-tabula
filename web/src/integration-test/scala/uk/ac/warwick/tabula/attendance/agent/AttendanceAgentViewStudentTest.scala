package uk.ac.warwick.tabula.attendance.agent

import org.scalatest.GivenWhenThen
import org.openqa.selenium.By
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceAgentViewStudentTest extends AttendanceFixture with GivenWhenThen {

	 "A Member of staff who is an agent" should "see a single student's attendance" in {
		 Given("I am logged in as Marker1")
		 signIn as P.Marker1 to Path("/")

		 And("Marker 1 is tutor to Student 1")
		 createStudentRelationship(P.Student1,P.Marker1)

		 When(s"I go to /attendance/agent/tutor/2013/${P.Student1.warwickId}")
		 go to Path(s"/attendance/agent/tutor/2013/${P.Student1.warwickId}")

		 Then("I see the tutee's attendance")
		 eventually(currentUrl should include(s"/attendance/agent/tutor/2013/${P.Student1.warwickId}"))
		 // usercode is set at student's name
		 pageSource should include(s"${P.Student1.usercode}")
		 className("striped-section-contents").webElement.findElements(By.className("point")).size() should be (3)
	 }

 }
