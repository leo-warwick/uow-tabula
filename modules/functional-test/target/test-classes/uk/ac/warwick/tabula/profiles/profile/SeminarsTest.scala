package uk.ac.warwick.tabula.profiles.profile

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear}
import uk.ac.warwick.tabula.web.FeaturesDriver

class SeminarsTest extends BrowserTest with GivenWhenThen with FeaturesDriver with StudentProfileFixture {

	"A student" should "be able sign up for small groups on their profile" in {

		Given("Student1 is a member of a small group set")
		val setId = createSmallGroupSet(
			moduleCode = "xxx02",
			groupSetName = "Module 2 Tutorial",
			formatName = "tutorial",
			allocationMethodName = "StudentSignUp",
			academicYear = FunctionalTestAcademicYear.current.startYear.toString
		)
		addStudentToGroupSet(P.Student1.usercode, setId)

		When("Student1 views their profile")
		signIn as P.Student1 to Path("/profiles")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		And("They view the Seminars page")
		click on linkText("Seminars")
		currentUrl should endWith ("/seminars")

		Then("They see the group to sign up")
		cssSelector(s"div.groupset-$setId").findElement.isDefined should be {true}

		When("They choose the group to join")
		click on radioButton("group")
		cssSelector(s"div.groupset-$setId input.sign-up-button").findElement.get.underlying.click()

		Then("They are a member of the group")
		cssSelector(s"div.groupset-$setId input.btn-primary").findElement.get.attribute("value").contains("Leave")

	}

}
