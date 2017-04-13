package uk.ac.warwick.tabula.attendance.manage

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.FunctionalTestAcademicYear
import uk.ac.warwick.tabula.attendance.AttendanceFixture

class AttendanceEditPointsTest extends AttendanceFixture with GivenWhenThen {

	val newPointName = "Renamed point"

	"A Member of staff" should "be able to edit and delete existing points on a scheme" in {


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

		// Stop HTMLUnit screwing up buttons
		ifHtmlUnitDriver(h=>h.setJavascriptEnabled(false))

		When("I choose to edit a point")
		click on linkText("Edit")

		Then("I see the edit point screen")
		eventually(pageSource should include("Edit monitoring point"))

		When("I change the point name")
		textField("name").value = newPointName

		And("change the weeks")
		singleSel("startWeek").value = "8"
		singleSel("endWeek").value = "8"

		And("set the type")
		radioButtonGroup("pointType").value = "standard"

		And("save the point")
		click on cssSelector("button[name=submit]")

		Then("I am redirected to the scheme")
		eventually(pageSource should include("Edit scheme"))
		pageSource should include(newPointName)

		When("I choose to delete a point")
		click on cssSelector("a.btn-danger")

		Then("I see the delete point screen")
		eventually(pageSource should include("Delete monitoring point"))

		When("I click the Delete button")
		click on cssSelector("button.btn-danger")

		Then("I am redirected to the scheme and the point is deleted")
		eventually(pageSource should include("Edit scheme"))
		cssSelector(".item-info.point").findAllElements.size should be (2)

		When("I click done")
		click on linkText("Done")

		Then("I am redirected to the manage home page")
		eventually(currentUrl should endWith(s"/attendance/manage/xxx/$thisAcademicYearString"))
		pageSource should include(s"Manage monitoring points for ${FunctionalTestAcademicYear.current.toString}")
	}
}
