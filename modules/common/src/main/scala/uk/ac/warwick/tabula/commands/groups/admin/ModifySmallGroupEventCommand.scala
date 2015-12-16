package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.LocalTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import scala.collection.JavaConverters._
import ModifySmallGroupEventCommand._
import uk.ac.warwick.tabula.validators.UsercodeListValidator

object ModifySmallGroupEventCommand {
	type Command = Appliable[SmallGroupEvent] with SelfValidating with BindListener with ModifySmallGroupEventCommandState

	val DefaultStartTime = new LocalTime(12, 0)
	val DefaultEndTime = DefaultStartTime.plusHours(1)

	def create(module: Module, set: SmallGroupSet, group: SmallGroup): Command =
		new CreateSmallGroupEventCommandInternal(module, set, group)
			with ComposableCommand[SmallGroupEvent]
			with CreateSmallGroupEventPermissions
			with CreateSmallGroupEventDescription
			with ModifySmallGroupEventValidation
			with ModifySmallGroupEventBinding
			with ModifySmallGroupEventScheduledNotifications
			with AutowiringSmallGroupServiceComponent

	def edit(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent): Command =
		new EditSmallGroupEventCommandInternal(module, set, group, event)
			with ComposableCommand[SmallGroupEvent]
			with EditSmallGroupEventPermissions
			with EditSmallGroupEventDescription
			with ModifySmallGroupEventValidation
			with ModifySmallGroupEventBinding
			with ModifySmallGroupEventScheduledNotifications
			with AutowiringSmallGroupServiceComponent
}

trait ModifySmallGroupEventCommandState extends CurrentSITSAcademicYear {
	def module: Module
	def set: SmallGroupSet
	def group: SmallGroup
	def existingEvent: Option[SmallGroupEvent]

	var weeks: JSet[JInteger] = JSet()
	var day: DayOfWeek = _
	var startTime: LocalTime = DefaultStartTime
	var endTime: LocalTime = DefaultEndTime
	var location: String = _
	var locationId: String = _
	var title: String = _
	var tutors: JList[String] = JArrayList()

	def weekRanges = Option(weeks) map { weeks => WeekRange.combine(weeks.asScala.toSeq.map { _.intValue }) } getOrElse Seq()
	def weekRanges_=(ranges: Seq[WeekRange]) {
		weeks =
			JHashSet(ranges
				.flatMap { range => range.minWeek to range.maxWeek }
				.map(i => JInteger(Some(i)))
				.toSet)
	}
}

trait CreateSmallGroupEventCommandState extends ModifySmallGroupEventCommandState {
	val existingEvent = None

	def isEmpty = tutors.isEmpty && weekRanges.isEmpty && day == null && startTime == DefaultStartTime && endTime == DefaultEndTime
}

trait EditSmallGroupEventCommandState extends ModifySmallGroupEventCommandState {
	def event: SmallGroupEvent
	def existingEvent = Some(event)
}

class CreateSmallGroupEventCommandInternal(val module: Module, val set: SmallGroupSet, val group: SmallGroup)
	extends ModifySmallGroupEventCommandInternal with CreateSmallGroupEventCommandState {

	self: SmallGroupServiceComponent =>

	copyFromDefaults(set)

	override def applyInternal() = transactional() {
		val event = new SmallGroupEvent(group)
		copyTo(event)
		smallGroupService.saveOrUpdate(event)
		smallGroupService.getOrCreateSmallGroupEventOccurrences(event)
		group.addEvent(event)
		smallGroupService.saveOrUpdate(group)
		event
	}
}

class EditSmallGroupEventCommandInternal(val module: Module, val set: SmallGroupSet, val group: SmallGroup, val event: SmallGroupEvent) extends ModifySmallGroupEventCommandInternal with EditSmallGroupEventCommandState {
	self: SmallGroupServiceComponent =>

	copyFrom(event)

	override def applyInternal() = transactional() {
		copyTo(event)
		smallGroupService.saveOrUpdate(event)
		smallGroupService.getOrCreateSmallGroupEventOccurrences(event)
		event
	}
}

abstract class ModifySmallGroupEventCommandInternal
	extends CommandInternal[SmallGroupEvent] with ModifySmallGroupEventCommandState {

	def copyFromDefaults(set: SmallGroupSet) {
		weekRanges = set.defaultWeekRanges

		Option(set.defaultLocation).foreach {
			case NamedLocation(name) => location = name
			case MapLocation(name, lid) =>
				location = name
				locationId = lid
		}

		if (set.defaultTutors != null) tutors.addAll(set.defaultTutors.knownType.allIncludedIds.asJava)
	}

	def copyFrom(event: SmallGroupEvent) {
		title = event.title

		Option(event.location).foreach {
			case NamedLocation(name) => location = name
			case MapLocation(name, lid) =>
				location = name
				locationId = lid
		}

		weekRanges = event.weekRanges
		day = event.day
		startTime = event.startTime
		endTime = event.endTime

		if (event.tutors != null) tutors.addAll(event.tutors.knownType.allIncludedIds.asJava)
	}

	def copyTo(event: SmallGroupEvent) {
		event.title = title

		// If the location name has changed, but the location ID hasn't, we're changing from a map location
		// to a named location
		Option(event.location).collect { case m: MapLocation => m }.foreach { mapLocation =>
			if (location != mapLocation.name && locationId == mapLocation.locationId) {
				locationId = null
			}
		}

		if (location.hasText) {
			if (locationId.hasText) {
				event.location = MapLocation(location, locationId)
			} else {
				event.location = NamedLocation(location)
			}
		} else {
			event.location = null
		}

		event.weekRanges = weekRanges
		event.day = day
		event.startTime = startTime
		event.endTime = endTime

		if (event.tutors == null) event.tutors = UserGroup.ofUsercodes
		event.tutors.knownType.includedUserIds = tutors.asScala
	}
}

trait ModifySmallGroupEventBinding extends BindListener {
	self: ModifySmallGroupEventCommandState =>

	override def onBind(result: BindingResult) {
		// Find all empty textboxes for tutors and remove them - otherwise we end up with a never ending list of empties
		val indexesToRemove = tutors.asScala.zipWithIndex.flatMap { case (tutor, index) =>
			if (!tutor.hasText) Some(index)
			else None
		}

		// We reverse because removing from the back is better
		indexesToRemove.reverse.foreach { tutors.remove }
	}
}

trait ModifySmallGroupEventValidation extends SelfValidating {
	self: ModifySmallGroupEventCommandState =>

	override def validate(errors: Errors) {
		if (tutors.isEmpty) { // TAB-1278 Allow unscheduled events
			if (weeks == null || weeks.isEmpty) errors.rejectValue("weeks", "smallGroupEvent.weeks.NotEmpty")

			if (day == null) errors.rejectValue("day", "smallGroupEvent.day.NotEmpty")

			if (startTime == null) errors.rejectValue("startTime", "smallGroupEvent.startTime.NotEmpty")

			if (endTime == null) errors.rejectValue("endTime", "smallGroupEvent.endTime.NotEmpty")
		} else {
			val tutorsValidator = new UsercodeListValidator(tutors, "tutors")
			tutorsValidator.validate(errors)
		}

		if (endTime != null && endTime.isBefore(startTime)) errors.rejectValue("endTime", "smallGroupEvent.endTime.beforeStartTime")

		if (location.safeContains("|")) errors.rejectValue("location", "smallGroupEvent.location.invalidChar")
	}
}

trait CreateSmallGroupEventPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateSmallGroupEventCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(group))
	}
}

trait CreateSmallGroupEventDescription extends Describable[SmallGroupEvent] {
	self: CreateSmallGroupEventCommandState =>

	override def describe(d: Description) {
		d.smallGroup(group)
	}

	override def describeResult(d: Description, event: SmallGroupEvent) =
		d.smallGroupEvent(event)
}

trait EditSmallGroupEventPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupEventCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		mustBeLinked(group, set)
		mustBeLinked(event, group)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(event))
	}
}

trait EditSmallGroupEventDescription extends Describable[SmallGroupEvent] {
	self: EditSmallGroupEventCommandState =>

	override def describe(d: Description) {
		d.smallGroupEvent(event)
	}

}

trait ModifySmallGroupEventScheduledNotifications
	extends SchedulesNotifications[SmallGroupEvent, SmallGroupEventOccurrence] with GeneratesNotificationsForSmallGroupEventOccurrence {

	self: SmallGroupServiceComponent =>

	override def transformResult(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence] =
		// get all the occurrences (even the ones in invalid weeks) so they can be cleared
		smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)

	override def scheduledNotifications(occurrence: SmallGroupEventOccurrence): Seq[ScheduledNotification[_ >: Null <: ToEntityReference]] = {
		generateNotifications(occurrence)
	}
}

trait GeneratesNotificationsForSmallGroupEventOccurrence {

	def generateNotifications(occurrence: SmallGroupEventOccurrence): Seq[ScheduledNotification[_ >: Null <: ToEntityReference]] = {
		// Only generate notifications for sets that collect attendance and occurrences that are in valid weeks...
		if (occurrence.event.group.groupSet.collectAttendance && occurrence.event.allWeeks.contains(occurrence.week)) {
			occurrence.dateTime.map(dt =>
				// ... and have a valid date time
				Seq(
					new ScheduledNotification[SmallGroupEventOccurrence](
						"SmallGroupEventAttendanceReminder",
						occurrence,
						dt.withTime(occurrence.event.endTime.getHourOfDay, occurrence.event.endTime.getMinuteOfHour, 0, 0)
					),
					new ScheduledNotification[SmallGroupEventOccurrence](
						"SmallGroupEventAttendanceReminder",
						occurrence,
						dt.plusDays(3)
					),
					new ScheduledNotification[SmallGroupEventOccurrence](
						"SmallGroupEventAttendanceReminder",
						occurrence,
						dt.plusDays(6)
					)
				)
			).getOrElse(Seq())
		} else {
			Seq()
		}
	}
}