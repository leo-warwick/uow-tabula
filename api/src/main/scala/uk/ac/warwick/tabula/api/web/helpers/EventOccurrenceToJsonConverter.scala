package uk.ac.warwick.tabula.api.web.helpers

import org.joda.time.DateTimeZone
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.{AliasedMapLocation, MapLocation}
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.helpers.StringUtils._

trait EventOccurrenceToJsonConverter {
  self: ProfileServiceComponent =>

  def jsonEventOccurrenceObject(event: EventOccurrence) = Map(
    "uid" -> event.uid,
    "name" -> event.name,
    "title" -> event.title,
    "description" -> event.description,
    "eventType" -> event.eventType.displayName,
    "start" -> DateFormats.IsoDateTime.print(event.start),
    "startDateTime" -> DateFormats.IsoDateTime.print(event.start.toDateTime(DateTimeZone.forID("Europe/London"))),
    "end" -> DateFormats.IsoDateTime.print(event.end),
    "endDateTime" -> DateFormats.IsoDateTime.print(event.end.toDateTime(DateTimeZone.forID("Europe/London"))),
    "location" -> (event.location match {
      case Some(AliasedMapLocation(alias, MapLocation(_, locationId, syllabusPlusName))) => Map(
        "name" -> alias,
        "locationId" -> locationId,
        "syllabusPlusName" -> syllabusPlusName
      )
      case Some(l: MapLocation) => Map(
        "name" -> l.name,
        "locationId" -> l.locationId,
        "syllabusPlusName" -> l.syllabusPlusName
      )
      case Some(l) => Map("name" -> l.name)
      case _ => null
    }),
    "onlineDeliveryUrl" -> event.onlineDeliveryUrl.orNull,
    "deliveryMethod" -> event.deliveryMethod.map(_.entryName).orNull,
    "context" -> event.parent.shortName,
    "parent" -> event.parent,
    "comments" -> event.comments.orNull,
    "staffUniversityIds" -> event.staff.map(_.getWarwickId).filter(_.hasText),
    "staff" -> event.staff.map { user =>
      Map(
        "universityId" -> user.getWarwickId,
        "firstName" -> user.getFirstName,
        "lastName" -> user.getLastName,
        "email" -> user.getEmail,
        "userType" -> user.getUserType
      )
    },
    "sgtGroup" -> Map(
      "sgtGroupId" -> event.sgtGroupId,
      "sgtGroupEventId" -> event.sgtGroupEventId
    )
  )

}
