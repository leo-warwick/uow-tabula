package uk.ac.warwick.tabula.profiles.profile

import org.joda.time.DateTime
import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.web.FeaturesDriver

import scala.jdk.CollectionConverters._

class PersonalTutorTest extends BrowserTest with GivenWhenThen with FeaturesDriver with StudentProfileFixture {

  "An admin" should "be able to view personal tutor details" in {

    Given("Admin1 is an admin of the student")
    createStaffMember(P.Admin1.usercode, deptCode = TEST_DEPARTMENT_CODE)

    When("Admin1 views the profile of Student1")
    signIn as P.Admin1 to Path(s"/profiles/view/${P.Student1.warwickId}")
    currentUrl should endWith(s"/profiles/view/${P.Student1.warwickId}")

    Then("They view the Personal tutor page")
    click on linkText("Personal tutor")
    currentUrl should endWith("/tutor")

    And("There is no personal tutor")
    pageSource should include("No personal tutor details found for this course and academic year")

    Given("Student1 has a personal tutor")
    createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
    createStudentRelationship(P.Student1, P.Marker1)

    When("Admin1 views the personal tutors of Student1")
    go to Path(s"/profiles/view/${P.Student1.warwickId}/tutor")

    Then("The personal tutor should be shown")
    pageSource should include("tabula-functest-marker1")
    pageSource should include("No meeting records exist for this academic year")

  }

  "A personal tutor" should "be able to create a meeting record" in {

    Given("Marker1 is a personal tutor of the student")
    createStaffMember(P.Marker1.usercode, deptCode = TEST_DEPARTMENT_CODE)
    createStudentRelationship(P.Student1, P.Marker1)

    When("Marker1 views the profile of Student1")
    signIn as P.Marker1 to Path(s"/profiles/view/${P.Student1.warwickId}")
    currentUrl should endWith(s"/profiles/view/${P.Student1.warwickId}")

    Then("They view the Personal tutor page")
    click on linkText("Personal tutor")
    currentUrl should endWith("/tutor")

    And("There is a Record meeting button")
    cssSelector("a.new-meeting-record").findAllElements.size should be(3)

    Then("They create a new record")

    click on linkText("Record meeting")
    eventually(find(cssSelector(".modal-body iframe")) should be('defined))
    switch to frame(find(cssSelector(".modal-body iframe")).get)
    eventually(textField(name("title")).isDisplayed should be(true))

    textField("title").value = "Created meeting"

    val datetime = DateTime.now.minusDays(1).withHourOfDay(11)

    eventually {
      click on textField("meetingDateStr")
      val dateTimePicker = className("datetimepicker").findAllElements.filter(_.isDisplayed).next()

      dateTimePicker.underlying.findElements(By.className("switch")).asScala.filter(_.isDisplayed).head.getText should be(datetime.toString("MMMM yyyy"))

      click on dateTimePicker.underlying.findElement(By.className("datetimepicker-days")).findElements(By.className("day")).asScala.filter { el => el.isDisplayed && el.getText == datetime.toString("d") }.head
    }

    eventually {
      click on textField("meetingTimeStr")
      val dateTimePicker = className("datetimepicker").findAllElements.filter(_.isDisplayed).next()

      click on dateTimePicker.underlying.findElement(By.className("datetimepicker-hours")).findElements(By.className("hour")).asScala.filter { el => el.isDisplayed && el.getText == datetime.toString("H") + ":00" }.head
      click on dateTimePicker.underlying.findElement(By.className("datetimepicker-minutes")).findElements(By.className("minute")).asScala.filter { el => el.isDisplayed && el.getText == datetime.toString("H") + ":00" }.head
    }

    eventually {
      click on textField("meetingEndTimeStr")
      val dateTimePicker = className("datetimepicker").findAllElements.filter(_.isDisplayed).next()

      click on dateTimePicker.underlying.findElement(By.className("datetimepicker-hours")).findElements(By.className("hour")).asScala.filter { el => el.isDisplayed && el.getText == datetime.plusHours(1).toString("H") + ":00" }.head
      click on dateTimePicker.underlying.findElement(By.className("datetimepicker-minutes")).findElements(By.className("minute")).asScala.filter { el => el.isDisplayed && el.getText == datetime.plusHours(1).toString("H") + ":00" }.head
    }

    singleSel("format").value = "f2f"

    switch to defaultContent

    click on cssSelector("button.btn-primary[type=submit]")

    eventually {
      currentUrl should endWith("/tutor")
    }

    Then("The new record is displayed")
    eventually(cssSelector("section.meetings table tbody tr").findAllElements.size should be(1))
    pageSource should include("Created meeting")

    When("Student1 views their profile")
    signIn as P.Student1 to Path(s"/profiles")
    currentUrl should endWith(s"/profiles/view/${P.Student1.warwickId}")

    And("They view the Personal tutor page")
    click on linkText("Personal tutor")
    currentUrl should endWith("/tutor")

    Then("They see a meeting requiring approval")
    pageSource should include("This record needs your approval. Please review, then approve or return it with comments")

    When("They approve the meeting")
    click on radioButton("approved")
    cssSelector("form.approval button.btn-primary").findElement.get.underlying.click()

    Then("The meeting is approved")
    eventually(cssSelector("input[name=approved]").findElement.isEmpty should be(true))
  }

}
