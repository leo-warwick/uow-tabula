package uk.ac.warwick.tabula.groups

import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear, LoginDetails}

trait SmallGroupsFixture extends BrowserTest with FixturesDriver with FeaturesDriver {

	val academicYear: FunctionalTestAcademicYear = FunctionalTestAcademicYear.current
	val academicYearString: String = academicYear.startYear.toString

  before{
    go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")
		createSmallGroupSet(
			moduleCode = "xxx01",
			groupSetName = "Test Lab",
			formatName = "lab",
			allocationMethodName = "Manual",
			academicYear = academicYearString
		)
		createSmallGroupSet(
			moduleCode = "xxx02",
			groupSetName = "Module 2 Tutorial",
			formatName = "tutorial",
			allocationMethodName = "StudentSignUp",
			academicYear = academicYearString
		)
  }

  def as[T](user: LoginDetails)(fn: =>T): T = {
		currentUser = user
    signIn as user to Path("/groups/")
    fn
  }



/*	def navigateToEditGroupsetPage(moduleCode:String, groupSetName:String)={
		as(P.Admin1){
			click on linkText("Go to the Test Services admin page")
			breadCrumbsMatch(Seq("Small Group Teaching"))
			goToEditPropertiesOf(moduleCode, groupSetName)

			val content = find(cssSelector("#main-content")).get.underlying
			val heading =content.findElement(By.tagName("h1"))
			heading.getText should startWith ("Create small groups for")
			content
		}}*/
}
