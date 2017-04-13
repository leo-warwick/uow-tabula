package uk.ac.warwick.tabula.exams

import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.web.FeaturesDriver
import uk.ac.warwick.tabula.{BrowserTest, LoginDetails}


class ExamFixtures extends BrowserTest with FeaturesDriver with GivenWhenThen {

	before {
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")

		Given("The exams feature is enabled")
		enableFeature("exams")

		And("the exam grids feature is enabled")
		enableFeature("examGrids")
	}

	def as[T](user: LoginDetails)(fn: => T): T = {
		currentUser = user
		signIn as user to Path("/exams/exams")

		fn
	}
}
