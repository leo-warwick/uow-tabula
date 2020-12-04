package uk.ac.warwick.tabula.web.views

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.ac.warwick.tabula.data.model.{AliasedMapLocation, MapLocation}
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter.{Hour12OptionalMins, IncludeDays}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.timetables.{EventOccurrence, RelatedUrl, TimetableEvent}

/**
  * serialises to the JSON which FullCalendar likes.
  *
  * Note: start and end are *Seconds* since the epoch, not milliseconds!
  */
case class FullCalendarEvent(
  title: String,
  fullTitle: String,
  allDay: Boolean,
  start: Long,
  end: Long,
  backgroundColor: String = "#4daacc", // tabulaBlueLight.
  borderColor: String = "#4daacc",
  textColor: String = "#000",
  // fields below here are not used by FullCalendar itself, they're custom fields
  // for use in the renderEvent callback
  formattedStartTime: String,
  formattedEndTime: String,
  formattedInterval: String,
  location: String = "",
  locationId: String = "", // for map links
  syllabusPlusName: Option[String] = None, // for events imported from S+
  onlineDeliveryUrl: Option[String],
  name: String = "",
  description: String = "",
  shorterTitle: String = "", // used in the pop-up to display event details
  tutorNames: String = "",
  parentType: String = "Empty",
  parentShortName: String = "",
  parentFullName: String = "",
  comments: String = "",
  relatedUrl: Option[RelatedUrl] = None,
  attendance: String = ""
)

object FullCalendarEvent {

  def apply(source: EventOccurrence, userLookup: UserLookupService): FullCalendarEvent = {
    val intervalFormatter = new ConfigurableIntervalFormatter(Hour12OptionalMins, IncludeDays)

    val shortTimeFormat = DateTimeFormat.shortTime()

    // Some event providers (namely Celcat) have events scheduled from xx:05 to xx:55.
    // This is fine for displaying the formatted time, but for the seconds time it's neater
    // to roll these times to the hour
    def rollToHour(dt: DateTime) =
      if (dt.getMinuteOfHour == 5) dt.minusMinutes(5)
      else if (dt.getMinuteOfHour == 55) dt.plusMinutes(5)
      else dt

    val startTimeSeconds = rollToHour(source.start.toDateTime).getMillis / 1000
    val endTimeSeconds = rollToHour(source.end.toDateTime).getMillis / 1000

    val parentTitle = source.parent match {
      case m: TimetableEvent.Module => for (sn <- m.shortName; fn <- m.fullName) yield s"$sn $fn"
      case o: TimetableEvent.Parent => o.shortName
    }

    val title =
      Seq(parentTitle, Some(source.eventType.displayName), source.location.map { l => s"(${l.name})" })
        .flatten
        .mkString(" ")

    FullCalendarEvent(
      title = title,
      fullTitle = source.title,
      allDay = false,
      start = startTimeSeconds,
      end = endTimeSeconds,
      formattedStartTime = shortTimeFormat.print(source.start.toDateTime),
      formattedEndTime = shortTimeFormat.print(source.end.toDateTime),
      formattedInterval = intervalFormatter.format(source.start.toDateTime, source.end.toDateTime),
      location = source.location.map(_.name).getOrElse(""),
      locationId = source.location match {
        case Some(MapLocation(_, locationId, _)) => locationId
        case Some(AliasedMapLocation(_, MapLocation(_, locationId, _))) => locationId
        case _ => ""
      },
      syllabusPlusName = source.location.collect { case l: MapLocation => l }.flatMap(_.syllabusPlusName),
      onlineDeliveryUrl = source.onlineDeliveryUrl,
      name = source.name,
      description = source.description,
      shorterTitle = source.parent.shortName.map(_ + " ").getOrElse("") + source.eventType.displayName,
      tutorNames = source.staff.map(_.getFullName).mkString(", "),
      parentType = source.parent match {
        case TimetableEvent.Empty(_, _) => "Empty"
        case TimetableEvent.Department(_, _) => "Department"
        case TimetableEvent.Module(_, _) => "Module"
        case TimetableEvent.Relationship(_, _) => "Relationship"
      },
      parentShortName = source.parent.shortName.getOrElse(""),
      parentFullName = source.parent.fullName.getOrElse(""),
      comments = source.comments.getOrElse(""),
      relatedUrl = source.relatedUrl,
      attendance = source.attendance.map(_.dbValue).getOrElse("")
    )
  }

  def colourEvents(uncoloured: Seq[FullCalendarEvent]): Seq[FullCalendarEvent] = {
    // 30% tints of ID7 brand colours
    val colours = Seq("#cec1d2", "#c5c6c8", "#e0d3b3", "#ffedc2", "#ebc6b6", "#fcd7bc", "#dcb7c0", "#fac6cb", "#d7d7b4", "#d8efe9", "#bccad7", "#b3e8f5")

    // an infinitely repeating stream of colours
    val colourStream = LazyList.continually(colours.to(LazyList)).flatten
    val contexts = uncoloured.map(_.parentShortName).distinct
    val contextsWithColours = contexts.zip(colourStream)
    uncoloured.map { event =>
      if (event.title == "Busy") {
        // FIXME hack
        event.copy(backgroundColor = "#bbb", borderColor = "#bbb")
      } else {
        val colour = contextsWithColours.find(_._1 == event.parentShortName).get._2
        event.copy(backgroundColor = colour, borderColor = colour)
      }
    }
  }
}
