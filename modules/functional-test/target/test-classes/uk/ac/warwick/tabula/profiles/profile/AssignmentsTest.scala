package uk.ac.warwick.tabula.profiles.profile

import org.openqa.selenium.By
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.BrowserTest
import uk.ac.warwick.tabula.coursework.CourseworkFixtures
import uk.ac.warwick.tabula.web.FeaturesDriver

class AssignmentsTest extends BrowserTest with GivenWhenThen with FeaturesDriver with CourseworkFixtures {

	"A student" should "see assignments requiring submission" in {

		Given("There is an assignment requiring a submission")
		withAssignment("xxx01", "Fully featured assignment"){ assignmentId => }

		When("Student1 views their profile")
		signIn as P.Student1 to Path(s"/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		And("Views their assignments")
		click on linkText("Assignments")

		Then("They see an assignment that needs submitting")
		cssSelector("div.striped-section.todo div.row a.btn-primary").findElement.isDefined

	}

	"A student" should "see assignments they have submited" in {

		Given("There is an assignment requiring a submission")
		// Done in the before

		When("Student1 views their profile")
		signIn as P.Student1 to Path(s"/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		And("Views their assignments")
		click on linkText("Assignments")

		Then("They see an assignment that has a submission")
		cssSelector("div.striped-section.doing div.row a.btn-primary").findElement.isDefined

	}

	"A student" should "see assignments that have released feedback" in {

		Given("There is an assignment with feedback released")
		signIn as P.Admin1 to Path("/coursework/admin/department/xxx/#module-xxx02")
		click on linkText("2 submissions and 2 items of feedback")
		click on linkText("Feedback")
		cssSelector("i.icon-share").findElement.get.underlying.findElement(By.xpath("./..")).click()
		click on checkbox("confirm")
		cssSelector("div.submit-buttons input[type=submit]").findElement.get.underlying.click()

		When("Student1 views their profile")
		signIn as P.Student1 to Path(s"/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		And("Views their assignments")
		click on linkText("Assignments")

		Then("They see an assignment that has feedback")
		cssSelector("div.striped-section.done div.row a.btn-primary").findElement.isDefined

	}

}
