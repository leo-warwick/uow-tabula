package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.data.model.{AliasedMapLocation, MapLocation, NamedLocation}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, WeekRange}
import uk.ac.warwick.tabula.helpers.StringUtils._

trait SmallGroupEventToJsonConverter {
  def jsonSmallGroupEventObject(event: SmallGroupEvent): Map[String, Any] = {
    Map(
      "id" -> event.id,
      "title" -> event.title,
      "weeks" -> event.weekRanges.map { case WeekRange(min, max) =>
        Map(
          "minWeek" -> min,
          "maxWeek" -> max
        )
      },
      "day" -> Option(event.day).map(_.name).orNull,
      "startTime" -> Option(event.startTime).map(_.toString("HH:mm")).orNull,
      "endTime" -> Option(event.endTime).map(_.toString("HH:mm")).orNull,
      "location" -> Option(event.location).map {
        case NamedLocation(name) => Map("name" -> name)
        case MapLocation(name, locationId, syllabusPlusName) =>
          Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
        case AliasedMapLocation(name, MapLocation(_, locationId, syllabusPlusName)) =>
          Map("name" -> name, "locationId" -> locationId) ++ syllabusPlusName.map(n => Map("syllabusPlusName" -> n)).getOrElse(Map())
      }.orNull,
      "tutors" -> event.tutors.users.map { user =>
        Seq(
          user.getUserId.maybeText.map("userId" -> _),
          user.getWarwickId.maybeText.map("universityId" -> _)
        ).flatten.toMap
      }
    )
  }
}
