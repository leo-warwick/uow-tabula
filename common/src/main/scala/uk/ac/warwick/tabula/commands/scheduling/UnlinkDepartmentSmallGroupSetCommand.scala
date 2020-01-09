package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.data.model.notifications.groups.UnlinkedDepartmentSmallGroupSetNotification
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object UnlinkDepartmentSmallGroupSetCommand {
  def apply() =
    new UnlinkDepartmentSmallGroupSetCommandInternal
      with AutowiringSmallGroupServiceComponent
      with ComposableCommand[Map[Department, Seq[DepartmentSmallGroupSet]]]
      with UnlinkDepartmentSmallGroupSetDescription
      with UnlinkDepartmentSmallGroupSetPermissions
      with UnlinkDepartmentSmallGroupSetNotifications
}


class UnlinkDepartmentSmallGroupSetCommandInternal extends CommandInternal[Map[Department, Seq[DepartmentSmallGroupSet]]] {

  self: SmallGroupServiceComponent =>

  override def applyInternal(): Map[Department, Seq[DepartmentSmallGroupSet]] = {
    val academicYear = AcademicYear.now()
    val setMap = transactional() {
      smallGroupService.findDepartmentSmallGroupSetsLinkedToSITSByDepartment(academicYear)
    }
    setMap.map { case (department, sets) => department -> sets.map { set =>
      transactional() {
        set.memberQuery = null
        set.members.knownType.includedUserIds = (set.members.knownType.staticUserIds diff set.members.knownType.excludedUserIds) ++ set.members.knownType.includedUserIds
        smallGroupService.saveOrUpdate(set)
        set
      }
    }
    }
  }

}

trait UnlinkDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.SmallGroups.UpdateMembership)
  }

}

trait UnlinkDepartmentSmallGroupSetDescription extends Describable[Map[Department, Seq[DepartmentSmallGroupSet]]] {

  override lazy val eventName = "UnlinkDepartmentSmallGroupSet"

  override def describe(d: Description): Unit = {

  }

  override def describeResult(d: Description, result: Map[Department, Seq[DepartmentSmallGroupSet]]): Unit = {
    d.property("updatedSets" -> result.map { case (dept, sets) => dept.code -> sets.map(_.id) })
  }
}

trait UnlinkDepartmentSmallGroupSetNotifications extends Notifies[Map[Department, Seq[DepartmentSmallGroupSet]], Map[Department, Seq[DepartmentSmallGroupSet]]] {

  def emit(result: Map[Department, Seq[DepartmentSmallGroupSet]]): Seq[UnlinkedDepartmentSmallGroupSetNotification] = {
    result.map { case (department, sets) =>
      Notification.init(new UnlinkedDepartmentSmallGroupSetNotification, null, sets, department)
    }.toSeq
  }
}
