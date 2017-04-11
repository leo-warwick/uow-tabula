package uk.ac.warwick.tabula.services.timetables

import dispatch.classic.{Credentials, Http}
import org.apache.http.client.HttpClient
import org.joda.time.LocalTime
import org.mockito.Matchers
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.data.model.{Module, NamedLocation}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.CacheStrategyComponent
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.util.cache.Caches.CacheStrategy

class CelcatTimetableFetchingServiceTest extends TestBase with Mockito {

	val module: Module = Fixtures.module("ib121")

	val httpClient: HttpClient = mock[HttpClient]

	val service = new CelcatHttpTimetableFetchingService(new CelcatConfiguration {
		override val wbsConfiguration: CelcatDepartmentConfiguration = CelcatDepartmentConfiguration (
			baseUri = "https://rimmer.wbs.ac.uk",
			credentials = Credentials(Wire.property("username"), "password")
		)
		val cacheEnabled = false
	}) with UserLookupComponent with TermServiceComponent with CacheStrategyComponent with LocationFetchingServiceComponent with ModuleAndDepartmentServiceComponent with DispatchHttpClientComponent {
		val userLookup = new MockUserLookup
		val termService = new TermServiceImpl
		val cacheStrategy = CacheStrategy.InMemoryOnly
		val locationFetchingService = new LocationFetchingService {
			def locationFor(name: String) = NamedLocation(name)
		}
		val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		moduleAndDepartmentService.getModuleByCode(Matchers.any[String]) answers {moduleCode =>
			Some(Fixtures.module(moduleCode.asInstanceOf[String]))
		}

		override val httpClient: Http = new Http {
			override def make_client: HttpClient = CelcatTimetableFetchingServiceTest.this.httpClient
		}
	}

	@Test def parseJSON() {
		val events = service.parseJSON(
			resourceAsString("1503003.json")
		)
		// 24 events, of which 9 are filtered out - TAB-4754
		events.events.size should be (15)

		val combined = service.combineIdenticalEvents(events).events.sorted
		combined.size should be (3)

		// Check that the first few events are as expected
		combined.head should be (TimetableEvent(
			uid = "c16d591a7430197b4a47bea06275b85e",
			name = "IB1210",
			"",
			"",
			TimetableEventType.Lecture,
			Seq(WeekRange(3), WeekRange(7)),
			DayOfWeek.Monday,
			new LocalTime(9, 0),
			new LocalTime(11, 0),
			Some(NamedLocation("WBS M2")),
			TimetableEvent.Parent(Some(module)),
			None,
			Nil,
			Nil,
			AcademicYear.parse("16/17"),
			relatedUrl = None,
			attendance = Map()
		))

		combined(2) should be (TimetableEvent(
			uid = "2db60b5c67d56908f1712746186c9d11",
			name = "IB1210",
			"",
			"",
			TimetableEventType.Seminar,
			Seq(WeekRange(5), WeekRange(8), WeekRange(6), WeekRange(4), WeekRange(9), WeekRange(10)),
			DayOfWeek.Monday,
			new LocalTime(10, 0),
			new LocalTime(11, 0),
			Some(NamedLocation("S0.09")),
			TimetableEvent.Parent(Some(module)),
			None,
			Nil,
			Nil,
			AcademicYear.parse("16/17"),
			relatedUrl = None,
			attendance = Map()
		))
	}

	@Test def tab2662() {
		val events = service.parseJSON(
			resourceAsString("duplicates.json")
		)
		events.events.size should be (2)

		val combined = service.combineIdenticalEvents(events).events.sorted
		combined.size should be (1)
		combined.head.weekRanges should be (Seq(WeekRange(5), WeekRange(6)))
	}

}