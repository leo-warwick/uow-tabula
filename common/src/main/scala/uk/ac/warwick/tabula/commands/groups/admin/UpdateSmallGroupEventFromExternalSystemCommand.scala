package uk.ac.warwick.tabula.commands.groups.admin

import java.util.concurrent.TimeoutException

import org.joda.time.LocalTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.{Location, MapLocation, Module, NamedLocation}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.TimetableEvent

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.util.Try

object UpdateSmallGroupEventFromExternalSystemCommand {

	def apply(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent) =
		new UpdateSmallGroupEventFromExternalSystemCommandInternal(module, set, group, event)
			with ComposableCommand[SmallGroupEvent]
			with UpdateSmallGroupEventFromExternalSystemPermissions
			with UpdateSmallGroupEventFromExternalSystemDescription
			with UpdateSmallGroupEventFromExternalSystemValidation
			with ModifySmallGroupEventScheduledNotifications
			with CommandSmallGroupEventUpdater
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringScientiaTimetableFetchingServiceComponent

}

trait UpdateSmallGroupEventFromExternalSystemRequestState {
	var index: JInteger = _
}

trait UpdateSmallGroupEventFromExternalSystemCommandState extends ImportSmallGroupEventsFromExternalSystemCommandState {
	self: UpdateSmallGroupEventFromExternalSystemRequestState
		with ModuleTimetableFetchingServiceComponent =>

	def group: SmallGroup
	def event: SmallGroupEvent

	lazy val timetableEvents: Seq[TimetableEvent] =
		Try {
			Await.result(timetableFetchingService.getTimetableForModule(module.code.toUpperCase, includeStudents = false), ImportSmallGroupEventsFromExternalSystemCommand.Timeout)
				.events
				.filter(ImportSmallGroupEventsFromExternalSystemCommand.isValidForYear(set.academicYear))
				.sorted
		}.recover {
			case _: TimeoutException | _: TimetableEmptyException => Nil
		}.get

}

class UpdateSmallGroupEventFromExternalSystemCommandInternal(val module: Module, val set: SmallGroupSet, val group: SmallGroup, val event: SmallGroupEvent)
	extends CommandInternal[SmallGroupEvent]
		with UpdateSmallGroupEventFromExternalSystemCommandState
		with UpdateSmallGroupEventFromExternalSystemRequestState {
	self: SmallGroupServiceComponent
		with UserLookupComponent
		with ModuleTimetableFetchingServiceComponent
		with SmallGroupEventUpdater =>

	override def applyInternal(): SmallGroupEvent = transactional() {
		val e = timetableEvents(index)

		val tutorUsercodes = e.staff.map { _.getUserId }

		updateEvent(module, set, group, event, e.weekRanges, e.day, e.startTime, e.endTime, e.location, e.name, tutorUsercodes)

		smallGroupService.saveOrUpdate(event)
		smallGroupService.getOrCreateSmallGroupEventOccurrences(event)
		event
	}

}

trait UpdateSmallGroupEventFromExternalSystemValidation extends SelfValidating {
	self: UpdateSmallGroupEventFromExternalSystemRequestState =>

	override def validate(errors: Errors): Unit = {
		if (index == null) {
			errors.rejectValue("index", "NotEmpty")
		}
	}
}

trait UpdateSmallGroupEventFromExternalSystemPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: UpdateSmallGroupEventFromExternalSystemCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		mustBeLinked(mandatory(set), mandatory(module))
		mustBeLinked(mandatory(group), mandatory(set))
		mustBeLinked(mandatory(event), mandatory(group))
		p.PermissionCheck(Permissions.SmallGroups.Update, event)
	}

}

trait UpdateSmallGroupEventFromExternalSystemDescription extends Describable[SmallGroupEvent] {
	self: UpdateSmallGroupEventFromExternalSystemCommandState =>

	override def describe(d: Description): Unit =
		d.smallGroupEvent(event)

}

trait SmallGroupEventUpdater {
	def updateEvent(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], title: String, tutorUsercodes: Seq[String]): SmallGroupEvent
}

trait CommandSmallGroupEventUpdater extends SmallGroupEventUpdater {
	def updateEvent(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], title: String, tutorUsercodes: Seq[String]): SmallGroupEvent = {
		val command = ModifySmallGroupEventCommand.edit(module, set, group, event)
		command.weekRanges = weeks
		command.day = day
		command.startTime = startTime
		command.endTime = endTime
		location.collect {
			case NamedLocation(name) => command.location = name

			case MapLocation(name, locationId, _) =>
				command.location = name
				command.locationId = locationId
		}

		command.title = title

		// Remove existing tutors if we're overwriting. This means that if we update the tutor in Tabula and there isn't
		// one in S+, we don't overwrite it. If there is one in S+ and then it's overwritten to have no tutor any more,
		// then... sadness?
		if (tutorUsercodes.nonEmpty) command.tutors.clear()
		command.tutors.addAll(tutorUsercodes.asJavaCollection)

		command.apply()
	}
}