package uk.ac.warwick.tabula.services.timetables

import com.google.common.collect.{Range => GRange}
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.{Cn, Value}
import net.fortuna.ical4j.model.property._
import org.joda.time._
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.AliasedMapLocation
import uk.ac.warwick.tabula.data.model.groups.WeekRange
import uk.ac.warwick.tabula.helpers.DateRange
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

trait EventOccurrenceServiceComponent {
  val eventOccurrenceService: EventOccurrenceService
}

trait TermBasedEventOccurrenceComponent extends EventOccurrenceServiceComponent {
  val eventOccurrenceService: TermBasedEventOccurrenceService
}

trait AutowiringTermBasedEventOccurrenceServiceComponent extends TermBasedEventOccurrenceComponent {
  val eventOccurrenceService: TermBasedEventOccurrenceService = Wire[TermBasedEventOccurrenceService]
}

/**
  * Resolves TimetableEvents into multiple EventOccurrences
  */
trait EventOccurrenceService {
  def fromTimetableEvent(event: TimetableEvent, dateRange: GRange[LocalDate]): Seq[EventOccurrence]

  def toVEvent(eventOccurrence: EventOccurrence): VEvent
}

abstract class TermBasedEventOccurrenceService extends EventOccurrenceService {
  self: ProfileServiceComponent =>

  def fromTimetableEvent(event: TimetableEvent, dateRange: GRange[LocalDate]): Seq[EventOccurrence] = {
    def buildEventOccurrence(week: WeekRange.Week, start: LocalDateTime, end: LocalDateTime, uid: String): EventOccurrence = {
      EventOccurrence(
        uid,
        event.name,
        event.title,
        event.description,
        event.eventType,
        start,
        end,
        event.location,
        event.onlineDeliveryUrl,
        event.deliveryMethod,
        event.parent,
        event.comments,
        event.staff,
        event.relatedUrl,
        event.attendance.get(week),
        event.sgtGroupId,
        event.sgtGroupEventId
      )
    }

    def eventDateToLocalDate(week: WeekRange.Week, localTime: LocalTime): LocalDateTime = {
      val localDateTime: Option[LocalDateTime] =
        event.year.weeks.get(week).map(_.firstDay.withDayOfWeek(event.day.jodaDayOfWeek).toLocalDateTime(localTime))

      // Considered just returning None here, but if we ever encounter an event whose week/day/time
      // specifications can't be converted into calendar dates, we have big problems with
      // data quality and we need to fix them.
      localDateTime.getOrElse(throw new RuntimeException("Unable to obtain a date for " + event))
    }

    val weeks = for {
      weekRange <- event.weekRanges
      week <- weekRange.toWeeks
    } yield week

    val eventsInIntersectingWeeks =
      weeks
        .filter { week =>
          event.year.weeks.get(week).exists { week =>
            dateRange.isConnected(week.dateRange) && !dateRange.intersection(week.dateRange).isEmpty
          }
        }
        .map { week =>
          buildEventOccurrence(
            week,
            eventDateToLocalDate(week, event.startTime),
            eventDateToLocalDate(week, event.endTime),
            s"$week-${event.uid}" // TODO rather than UID swapping here, this should be a recurring event
          )
        }

    // do not remove; import needed for sorting
    // should be: import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
    import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
    eventsInIntersectingWeeks
      .filter(DateRange.inRange(dateRange))
      .sortBy(_.start)
  }

  def toVEvent(eventOccurrence: EventOccurrence): VEvent = {

    var end: DateTime = eventOccurrence.end.toDateTime
    if (eventOccurrence.start.toDateTime.isEqual(end)) {
      end = end.plusMinutes(1)
    }

    val moduleSummary = for (
      module <- Option(eventOccurrence.parent).collect({ case m: TimetableEvent.Module => m });
      sn <- module.shortName;
      fn <- module.fullName
    ) yield s"$sn $fn "
    val summary = eventOccurrence.title.maybeText.getOrElse(eventOccurrence.name)
    val event: VEvent = new VEvent(toDateTime(eventOccurrence.start.toDateTime), toDateTime(end.toDateTime), (moduleSummary.getOrElse("") + summary).safeSubstring(0, 255))
    event.getStartDate.getParameters.add(Value.DATE_TIME)
    event.getEndDate.getParameters.add(Value.DATE_TIME)

    if (eventOccurrence.description.hasText) {
      event.getProperties.add(new Description(eventOccurrence.description))
    }

    (eventOccurrence.location match {
      case Some(AliasedMapLocation(alias: String, _)) => Some(alias)
      case Some(location: model.Location) => Some(location.name)
      case _ => None
    }).foreach(name => event.getProperties.add(new Location(name)))

    eventOccurrence.onlineDeliveryUrl.foreach(url => event.getProperties.add(new Url(new java.net.URI(url))))

    event.getProperties.add(new Uid(eventOccurrence.uid))
    event.getProperties.add(Method.PUBLISH)
    event.getProperties.add(Transp.OPAQUE)

    eventOccurrence.staff.headOption match {
      case Some(user) if user.isFoundUser =>
        val email = if (user.getEmail.hasText) user.getEmail else "no-reply@tabula.warwick.ac.uk"
        val organiser: Organizer = new Organizer(s"MAILTO:$email")
        organiser.getParameters.add(new Cn(user.getFullName))
        event.getProperties.add(organiser)
      case _ =>
        val organiser: Organizer = new Organizer(s"MAILTO:no-reply@tabula.warwick.ac.uk")
        event.getProperties.add(organiser)
    }

    event
  }

  private def toDateTime(dt: DateTime): net.fortuna.ical4j.model.DateTime = {
    val calUTC = new net.fortuna.ical4j.model.DateTime(true)
    calUTC.setTime(dt.toDateTime(DateTimeZone.UTC).getMillis)
    calUTC
  }

}

@Service("termBasedEventOccurrenceService")
class TermBasedEventOccurrenceServiceImpl
  extends TermBasedEventOccurrenceService
    with AutowiringProfileServiceComponent
