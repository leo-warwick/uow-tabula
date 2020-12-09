package uk.ac.warwick.tabula.commands.groups.admin

import org.apache.commons.validator.routines.UrlValidator
import org.joda.time.LocalTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethod._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.WAI2GoLocation
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator

import scala.jdk.CollectionConverters._

object ModifySmallGroupEventCommand {
  type Command = Appliable[SmallGroupEvent] with SelfValidating with BindListener with ModifySmallGroupEventCommandState

  def create(module: Module, set: SmallGroupSet, group: SmallGroup, isImport: Boolean = false): Command =
    new CreateSmallGroupEventCommandInternal(module, set, group, isImport)
      with ComposableCommand[SmallGroupEvent]
      with CreateSmallGroupEventPermissions
      with CreateSmallGroupEventDescription
      with ModifySmallGroupEventValidation
      with ModifySmallGroupEventBinding
      with ModifySmallGroupEventScheduledNotifications
      with AutowiringSmallGroupServiceComponent

  def edit(module: Module, set: SmallGroupSet, group: SmallGroup, event: SmallGroupEvent, isImport: Boolean = false): Command =
    new EditSmallGroupEventCommandInternal(module, set, group, event, isImport)
      with ComposableCommand[SmallGroupEvent]
      with EditSmallGroupEventPermissions
      with EditSmallGroupEventDescription
      with ModifySmallGroupEventValidation
      with ModifySmallGroupEventBinding
      with ModifySmallGroupEventScheduledNotifications
      with AutowiringSmallGroupServiceComponent
}

trait ModifySmallGroupEventCommandState {
  def module: Module

  def set: SmallGroupSet

  def group: SmallGroup

  def existingEvent: Option[SmallGroupEvent]

  def isImport: Boolean // true if this isn't a UI action

  def academicYear: AcademicYear = set.academicYear

  var weeks: JSet[JInteger] = JSet()
  var day: DayOfWeek = _
  var startTime: LocalTime = _
  var endTime: LocalTime = _
  var location: String = _
  var locationId: String = _
  var title: String = _
  var tutors: JList[String] = JArrayList()
  var relatedUrl: String = _
  var relatedUrlTitle: String = _
  var useNamedLocation: Boolean = _
  var locationAlias: String = _
  var possibleMapLocations: Seq[WAI2GoLocation] = Nil

  var deliveryMethod: EventDeliveryMethod = _
  var onlineDeliveryUrl: String = _
  var onlinePlatform: OnlinePlatform = _

  def weekRanges: Seq[WeekRange] = Option(weeks) map { weeks => WeekRange.combine(weeks.asScala.toSeq.map(_.intValue))
  } getOrElse Seq()

  def weekRanges_=(ranges: Seq[WeekRange]): Unit = {
    weeks =
      JHashSet(ranges
        .flatMap { range => range.minWeek to range.maxWeek }
        .map(i => JInteger(Some(i)))
        .toSet)
  }
}

trait CreateSmallGroupEventCommandState extends ModifySmallGroupEventCommandState {
  val existingEvent: Option[SmallGroupEvent] = None

  def isEmpty: Boolean = tutors.isEmpty && weekRanges.isEmpty && day == null && startTime == null && endTime == null
}

trait EditSmallGroupEventCommandState extends ModifySmallGroupEventCommandState {
  def event: SmallGroupEvent

  def existingEvent = Some(event)
}

class CreateSmallGroupEventCommandInternal(val module: Module, var set: SmallGroupSet, var group: SmallGroup, val isImport: Boolean)
  extends ModifySmallGroupEventCommandInternal with CreateSmallGroupEventCommandState {

  self: SmallGroupServiceComponent =>

  copyFromDefaults(set)

  override def applyInternal(): SmallGroupEvent = transactional() {
    val event = new SmallGroupEvent(group)
    copyTo(event)
    smallGroupService.saveOrUpdate(event)
    smallGroupService.getOrCreateSmallGroupEventOccurrences(event)
    group.addEvent(event)
    smallGroupService.saveOrUpdate(group)
    event
  }
}

class EditSmallGroupEventCommandInternal(val module: Module, val set: SmallGroupSet, val group: SmallGroup, val event: SmallGroupEvent, val isImport: Boolean)
  extends ModifySmallGroupEventCommandInternal with EditSmallGroupEventCommandState {

  self: SmallGroupServiceComponent =>

  copyFrom(event)

  override def applyInternal(): SmallGroupEvent = transactional() {
    copyTo(event)
    smallGroupService.saveOrUpdate(event)
    smallGroupService.getOrCreateSmallGroupEventOccurrences(event)
    event
  }
}

abstract class ModifySmallGroupEventCommandInternal
  extends CommandInternal[SmallGroupEvent] with ModifySmallGroupEventCommandState {

  def copyFromDefaults(set: SmallGroupSet): Unit = {
    weekRanges = set.defaultWeekRanges
    day = set.defaultDay
    startTime = set.defaultStartTime
    endTime = set.defaultEndTime

    Option(set.defaultLocation).foreach {
      case NamedLocation(name) => location = name
      case MapLocation(name, lid, _) =>
        location = name
        locationId = lid
      case AliasedMapLocation(displayName, MapLocation(name, lid, _)) =>
        location = name
        locationId = lid
        locationAlias = displayName
    }

    if (set.defaultTutors != null) tutors.addAll(set.defaultTutors.knownType.allIncludedIds.asJava)
    if (set.defaultDeliveryMethod != null) deliveryMethod = set.defaultDeliveryMethod
    if (set.defaultOnlineDeliveryUrl != null) onlineDeliveryUrl = set.defaultOnlineDeliveryUrl
    if (set.defaultOnlinePlatform != null) onlinePlatform = set.defaultOnlinePlatform
  }

  def copyFrom(event: SmallGroupEvent): Unit = {
    title = event.title

    Option(event.location).foreach {
      case NamedLocation(name) => location = name
      case MapLocation(name, lid, _) =>
        location = name
        locationId = lid
      case AliasedMapLocation(displayName, MapLocation(name, lid, _)) =>
        location = name
        locationId = lid
        locationAlias = displayName
    }

    weekRanges = event.weekRanges
    day = event.day
    startTime = event.startTime
    endTime = event.endTime
    relatedUrl = event.relatedUrl
    relatedUrlTitle = event.relatedUrlTitle

    if (event.tutors != null) tutors.addAll(event.tutors.knownType.allIncludedIds.asJava)

    deliveryMethod = event.deliveryMethod
    onlineDeliveryUrl = event.onlineDeliveryUrl
    onlinePlatform = event.onlinePlatform.orNull
  }

  def copyTo(event: SmallGroupEvent): Unit = {
    event.title = title
    event.deliveryMethod = deliveryMethod

    if (event.deliveryMethod == FaceToFaceOnly) {
      // clear online only fields
      event.onlineDeliveryUrl = null
      event.onlinePlatform = None
    } else {
      event.onlineDeliveryUrl = onlineDeliveryUrl
      event.onlinePlatform = Option(onlinePlatform)
    }

    if (event.deliveryMethod == OnlineOnly) {
      event.location = null
    } else {
      // If the location name has changed, but the location ID hasn't, we're changing from a map location
      // to a named location
      Option(event.location).collect { case m: MapLocation => m }.foreach { mapLocation =>
        if (location != mapLocation.name && locationId == mapLocation.locationId) {
          locationId = null
        }
      }

      if (location.hasText) {
        if (locationId.hasText) {
          if (locationAlias.hasText) {
            event.location = AliasedMapLocation(locationAlias, MapLocation(location, locationId))
          } else {
            event.location = MapLocation(location, locationId)
          }
        } else {
          event.location = NamedLocation(location)
        }
      } else {
        event.location = null
      }
    }

    event.weekRanges = weekRanges
    event.day = day
    event.startTime = startTime
    event.endTime = endTime
    event.relatedUrl = relatedUrl
    event.relatedUrlTitle = relatedUrlTitle

    if (event.tutors == null) event.tutors = UserGroup.ofUsercodes
    event.tutors.knownType.includedUserIds = tutors.asScala.toSet

  }
}

trait ModifySmallGroupEventBinding extends BindListener {
  self: ModifySmallGroupEventCommandState =>

  override def onBind(result: BindingResult): Unit = {
    // Find all empty textboxes for tutors and remove them - otherwise we end up with a never ending list of empties
    val indexesToRemove = tutors.asScala.zipWithIndex.flatMap { case (tutor, index) =>
      if (!tutor.hasText) Some(index)
      else None
    }

    // We reverse because removing from the back is better
    indexesToRemove.reverse.foreach {
      tutors.remove
    }
  }
}

trait ModifySmallGroupEventValidation extends SelfValidating {
  self: ModifySmallGroupEventCommandState =>

  override def validate(errors: Errors): Unit = {
    if (tutors.isEmpty) { // TAB-1278 Allow unscheduled events
      if (weeks == null || weeks.isEmpty) errors.rejectValue("weeks", "smallGroupEvent.weeks.NotEmpty")

      if (day == null) errors.rejectValue("day", "smallGroupEvent.day.NotEmpty")

      if (startTime == null) errors.rejectValue("startTime", "smallGroupEvent.startTime.NotEmpty")

      if (endTime == null) errors.rejectValue("endTime", "smallGroupEvent.endTime.NotEmpty")
    } else {
      val tutorsValidator = new UsercodeListValidator(tutors, "tutors")
      tutorsValidator.validate(errors)
      if (startTime == null && (day != null || endTime != null)) errors.rejectValue("startTime", "smallGroupEvent.startTime.NotEmpty")
      if (startTime != null && endTime == null) errors.rejectValue("endTime", "smallGroupEvent.endTime.NotEmpty")
    }

    if (relatedUrl != null && relatedUrl.nonEmpty) {
      if (!relatedUrl.toLowerCase.startsWith("http://") && !relatedUrl.toLowerCase.startsWith("https://")) {
        relatedUrl = s"http://$relatedUrl"
      }
      if (!new UrlValidator().isValid(relatedUrl)) errors.rejectValue("relatedUrl", "smallGroupEvent.url.invalid")
    }

    if (relatedUrlTitle.safeLength > SmallGroupEvent.RelatedUrlTitleMaxLength) {
      errors.rejectValue("relatedUrlTitle", "smallGroupEvent.urlTitle.tooLong", Array(SmallGroupEvent.RelatedUrlTitleMaxLength: java.lang.Integer), "")
    }

    if (startTime != null && endTime != null && endTime.isBefore(startTime)) errors.rejectValue("endTime", "smallGroupEvent.endTime.beforeStartTime")

    if (deliveryMethod != OnlineOnly) {
      if (location.safeContains("|")) errors.rejectValue("location", "smallGroupEvent.location.invalidChar")
      if (!isImport && location.hasText && !locationId.hasText && !useNamedLocation) errors.rejectValue("useNamedLocation", "smallGroupEvent.location.named")
    }

    // Verify that the day we're asking for actually exists
    if (weekRanges.nonEmpty && day != null) {
      if (academicYear.weeks(weekRanges.min.minWeek).firstDay.getDayOfWeek > day.jodaDayOfWeek) {
        errors.rejectValue("weeks", "smallGroupEvent.weeks.invalidForThisYear", Array[Object](weekRanges.min.minWeek: JInteger, day.name), s"There is no $day in week ${weekRanges.min.minWeek} for this year")
      }

      if (academicYear.weeks(weekRanges.max.maxWeek).lastDay.getDayOfWeek < day.jodaDayOfWeek) {
        errors.rejectValue("weeks", "smallGroupEvent.weeks.invalidForThisYear", Array[Object](weekRanges.max.maxWeek: JInteger, day.name), s"There is no $day in week ${weekRanges.max.maxWeek} for this year")
      }
    }

    if (deliveryMethod == null) {
      errors.rejectValue("deliveryMethod", "smallGroupEvent.deliveryMethod.NotEmpty")
    }

    if (onlineDeliveryUrl.hasText) {
      if (!onlineDeliveryUrl.toLowerCase.startsWith("http://") && !onlineDeliveryUrl.toLowerCase.startsWith("https://")) {
        onlineDeliveryUrl = s"http://$onlineDeliveryUrl"
      }
      if (!new UrlValidator().isValid(onlineDeliveryUrl)) errors.rejectValue("onlineDeliveryUrl", "smallGroupEvent.url.invalid")
    }
  }
}

trait CreateSmallGroupEventPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CreateSmallGroupEventCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(group))
  }
}

trait CreateSmallGroupEventDescription extends Describable[SmallGroupEvent] {
  self: CreateSmallGroupEventCommandState =>

  override def describe(d: Description): Unit = {
    d.smallGroup(group)
  }

  override def describeResult(d: Description, event: SmallGroupEvent): Unit =
    d.smallGroupEvent(event)
}

trait EditSmallGroupEventPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: EditSmallGroupEventCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(set, module)
    mustBeLinked(group, set)
    mustBeLinked(event, group)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(event))
  }
}

trait EditSmallGroupEventDescription extends Describable[SmallGroupEvent] {
  self: EditSmallGroupEventCommandState =>

  override def describe(d: Description): Unit = {
    d.smallGroupEvent(event)
  }

}

trait ModifySmallGroupEventScheduledNotifications
  extends SchedulesNotifications[SmallGroupEvent, SmallGroupEventOccurrence] with GeneratesNotificationsForSmallGroupEventOccurrence {

  self: SmallGroupServiceComponent =>

  override def transformResult(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence] =
  // get all the occurrences (even the ones in invalid weeks) so they can be cleared
    smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)

  override def scheduledNotifications(occurrence: SmallGroupEventOccurrence): Seq[ScheduledNotification[SmallGroupEventOccurrence]] = {
    generateNotifications(occurrence)
  }
}

trait GeneratesNotificationsForSmallGroupEventOccurrence {

  def generateNotifications(occurrence: SmallGroupEventOccurrence): Seq[ScheduledNotification[SmallGroupEventOccurrence]] = {
    // Only generate notifications for sets that collect attendance and occurrences that are in valid weeks...
    if (occurrence.event.group.groupSet.collectAttendance && occurrence.event.allWeeks.contains(occurrence.week)) {
      occurrence.startDateTime.map(_.toDateTime).map(dt => {
        // ... and have a valid date time
        val endOfEvent = if (occurrence.event.endTime != null)
          dt.withTime(occurrence.event.endTime.getHourOfDay, occurrence.event.endTime.getMinuteOfHour, 0, 0)
        else
          dt
        Seq(
          new ScheduledNotification[SmallGroupEventOccurrence](
            "SmallGroupEventAttendanceReminder",
            occurrence,
            endOfEvent
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
      }).getOrElse(Seq())
    } else {
      Seq()
    }
  }
}
