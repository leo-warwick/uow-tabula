package uk.ac.warwick.tabula.attendance.home

import org.scalatest.GivenWhenThen
import org.openqa.selenium.By
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceViewPointsTest extends AttendanceFixture with GivenWhenThen{

	 "A Member of staff" should "see the View Students page" in {
		 Given("I am logged in as Admin1")
		 signIn as P.Admin1 to Path("/")

		 When(s"I go to /attendance/view/xxx/$thisAcademicYearString/points")
		 go to Path(s"/attendance/view/xxx/$thisAcademicYearString/points")

		 Then("I see the page, but no points")

		 pageSource should include("View monitoring points")
		 className("monitoring-points").findElement should be (None)

		 When("I filter only Undergraduate")
		 click on id("filterCommand").webElement.findElement(By.className("filter-short-values"))
		 cssSelector(".dropdown-menu.filter-list").findElement.get.isDisplayed should be (true)
		 click on cssSelector("input[name=courseTypes][value=UG]")

		 Then("The points are displayed")
		 eventuallyAjax{
			 id("filter-results").webElement.findElements(By.cssSelector(".monitoring-points .item-info.point")).size should be > 0
		 }

		 /* For some reason HtmlUnit is blanking the href for each of the Record buttons so the rest of the test fails
		 When("I choose to record the first point")
		 click on id("filter-results").webElement.findElement(By.cssSelector(".monitoring-points .item-info.point a.btn-primary"))

		 Then("I am redirected to record the grouped point")
		 eventually(currentUrl should(include("/attendance/view/xxx/points") and include("record")))
		 pageSource should include("Record attendance")
		 pageSource should include("Point 1")
		 id("recordAttendance").webElement.findElements(By.cssSelector("div.item-info")).size() should be > 0
		 */
	 }

 }
