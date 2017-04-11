package uk.ac.warwick.tabula.services.timetables

import org.joda.time._
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.util.termdates.{TermFactory, TermImpl}

class EventOccurrenceServiceTest extends TestBase with Mockito {

	val module: Module = Fixtures.module("XX-123")
	val week1:WeekRange.Week = 1
	val week2:WeekRange.Week = 2
	// deliberately pick a date that _isn't_ now, so that we can highlight places where we're accidentally
	// guessing the current year instead of reading it from the event
	val week1Start: DateTime = DateTime.now().minusYears(2).withDayOfWeek(DateTimeConstants.MONDAY).withTimeAtStartOfDay()
	val year: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(week1Start)
	val week1end: DateTime = week1Start.plusDays(7)
	val week2Start: DateTime = week1end
	val week2End: DateTime = week2Start.plusDays(7)
	val tenAm = new LocalTime(10,0,0)
	val tenThirty = new LocalTime(10,30,0)

	val singleWeek = Seq(WeekRange(week1,week1))
	val twoWeeks = Seq(WeekRange(week1,week2))

	val intervalIncludingOccurrence = new Interval(week1Start,week1end)
	val intervalIncludingTwoOccurrences = new Interval(week1Start,week2End)


	val singleOccurrence = TimetableEvent("test", "test", "test", "test", TimetableEventType.Lecture, singleWeek, DayOfWeek.Monday, tenAm, tenThirty, None, TimetableEvent.Parent(Some(module)), None, Nil, Nil, year, None, Map())
	val doubleOccurrenence	= TimetableEvent("test", "test", "test", "test", TimetableEventType.Lecture, twoWeeks, DayOfWeek.Monday, tenAm, tenThirty, None, TimetableEvent.Parent(Some(module)), None, Nil, Nil, year, None, Map())



	val intervalOutsideOccurrence = new Interval(1,2)

	val occurrenceService = new TermBasedEventOccurrenceService with WeekToDateConverterComponent with TermServiceComponent with ProfileServiceComponent with UserLookupComponent {
		val weekToDateConverter: WeekToDateConverter = mock[WeekToDateConverter]
		var termService: TermService = mock[TermService]
		val profileService: ProfileService = mock[ProfileService]
		val userLookup = new MockUserLookup
	}

	val termFactory: TermFactory = mock[TermFactory]

	occurrenceService.termService.getTermFromDate(any[DateTime]) returns new TermImpl(termFactory, DateTime.now().minusDays(14), DateTime.now().plusDays(7),TermType.autumn)

	@Test
	def singleOccurenceDuringInterval(){
		occurrenceService.weekToDateConverter.intersectsWeek(intervalIncludingOccurrence,week1,year) returns true
		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(week1Start.plusHours(10).toLocalDateTime)
		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(week1Start.plusHours(10).plusMinutes(30).toLocalDateTime)

		val eo = occurrenceService.fromTimetableEvent(singleOccurrence,intervalIncludingOccurrence)


		eo.size should be (1)

		// verify that the academicYear used to calculate the intersection and the occurrence dates
		// is the year from the event, not the current year
		verify(occurrenceService.weekToDateConverter, times(1)).intersectsWeek(intervalIncludingOccurrence,week1,year)
		verify(occurrenceService.weekToDateConverter, times(1)).toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year)
		verify(occurrenceService.weekToDateConverter, times(1)).toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year)

	}

	@Test
	def singleOccurenceOutsideInterval(){
		occurrenceService.weekToDateConverter.intersectsWeek(intervalOutsideOccurrence,week1,year) returns false
		val eo = occurrenceService.fromTimetableEvent(singleOccurrence,intervalIncludingOccurrence)
		eo.size should be (0)
	}

	@Test
	def singleOccurenceExcludedByMidweekInterval(){

		// make the event end before the requested interval starts
		val eventEndDate = intervalIncludingOccurrence.getStart.minusDays(1).toLocalDateTime

		// ...but the week that the event is in, still intersects with the interval
		occurrenceService.weekToDateConverter.intersectsWeek(intervalIncludingOccurrence,1,year) returns true

		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(new LocalDateTime())
		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(eventEndDate)

		val eo = occurrenceService.fromTimetableEvent(singleOccurrence,intervalIncludingOccurrence)
		eo.size should be (0)
	}

	@Test
	def multipleOccurrencesDuringInterval(){
		occurrenceService.weekToDateConverter.intersectsWeek(intervalIncludingTwoOccurrences,week1,year) returns true
		occurrenceService.weekToDateConverter.intersectsWeek(intervalIncludingTwoOccurrences,week2,year) returns true
		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(week1Start.plusHours(10).toLocalDateTime)
		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(week1Start.plusHours(10).plusMinutes(30).toLocalDateTime)
		occurrenceService.weekToDateConverter.toLocalDatetime(week2,DayOfWeek.Monday,tenAm,year) returns Some(week2Start.plusHours(10).toLocalDateTime)
		occurrenceService.weekToDateConverter.toLocalDatetime(week2,DayOfWeek.Monday,tenThirty,year) returns Some(week2Start.plusHours(10).plusMinutes(30).toLocalDateTime)

		val eo = occurrenceService.fromTimetableEvent(doubleOccurrenence,intervalIncludingTwoOccurrences)

		eo.size should be (2)
	}

	@Test
	def multipleOccurrencesNotAllDuringInterval(){
		occurrenceService.weekToDateConverter.intersectsWeek(intervalIncludingTwoOccurrences,week1,year) returns true
		occurrenceService.weekToDateConverter.intersectsWeek(intervalIncludingTwoOccurrences,week2,year) returns false
		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenAm,year) returns Some(week1Start.plusHours(10).toLocalDateTime)
		occurrenceService.weekToDateConverter.toLocalDatetime(week1,DayOfWeek.Monday,tenThirty,year) returns Some(week1Start.plusHours(10).plusMinutes(30).toLocalDateTime)

		val eo = occurrenceService.fromTimetableEvent(doubleOccurrenence,intervalIncludingTwoOccurrences)

		eo.size should be (1)
	}


}
