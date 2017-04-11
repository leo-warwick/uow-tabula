package uk.ac.warwick.tabula.services.timetables
import org.joda.time.DateTime
import uk.ac.warwick.tabula.helpers.StoppedClockComponent
import uk.ac.warwick.tabula._



object ScientiaTests {
	class ManualYears() extends PropertiesContext(Map(
		"scientia.base.url" -> "https://test-timetable.example.com/xml",
		"scientia.years" -> "16/17,12/13"
	))

	class AutoYears() extends PropertiesContext(Map(
		"scientia.base.url" -> "https://test-timetable.example.com/xml"
	))
}

class ScientiaTests extends TestBase with Mockito with FunctionalContextTesting {
	import ScientiaTests._

	def createComponent = new AutowiringScientiaConfigurationComponent with StoppedClockComponent {
		override val stoppedTime: DateTime = new DateTime().withDate(2016,2,1)
	}

	@Test def propertyYears = inContext[ManualYears] {
		createComponent.scientiaConfiguration.perYearUris should be (Seq(
			("https://test-timetable.example.com/xml1617/", AcademicYear.parse("16/17")),
			("https://test-timetable.example.com/xml1213/", AcademicYear.parse("12/13"))
		))
	}

	@Test def autoYears = inContext[AutoYears] {
		createComponent.scientiaConfiguration.perYearUris should be (Seq(
			("https://test-timetable.example.com/xml1516/", AcademicYear.parse("15/16"))
		))
	}

}
