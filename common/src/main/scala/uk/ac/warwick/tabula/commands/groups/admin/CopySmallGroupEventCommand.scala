package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object CopySmallGroupEventCommand {
  type Command = Appliable[SmallGroupEvent] with SelfValidating with BindListener with CopySmallGroupEventCommandState

  def copy(module: Module, event: SmallGroupEvent): Command =
    new CopySmallGroupEventCommandInternal(module, event)
      with ComposableCommand[SmallGroupEvent]
      with CopySmallGroupEventPermissions
      with CopySmallGroupEventDescription
      with ModifySmallGroupEventValidation
      with ModifySmallGroupEventBinding
      with ModifySmallGroupEventScheduledNotifications
      with AutowiringSmallGroupServiceComponent
}

trait CopySmallGroupEventCommandState extends ModifySmallGroupEventCommandState {

  def event: SmallGroupEvent

  def existingEvent: Option[SmallGroupEvent] = Some(event)

  override def group: SmallGroup = event.group

  override def set: SmallGroupSet = group.groupSet

  override def isImport: Boolean = false

  override def isCopy: Boolean = true

}

trait CopySmallGroupEventPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CopySmallGroupEventCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(group))
  }
}

trait CopySmallGroupEventDescription extends Describable[SmallGroupEvent] {
  self: CopySmallGroupEventCommandState =>

  override def describe(d: Description): Unit = {
      d.smallGroupEvent(event)
  }

  override def describeResult(d: Description, event: SmallGroupEvent): Unit =
    d.smallGroupEvent(event)
}

class CopySmallGroupEventCommandInternal(val module: Module, val event: SmallGroupEvent)
  extends ModifySmallGroupEventCommandInternal with CopySmallGroupEventCommandState {

  self: SmallGroupServiceComponent =>

  copyFrom(event)

  override def applyInternal(): SmallGroupEvent = createGroupEvent()
}
