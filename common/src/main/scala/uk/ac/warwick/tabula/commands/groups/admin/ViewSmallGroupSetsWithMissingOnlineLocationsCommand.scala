package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.groups.admin.ViewSmallGroupSetsWithMissingOnlineLocationsCommand._
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.{EventDeliveryMethod, SmallGroupEvent, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.collection.immutable.ListMap


object ViewSmallGroupSetsWithMissingOnlineLocationsCommand {
  val RequiredPermission = Permissions.SmallGroups.Update

  type Result = Seq[(SmallGroupSet, Map[EventDeliveryMethod, Seq[SmallGroupEvent]])]

  def apply(academicYear: AcademicYear, department: Department) =
    new ViewSmallGroupSetsWithMissingOnlineLocationsCommand(academicYear, department)
      with ComposableCommand[Result]
      with ViewSmallGroupSetsWithMissingOnlineLocationPermissions
      with AutowiringSmallGroupServiceComponent
      with ReadOnly with Unaudited {
      override lazy val eventName = "ViewSmallGroupSetsWithMissingOnlineLocations"
    }
}

class ViewSmallGroupSetsWithMissingOnlineLocationsCommand(val academicYear: AcademicYear, val department: Department)
  extends CommandInternal[Result]
    with ViewSmallGroupSetsWithMissingOnlineLocationState {
  self: SmallGroupServiceComponent =>

  override def applyInternal(): Result = {
    val setInfo = smallGroupService.listSmallGroupSetsWithEventsWithoutOnlineLocation(academicYear, Some(department))
      .toSeq.sortBy { case (set, _) => (set.module, set) }
    setInfo.map { case (s, events) =>
      (s, ListMap(events.groupBy(_.deliveryMethod).toSeq.sortWith(_._1.description > _._1.description): _*))
    }
  }
}

trait ViewSmallGroupSetsWithMissingOnlineLocationPermissions extends RequiresPermissionsChecking {
  self: ViewSmallGroupSetsWithMissingOnlineLocationState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(ViewSmallGroupSetsWithMissingOnlineLocationsCommand.RequiredPermission, department)
  }
}

trait ViewSmallGroupSetsWithMissingOnlineLocationState {
  def academicYear: AcademicYear

  def department: Department
}
