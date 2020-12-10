package uk.ac.warwick.tabula.services.timetables

import org.joda.time._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.DateRange
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

class EventOccurrenceServiceTest extends TestBase with Mockito {

  val module: Module = Fixtures.module("XX-123")
  val week1: WeekRange.Week = 1
  val week2: WeekRange.Week = 2

  // deliberately pick a date that _isn't_ now, so that we can highlight places where we're accidentally
  // guessing the current year instead of reading it from the event
  val year: AcademicYear = AcademicYear(2016)
  val week1Start: LocalDate = year.weeks(1).firstDay
  val week1end: LocalDate = year.weeks(1).lastDay
  val week2Start: LocalDate = year.weeks(2).firstDay
  val week2End: LocalDate = year.weeks(2).lastDay
  val tenAm = new LocalTime(10, 0, 0)
  val tenThirty = new LocalTime(10, 30, 0)

  val singleWeek = Seq(WeekRange(week1))
  val twoWeeks = Seq(WeekRange(week1, week2))

  val intervalIncludingOccurrence = DateRange(week1Start, week1end)
  val intervalIncludingTwoOccurrences = DateRange(week1Start, week2End)

  val singleOccurrence = TimetableEvent("test", "test", "test", "test", TimetableEventType.Lecture, singleWeek, DayOfWeek.Monday, tenAm, tenThirty, None, None, None, TimetableEvent.Parent(Some(module)), None, Nil, Nil, year, None, Map(), None, None)
  val doubleOccurrenence = TimetableEvent("test", "test", "test", "test", TimetableEventType.Lecture, twoWeeks, DayOfWeek.Monday, tenAm, tenThirty, None, None, None, TimetableEvent.Parent(Some(module)), None, Nil, Nil, year, None, Map(), None, None)

  val intervalOutsideOccurrence = DateRange(new LocalDate(1970, DateTimeConstants.JANUARY, 1), new LocalDate(1970, DateTimeConstants.JANUARY, 2))

  val occurrenceService = new TermBasedEventOccurrenceService with ProfileServiceComponent with UserLookupComponent {
    val profileService: ProfileService = mock[ProfileService]
    val userLookup = new MockUserLookup
  }

  @Test
  def singleOccurenceDuringInterval(): Unit = {
    val eo = occurrenceService.fromTimetableEvent(singleOccurrence, intervalIncludingOccurrence)
    eo.size should be(1)
  }

  @Test
  def singleOccurenceOutsideInterval(): Unit = {
    val eo = occurrenceService.fromTimetableEvent(singleOccurrence, intervalOutsideOccurrence)
    eo.size should be(0)
  }

  @Test
  def multipleOccurrencesDuringInterval(): Unit = {
    val eo = occurrenceService.fromTimetableEvent(doubleOccurrenence, intervalIncludingTwoOccurrences)
    eo.size should be(2)
  }

  @Test
  def multipleOccurrencesNotAllDuringInterval(): Unit = {
    val eo = occurrenceService.fromTimetableEvent(doubleOccurrenence, intervalIncludingOccurrence)
    eo.size should be(1)
  }


}
