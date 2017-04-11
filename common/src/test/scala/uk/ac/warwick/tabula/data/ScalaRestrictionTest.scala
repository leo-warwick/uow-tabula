package uk.ac.warwick.tabula.data

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{Fixtures, AcademicYear, TestBase}
import uk.ac.warwick.tabula.data.ScalaRestriction._


class ScalaRestrictionTest extends TestBase {

	@Test def validateMultipleModuleRestriction: Unit = {

		val mod1 = Fixtures.module("Advanced Foraging", "FO907")
		val mod2 = Fixtures.module("Fungal Forays", "FO306")
		val modules = Seq(mod1, mod2)

		val mrRestriction: Option[ScalaRestriction] = inIfNotEmptyMultipleProperties(
			Seq("moduleRegistration.module", "moduleRegistration.year"),
			Seq(modules, Seq(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))
		)

		val currentAcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		mrRestriction.get.toString should be (s"ScalaRestriction[underlying=(moduleRegistration.module in (Module[advanced foraging], Module[fungal forays]) and moduleRegistration.year in (${currentAcademicYear.toString})),aliases=Map()]")
	}

	@Test def validateNoModuleRestriction: Unit = {

		val modules = Seq()

		val mrRestriction: Option[ScalaRestriction] = inIfNotEmptyMultipleProperties(
			Seq("moduleRegistration.module", "moduleRegistration.year"),
			Seq(modules, Seq(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))
		)

		mrRestriction should be (None)
	}

}
