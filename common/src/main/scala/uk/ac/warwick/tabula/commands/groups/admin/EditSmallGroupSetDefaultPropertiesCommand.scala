package uk.ac.warwick.tabula.commands.groups.admin

import org.apache.commons.validator.routines.UrlValidator
import org.joda.time.LocalTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.EditSmallGroupSetDefaultPropertiesCommand._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.UsercodeListValidator

import scala.jdk.CollectionConverters._

object EditSmallGroupSetDefaultPropertiesCommand {
  val DefaultStartTime = new LocalTime(12, 0)
  val DefaultEndTime: LocalTime = DefaultStartTime.plusHours(1)

  def apply(module: Module, set: SmallGroupSet) =
    new EditSmallGroupSetDefaultPropertiesCommandInternal(module, set)
      with ComposableCommand[SmallGroupSet]
      with EditSmallGroupSetDefaultPropertiesPermissions
      with EditSmallGroupSetDefaultPropertiesValidation
      with EditSmallGroupSetDefaultPropertiesDescription
      with AutowiringSmallGroupServiceComponent
}

trait EditSmallGroupSetDefaultPropertiesCommandState {
  def module: Module

  def set: SmallGroupSet

  var defaultWeeks: JSet[JInteger] = JSet()
  var defaultDay: DayOfWeek = _
  var defaultStartTime: LocalTime = DefaultStartTime
  var defaultEndTime: LocalTime = DefaultEndTime
  var defaultTutors: JList[String] = JArrayList()
  var defaultLocation: String = _
  var defaultLocationId: String = _
  var resetExistingEvents: Boolean = false
  var useNamedLocation: Boolean = _
  var defaultLocationAlias: String = _
  var defaultDeliveryMethod: EventDeliveryMethod = _
  var defaultOnlineDeliveryUrl: String = _
  var defaultOnlinePlatform: OnlinePlatform = _

  def defaultWeekRanges: Seq[WeekRange] = Option(defaultWeeks) map { weeks => WeekRange.combine(weeks.asScala.toSeq.map(_.intValue))
  } getOrElse Seq()

  def defaultWeekRanges_=(ranges: Seq[WeekRange]): Unit = {
    defaultWeeks =
      JHashSet(ranges
        .flatMap { range => range.minWeek to range.maxWeek }
        .map(i => JInteger(Some(i)))
        .toSet)
  }
}

class EditSmallGroupSetDefaultPropertiesCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[SmallGroupSet] with EditSmallGroupSetDefaultPropertiesCommandState {
  self: SmallGroupServiceComponent =>

  copyFrom(set)

  override def applyInternal(): SmallGroupSet = {
    copyTo(set)

    if (resetExistingEvents) {
      set.groups.asScala.flatMap(_.events).foreach { event =>
        event.weekRanges = set.defaultWeekRanges
        event.day = set.defaultDay
        event.startTime = set.defaultStartTime
        event.endTime = set.defaultEndTime
        event.location = set.defaultLocation
        event.tutors.copyFrom(set.defaultTutors)
        event.deliveryMethod = set.defaultDeliveryMethod
        event.onlineDeliveryUrl = set.defaultOnlineDeliveryUrl
        event.onlinePlatform = Option(set.defaultOnlinePlatform)
      }
    }

    smallGroupService.saveOrUpdate(set)
    set
  }

  def copyFrom(set: SmallGroupSet): Unit = {
    Option(set.defaultLocation).foreach {
      case NamedLocation(name) => defaultLocation = name
      case MapLocation(name, lid, _) =>
        defaultLocation = name
        defaultLocationId = lid
      case AliasedMapLocation(displayName, MapLocation(name, lid, _)) =>
        defaultLocation = name
        defaultLocationId = lid
        defaultLocationAlias = displayName
    }

    defaultWeekRanges = set.defaultWeekRanges
    defaultDay = set.defaultDay
    defaultStartTime = set.defaultStartTime
    defaultEndTime = set.defaultEndTime
    defaultDeliveryMethod = set.defaultDeliveryMethod
    defaultOnlineDeliveryUrl = set.defaultOnlineDeliveryUrl
    defaultOnlinePlatform = set.defaultOnlinePlatform

    if (set.defaultTutors != null) defaultTutors.addAll(set.defaultTutors.knownType.allIncludedIds.asJava)
  }

  def copyTo(set: SmallGroupSet): Unit = {
    // If the location name has changed, but the location ID hasn't, we're changing from a map location
    // to a named location
    Option(set.defaultLocation).collect { case m: MapLocation => m }.foreach { mapLocation =>
      if (defaultLocation != mapLocation.name && defaultLocationId == mapLocation.locationId) {
        defaultLocationId = null
      }
    }

    if (defaultLocation.hasText) {
      if (defaultLocationId.hasText) {
        if (defaultLocationAlias.hasText) {
          set.defaultLocation = AliasedMapLocation(defaultLocationAlias, MapLocation(defaultLocation, defaultLocationId))
        } else {
          set.defaultLocation = MapLocation(defaultLocation, defaultLocationId)
        }
      } else {
        set.defaultLocation = NamedLocation(defaultLocation)
      }
    } else {
      set.defaultLocation = null
    }

    set.defaultWeekRanges = defaultWeekRanges
    set.defaultDay = defaultDay
    set.defaultStartTime = defaultStartTime
    set.defaultEndTime = defaultEndTime

    set.defaultDeliveryMethod = defaultDeliveryMethod
    set.defaultOnlineDeliveryUrl = defaultOnlineDeliveryUrl
    set.defaultOnlinePlatform = defaultOnlinePlatform

    if (set.defaultTutors == null) set.defaultTutors = UserGroup.ofUsercodes
    set.defaultTutors.knownType.includedUserIds = defaultTutors.asScala.toSet
  }
}

trait EditSmallGroupSetDefaultPropertiesValidation extends SelfValidating {
  self: EditSmallGroupSetDefaultPropertiesCommandState =>

  override def validate(errors: Errors): Unit = {
    if (defaultLocation.safeContains("|")) errors.rejectValue("defaultLocation", "smallGroupEvent.location.invalidChar")

    if (!defaultTutors.isEmpty) {
      val tutorsValidator = new UsercodeListValidator(defaultTutors, "defaultTutors")
      tutorsValidator.validate(errors)
    }

    if (defaultEndTime != null && defaultStartTime != null && defaultEndTime.isBefore(defaultStartTime))
      errors.rejectValue("defaultEndTime", "smallGroupEvent.endTime.beforeStartTime")

    if (defaultLocation.hasText && !defaultLocationId.hasText && !useNamedLocation) errors.rejectValue("useNamedLocation", "smallGroupEvent.defaults.location.named")

    if (defaultOnlineDeliveryUrl.hasText) {
      if (!defaultOnlineDeliveryUrl.toLowerCase.startsWith("http://") && !defaultOnlineDeliveryUrl.toLowerCase.startsWith("https://")) {
        defaultOnlineDeliveryUrl = s"http://$defaultOnlineDeliveryUrl"
      }
      if (!new UrlValidator().isValid(defaultOnlineDeliveryUrl)) errors.rejectValue("defaultOnlineDeliveryUrl", "smallGroupEvent.url.invalid")
    }
  }
}

trait EditSmallGroupSetDefaultPropertiesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: EditSmallGroupSetDefaultPropertiesCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(set, module)
    p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
  }
}

trait EditSmallGroupSetDefaultPropertiesDescription extends Describable[SmallGroupSet] {
  self: EditSmallGroupSetDefaultPropertiesCommandState =>

  override def describe(d: Description): Unit = {
    d.smallGroupSet(set)
  }

}
