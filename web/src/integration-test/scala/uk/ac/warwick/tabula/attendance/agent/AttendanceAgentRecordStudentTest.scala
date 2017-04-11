package uk.ac.warwick.tabula.attendance.agent

import org.scalatest.GivenWhenThen
import org.openqa.selenium.By
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceAgentRecordStudentTest extends AttendanceFixture with GivenWhenThen {

	 "A Member of staff who is an agent" should "be able to record a single student's attendance" in {
		 Given("I am logged in as Marker1")
		 signIn as P.Marker1 to Path("/")

		 And("Marker 1 is tutor to Student 1")
		 createStudentRelationship(P.Student1,P.Marker1)

		 When(s"I go to /attendance/agent/tutor/2013/${P.Student1.warwickId}/record")
		 go to Path(s"/attendance/agent/tutor/2013/${P.Student1.warwickId}/record")

		 Then("I see the tutee's attendance")
		 eventually(currentUrl should include(s"/attendance/agent/tutor/2013/${P.Student1.warwickId}/record"))
		 pageSource should include("Record attendance")
		 // usercode is set at student's name
		 pageSource should include(s"${P.Student1.usercode}")
		 id("recordAttendance").webElement.findElements(By.className("point")).size() should be (3)

		 And("Submit the form")
		 click on id("recordAttendance")
			 .webElement.findElement(By.cssSelector("input[type=submit]"))

		 Then("I am redirected to the student's attendance view")
		 eventually(currentUrl should include(s"/attendance/agent/tutor/2013/${P.Student1.warwickId}"))

	 }

 }
