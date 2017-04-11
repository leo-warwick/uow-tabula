package uk.ac.warwick.tabula.profiles.profile

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.{FunctionalTestAcademicYear, BrowserTest}
import uk.ac.warwick.tabula.web.FeaturesDriver

class ModulesTest extends BrowserTest with GivenWhenThen with FeaturesDriver with StudentProfileFixture {

	"An admin" should "be able to view personal tutor details" in {

		Given("Admin1 is an admin of the student")
		createStaffMember(P.Admin1.usercode, deptCode = TEST_DEPARTMENT_CODE)

		And("Student1 is registered on modules")
		registerStudentsOnModule(Seq(P.Student1), "xxx01", Some(FunctionalTestAcademicYear.current.startYear.toString))
		registerStudentsOnModule(Seq(P.Student1), "xxx02", Some(FunctionalTestAcademicYear.current.startYear.toString))
		registerStudentsOnModule(Seq(P.Student1), "xxx03", Some(FunctionalTestAcademicYear.current.startYear.toString))

		When("Admin1 views the profile of Student1")
		signIn as P.Admin1 to Path(s"/profiles/view/${P.Student1.warwickId}")
		currentUrl should endWith (s"/profiles/view/${P.Student1.warwickId}")

		And("They view the Modules page")
		click on linkText("Modules")
		currentUrl should endWith ("/modules")

		Then("They see the modules registrations")
		cssSelector("div.striped-section").findAllElements.size should be (3)

	}

}
