package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.data.model.MapLocation
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.timetables.TimetableEvent
import uk.ac.warwick.tabula.helpers.StringUtils._

trait TimetableEventToJsonConverter {
	self: ProfileServiceComponent =>

	def jsonTimetableEventObject(event: TimetableEvent) = Map(
		"uid" -> event.uid,
		"name" -> event.name,
		"title" -> event.title,
		"description" -> event.description,
		"eventType" -> event.eventType.displayName,
		"weekRanges" -> event.weekRanges.map { range => Map("minWeek" -> range.minWeek, "maxWeek" -> range.maxWeek) },
		"day" -> event.day.name,
		"startTime" -> event.startTime.toString("HH:mm"),
		"endTime" -> event.endTime.toString("HH:mm"),
		"location" -> (event.location match {
			case Some(l: MapLocation) => Map(
				"name" -> l.name,
				"locationId" -> l.locationId
			)
			case Some(l) => Map("name" -> l.name)
			case _ => null
		}),
		"context" -> event.parent.shortName,
		"parent" -> event.parent,
		"comments" -> event.comments.orNull,
		"staffUniversityIds" -> event.staff.map { _.getWarwickId }.filter { _.hasText },
		"staff" -> event.staff.map { user =>
			Map(
				"universityId" -> user.getWarwickId,
				"firstName" -> user.getFirstName,
				"lastName" -> user.getLastName,
				"email" -> user.getEmail,
				"userType" -> user.getUserType
			)
		},
		"year" -> event.year.toString
	)

}
