package uk.ac.warwick.tabula.profiles.profile

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.web.FeaturesDriver

class PersonalTutorTest extends BrowserTest with GivenWhenThen with FeaturesDriver with StudentProfileFixture {

	val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("dd-MMM-yyyy")
	val timeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss")

	"An admin" should "be able to view personal tutor details" in {

		Given("Admin1 is an admin of the student")
		createStaffMember(P.Admin1.usercode, deptCode = TEST_DEPARTMENT_CODE)

		When("Admin1 views the profile of Student1")
		signIn as P.Admin1 to Path(s"/profiles/view/${P.Student1.warwickId}")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		Then("They view the Personal tutor page")
		click on linkText("Personal tutor")
		currentUrl should endWith ("/tutor")

		And("There is no personal tutor")
		pageSource should include ("No personal tutor details found for this course and academic year")

		Given("Student1 has a personal tutor")
		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
		createStudentRelationship(P.Student1, P.Marker1)

		When("Admin1 views the personal tutors of Student1")
		go to Path(s"/profiles/view/${P.Student1.warwickId}/tutor")

		Then("The personal tutor should be shown")
		pageSource should include ("tabula-functest-marker1")
		pageSource should include ("No meeting records exist for this academic year")

	}

	"A personal tutor" should "be able to create a meeting record" in {

		Given("Marker1 is a personal tutor of the student")
		createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
		createStudentRelationship(P.Student1, P.Marker1)

		When("Marker1 views the profile of Student1")
		signIn as P.Marker1 to Path(s"/profiles/view/${P.Student1.warwickId}")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		Then("They view the Personal tutor page")
		click on linkText("Personal tutor")
		currentUrl should endWith ("/tutor")

		And("There is a Record meeting button")
		cssSelector("a.new-meeting-record").findAllElements.size should be (2)

		Then("They create a new record")

		// Modals don't work in HtmlUnit, so screw them
		ifHtmlUnitDriver(
			operation = { d =>
				d.setJavascriptEnabled(false)
				click on linkText("Record meeting")
				d.setJavascriptEnabled(true)
			},
			otherwise = { d =>
				click on linkText("Record meeting")
        eventuallyAjax(find(cssSelector(".modal-body iframe")) should be ('defined))
				switch to frame(find(cssSelector(".modal-body iframe")).get)
				textField(name("title")).isDisplayed should be (true)
			}
		)

		textField("title").value = "Created meeting"
		val datetime = DateTime.now.minusDays(1).withHourOfDay(11)
		textField("meetingDateStr").value = dateFormatter.print(datetime)
		textField("meetingTimeStr").value = timeFormatter.print(datetime)
		textField("meetingEndTimeStr").value = timeFormatter.print(datetime.plusHours(1))
		singleSel("format").value = "f2f"

		switch to defaultContent

		click on cssSelector("button.btn-primary[type=submit]")

		eventually {
			currentUrl should endWith ("/tutor")
		}

		Then("The new record is displayed")
		cssSelector("section.meetings table tbody tr").findAllElements.size should be (1)
		pageSource should include ("Created meeting")

		When("Student1 views their profile")
		signIn as P.Student1 to Path(s"/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		And("They view the Personal tutor page")
		click on linkText("Personal tutor")
		currentUrl should endWith ("/tutor")

		Then("They see a meeting requiring approval")
		pageSource should include ("This record needs your approval. Please review, then approve or return it with comments")

		When("They approve the meeting")
		click on radioButton("approved")
		cssSelector("form.approval button.btn-primary").findElement.get.underlying.click()

		Then("The meeting is approved")
		eventuallyAjax(cssSelector("input[name=approved]").findElement.isEmpty should be (true))
	}

}
