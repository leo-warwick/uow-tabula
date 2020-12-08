package uk.ac.warwick.tabula.services.timetables

import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.CloseableHttpClient
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

import scala.util.Success

class CelcatTimetableFetchingServiceTest extends TestBase with Mockito {

  val module: Module = Fixtures.module("ib121")

  val httpClient: CloseableHttpClient = mock[CloseableHttpClient]

  val service = new CelcatHttpTimetableFetchingService(new CelcatConfiguration {
    override val wbsConfiguration: CelcatDepartmentConfiguration = CelcatDepartmentConfiguration(
      baseUri = "https://rimmer.wbs.ac.uk:4531",
      credentials = new UsernamePasswordCredentials(Wire.property("username"), "password")
    )
    val cacheEnabled = false
  }) with UserLookupComponent with ProfileServiceComponent with CacheStrategyComponent with LocationFetchingServiceComponent with ModuleAndDepartmentServiceComponent with ApacheHttpClientComponent with FeaturesComponent {
    val userLookup = new MockUserLookup
    val profileService = mock[ProfileService]
    val cacheStrategy = CacheStrategy.CaffeineRequired
    val locationFetchingService: LocationFetchingService = (_: String) => Success(Nil)
    val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
    moduleAndDepartmentService.getModuleByCode(any[String]) answers { moduleCode: Any =>
      Some(Fixtures.module(moduleCode.asInstanceOf[String]))
    }

    override val httpClient: CloseableHttpClient = CelcatTimetableFetchingServiceTest.this.httpClient
    val features = new FeaturesImpl
  }

  @Test def parseJSON(): Unit = {
    val events = service.parseJSON(
      resourceAsString("1503003.json"),
      filterLectures = true
    )
    // 26 events, of which 11 are filtered out - TAB-4754/TAB-7601
    events.events.size should be(15)

    val combined = service.combineIdenticalEvents(events).events.sorted
    combined.size should be(3)

    // Check that the first few events are as expected
    combined.head should be(TimetableEvent(
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
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      TimetableEvent.Parent(Some(module)),
      None,
      Nil,
      Nil,
      AcademicYear.parse("16/17"),
      relatedUrl = None,
      attendance = Map()
    ))

    combined(2) should be(TimetableEvent(
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
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      TimetableEvent.Parent(Some(module)),
      None,
      Nil,
      Nil,
      AcademicYear.parse("16/17"),
      relatedUrl = None,
      attendance = Map()
    ))
  }

  @Test def tab2662(): Unit = {
    val events = service.parseJSON(
      resourceAsString("duplicates.json"),
      filterLectures = true
    )
    events.events.size should be(2)

    val combined = service.combineIdenticalEvents(events).events.sorted
    combined.size should be(1)
    combined.head.weekRanges should be(Seq(WeekRange(5), WeekRange(6)))
  }

}
