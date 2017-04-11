package uk.ac.warwick.tabula.attendance.agent

import org.scalatest.GivenWhenThen
import org.openqa.selenium.By
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceAgentRecordPointTest extends AttendanceFixture with GivenWhenThen {

	 "A Member of staff who is an agent" should "be able to record attendance for a point" in {
		 Given("I am logged in as Marker1")
		 signIn as P.Marker1 to Path("/")

		 And("Marker 1 is tutor to Student 1")
		 createStudentRelationship(P.Student1,P.Marker1)

		 When(s"I go to /attendance/agent/tutor/2013")
		 go to Path(s"/attendance/agent/tutor/2013")

		 Then("I see a list of my tutees")
		 eventually(currentUrl should include("/attendance/agent/tutor/2013"))
		 pageSource should include("My personal tutees")
		 className("monitoring-points").webElement.findElements(By.className("point")).size() should be > 0

		 When("I record the first grouped point")
		 click on className("monitoring-points").webElement.findElement(By.cssSelector("div.point a.btn-primary"))

		 Then("I am redirected to record the grouped point")
		 eventually(currentUrl should(include("/attendance/agent/tutor/2013/point")))
		 pageSource should include("Record attendance")
		 pageSource should include("Point 1")
		 id("recordAttendance").webElement.findElements(By.cssSelector("div.item-info")).size() should be > 0

	 }

 }
