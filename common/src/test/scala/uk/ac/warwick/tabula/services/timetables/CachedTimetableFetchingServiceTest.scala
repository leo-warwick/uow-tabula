package uk.ac.warwick.tabula.services.timetables

import net.spy.memcached.transcoders.SerializingTranscoder
import org.joda.time.LocalTime
import org.junit.Before
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.data.model.{Module, NamedLocation}
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.util.cache.Caches

import scala.concurrent.Future

class CachedTimetableFetchingServiceTest extends TestBase with Mockito {

  private trait Fixture {
    val module: Module = Fixtures.module("cs118")
    val studentId = "studentId"
    val studentEvents: Seq[TimetableEvent] = Seq(TimetableEvent("test", "test", "test", "test", TimetableEventType.Lecture, Nil, DayOfWeek.Monday, new LocalTime, new LocalTime, None, None, None, TimetableEvent.Parent(None), None, Nil, Nil, AcademicYear(2013), None, Map(), None, None))
    val delegate: CompleteTimetableFetchingService = mock[CompleteTimetableFetchingService]

    delegate.getTimetableForStudent(studentId) returns Future.successful(EventList.fresh(studentEvents))

    val cache = new CachedCompleteTimetableFetchingService(delegate, "cacheName")
  }

  @Before def clearCaches(): Unit = {
    Caches.builder("cacheName", Caches.CacheStrategy.CaffeineRequired)
      .maximumSize(10000) // y tho
      .buildStore()
      .clear()
  }

  @Test
  def firstRequestIsPassedThrough(): Unit = new Fixture {
    cache.getTimetableForStudent(studentId).futureValue.events should be(studentEvents)
    verify(delegate, times(1)).getTimetableForStudent(studentId)
  }

  @Test
  def repeatedRequestsAreCached(): Unit = new Fixture {
    cache.getTimetableForStudent(studentId).futureValue.events should be(studentEvents)
    cache.getTimetableForStudent(studentId).futureValue.events should be(studentEvents)
    verify(delegate, times(1)).getTimetableForStudent(studentId)
  }

  @Test
  def keyTypesAreDiscriminated(): Unit = new Fixture {
    // deliberately use the student ID to look up some staff events. The cache key should be the ID + the type of
    // request (staff, student, room, etc) so we should get different results back for student and staff

    val staffEvents = Seq(TimetableEvent("test2", "test2", "test2", "test2", TimetableEventType.Lecture, Nil, DayOfWeek.Monday, new LocalTime, new LocalTime, None, None, None, TimetableEvent.Parent(None), None, Nil, Nil, AcademicYear(2013), None, Map(), None, None))
    delegate.getTimetableForStaff(studentId) returns Future.successful(EventList.fresh(staffEvents))

    cache.getTimetableForStudent(studentId).futureValue.events should be(studentEvents)
    cache.getTimetableForStudent(studentId).futureValue.events should be(studentEvents)
    cache.getTimetableForStaff(studentId).futureValue.events should be(staffEvents)
    cache.getTimetableForStaff(studentId).futureValue.events should be(staffEvents)
    verify(delegate, times(1)).getTimetableForStudent(studentId)
    verify(delegate, times(1)).getTimetableForStaff(studentId)
  }

  @Test
  def serialization(): Unit = {
    val transcoder: SerializingTranscoder = new SerializingTranscoder
    transcoder.encode(TimetableCacheKey.StudentKey("0672089"))
    transcoder.encode(TimetableCacheKey.StudentKey(""))
    transcoder.encode(TimetableCacheKey.ModuleKey("cs118"))
  }

  @Test
  def eventListSerialization(): Unit = {
    val module = Fixtures.module("cs118")
    val transcoder: SerializingTranscoder = new SerializingTranscoder

    val tutor1 = new User("cuscav")
    tutor1.setFoundUser(true)
    tutor1.setWarwickId("0672089")

    val tutor2 = new User("cusebr")
    tutor2.setFoundUser(true)
    tutor2.setWarwickId("0672088")

    val student = new User("student")
    student.setFoundUser(true)
    student.setWarwickId("1234657")

    val events = List(
      TimetableEvent(
        "event 1 uid",
        "event 1 name",
        "event 1 title",
        "event 1 description",
        TimetableEventType.Lecture,
        Seq(WeekRange(1, 10), WeekRange(18)),
        DayOfWeek.Monday,
        new LocalTime(16, 0),
        new LocalTime(17, 0),
        Some(NamedLocation("event 1 location")),
        onlineDeliveryUrl = None,
        deliveryMethod = None,
        TimetableEvent.Parent(Some(module)),
        Some("Comments!"),
        Seq(tutor1, tutor2),
        Seq(student),
        AcademicYear.now(),
        relatedUrl = None,
        attendance = Map(),
        sgtGroupId = None,
        sgtGroupEventId = None
      ),
      TimetableEvent(
        "event 2 uid",
        "event 2 name",
        "event 2 title",
        "event 2 description",
        TimetableEventType.Other("Another type"),
        Seq(WeekRange(1), WeekRange(2)),
        DayOfWeek.Tuesday,
        new LocalTime(10, 0),
        new LocalTime(14, 0),
        None,
        onlineDeliveryUrl = None,
        deliveryMethod = None,
        TimetableEvent.Parent(None),
        None,
        Nil,
        Nil,
        AcademicYear.now(),
        relatedUrl = None,
        attendance = Map(),
        sgtGroupId = None,
        sgtGroupEventId = None
      )
    )

    val cachedData = transcoder.encode(events)
    cachedData should not be null

    transcoder.decode(cachedData) should be(events)
  }

}
