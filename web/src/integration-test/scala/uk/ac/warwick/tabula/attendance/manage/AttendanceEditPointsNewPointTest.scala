package uk.ac.warwick.tabula.attendance.manage

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceEditPointsNewPointTest extends AttendanceFixture with GivenWhenThen {

	val newPointName = "New point"

	"A Member of staff" should "be able to add a new point to a scheme" in {
		Given("I am logged in as Admin1")
		signIn as P.Admin1 to Path("/")

		When(s"I go to /attendance/manage/xxx/$thisAcademicYearString")
		go to Path(s"/attendance/manage/xxx/$thisAcademicYearString")

		And("I choose to edit the points on a scheme")
		click on linkText("3 points")

		Then("I see the points currently on the scheme")
		eventually(currentUrl should endWith(s"points"))
		pageSource should include("3 points on this scheme")
		cssSelector(".item-info.point").findAllElements.size should be (3)

		When("I choose to add a point")
		click on cssSelector("button.add-blank-point")

		Then("I see the add point screen")
		eventually(pageSource should include("Add monitoring point to scheme"))

		When("I change the point name")
		textField("name").value = newPointName

		And("change the weeks")
		singleSel("startWeek").value = "8"
		singleSel("endWeek").value = "8"

		And("set the type")
		radioButtonGroup("pointType").value = "standard"

		// Stop HTMLUnit screwing up buttons
		ifHtmlUnitDriver(h=>h.setJavascriptEnabled(false))

		And("save the point")
		click on cssSelector("button[name=submit]")

		Then("I am redirected to the scheme and the new point is there")
		eventually(pageSource should include("Edit scheme"))
		pageSource should include(newPointName)
		cssSelector(".item-info.point").findAllElements.size should be (4)

		When("I click done")
		click on linkText("Done")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith(s"/attendance/manage/xxx/$thisAcademicYearString"))
		pageSource should include(s"Manage monitoring points for ${FunctionalTestAcademicYear.current.toString}")
	}
}
