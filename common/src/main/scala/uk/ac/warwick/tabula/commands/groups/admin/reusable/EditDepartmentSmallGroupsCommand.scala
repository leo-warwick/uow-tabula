package uk.ac.warwick.tabula.commands.groups.admin.reusable

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.mutable

object EditDepartmentSmallGroupsCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet) =
		new EditDepartmentSmallGroupsCommandInternal(department, set)
			with ComposableCommand[Seq[DepartmentSmallGroup]]
			with EditDepartmentSmallGroupsPermissions
			with EditDepartmentSmallGroupsValidation
			with EditDepartmentSmallGroupsDescription
			with PopulateEditDepartmentSmallGroupsCommand
			with EditDepartmentSmallGroupsCommandRemoveTrailingEmptyGroups
			with AutowiringSmallGroupServiceComponent
}

trait EditDepartmentSmallGroupsCommandState {
	def department: Department
	def set: DepartmentSmallGroupSet

	var groupNames: JList[String] = LazyLists.create()
	var groupIds: JList[String] = LazyLists.create()
}

class EditDepartmentSmallGroupsCommandInternal(val department: Department, val set: DepartmentSmallGroupSet) extends CommandInternal[Seq[DepartmentSmallGroup]] with EditDepartmentSmallGroupsCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal(): mutable.Buffer[DepartmentSmallGroup] = {
		// Edit existing groups and add new groups
		groupNames.asScala.zipWithIndex.foreach { case (groupName, groupNameIndex) =>
			groupIds.asScala.zipWithIndex.find { case (_, groupIdIndex) => groupIdIndex == groupNameIndex} match {
				case Some((groupId, _)) =>
					// Edit an existing group
					set.groups.asScala.find(_.id == groupId)
						.getOrElse(throw new IllegalArgumentException(s"Could not find existing group with id = $groupId for group $groupName"))
						.name = groupName
				case _ =>
					// Add a new group
					val group = new DepartmentSmallGroup(set)
					group.name = groupName

					set.groups.add(group)

					// We also need to create a linked group elsewhere for any sets linked to this set
					set.linkedSets.asScala.foreach { smallGroupSet =>
						val smallGroup = new SmallGroup
						smallGroup.name = groupName
						smallGroup.linkedDepartmentSmallGroup = group
						smallGroupSet.groups.add(smallGroup)
					}
			}
		}

		// Remove groups
		val groupsToRemove = set.groups.asScala.filter(group => !groupNames.contains(group.name) && !groupIds.contains(group.id))
		groupsToRemove.foreach { group =>
			// Remove any linked groups
			group.linkedGroups.asScala.map { smallGroup =>
				smallGroup.preDelete()
				smallGroup
			}.foreach { smallGroup =>
				smallGroup.groupSet.groups.remove(smallGroup)
			}
		}
		set.groups.removeAll(groupsToRemove.asJava)

		smallGroupService.saveOrUpdate(set)
		set.linkedSets.asScala.foreach(smallGroupService.saveOrUpdate)

		set.groups.asScala
	}
}

trait PopulateEditDepartmentSmallGroupsCommand extends PopulateOnForm {
	self: EditDepartmentSmallGroupsCommandState =>

	override def populate() {
		groupNames.clear()
		groupIds.clear()
		set.groups.asScala.sortBy(_.name).foreach(group => {
			groupNames.add(group.name)
			groupIds.add(group.id)
		})
	}
}

trait EditDepartmentSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentSmallGroupsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditDepartmentSmallGroupsDescription extends Describable[Seq[DepartmentSmallGroup]] {
	self: EditDepartmentSmallGroupsCommandState =>

	override def describe(d: Description) {
		d.department(set.department).properties("smallGroupSet" -> set.id)
	}

}

trait EditDepartmentSmallGroupsValidation extends SelfValidating {
	self: EditDepartmentSmallGroupsCommandState with SmallGroupServiceComponent =>

	override def validate(errors: Errors) {
		groupNames.asScala.zipWithIndex.foreach { case (name, index) =>
			if (!name.hasText) errors.rejectValue(s"groupNames[$index]", "smallGroup.name.NotEmpty")
			else if (name.orEmpty.length > 200) errors.rejectValue(s"groupNames[$index]", "smallGroup.name.Length", Array[Object](200: JInteger), "")
		}

		val postedGroupNameSize = groupNames.size
		set.groups.asScala.filterNot(g => groupNames.contains(g.name)).zipWithIndex.foreach { case (group, index) =>
			if (!group.students.isEmpty) {
				groupNames.add(group.name) // Add the group name back in so the error message makes sense
				errors.rejectValue(s"groupNames[${postedGroupNameSize + index}]", "smallGroup.delete.notEmpty")
			} else {
				val hasAttendance =
					group.linkedGroups.asScala.flatMap(_.events).exists { event =>
						smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
							.exists {
								_.attendance.asScala.exists { attendance =>
									attendance.state != AttendanceState.NotRecorded
								}
							}
					}

				if (hasAttendance) {
					groupNames.add(group.name) // Add the group name back in so the error message makes sense
					errors.rejectValue(s"groupNames[${postedGroupNameSize + index}]", "smallGroupEvent.delete.hasAttendance")
				}
			}
		}
	}
}

trait EditDepartmentSmallGroupsCommandRemoveTrailingEmptyGroups extends BindListener {
	self: EditDepartmentSmallGroupsCommandState =>

	override def onBind(result: BindingResult) {
		// If the last element of events is both a Creation and is empty, disregard it
		while (!groupNames.isEmpty && !groupNames.asScala.last.hasText) {
			groupNames.remove(groupNames.asScala.last)
		}
	}
}