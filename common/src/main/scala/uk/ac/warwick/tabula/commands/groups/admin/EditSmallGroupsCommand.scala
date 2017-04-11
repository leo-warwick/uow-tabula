package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupAllocationMethod, SmallGroupSet}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import EditSmallGroupsCommand._

import scala.collection.mutable

object EditSmallGroupsCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new EditSmallGroupsCommandInternal(module, set)
			with ComposableCommand[Seq[SmallGroup]]
			with EditSmallGroupsPermissions
			with EditSmallGroupsValidation
			with EditSmallGroupsDescription
			with PopulateEditSmallGroupsCommand
			with EditSmallGroupsCommandRemoveTrailingEmptyGroups
			with AutowiringSmallGroupServiceComponent {
			populate()
		}

	trait GroupProperties {
		def module: Module
		def set: SmallGroupSet

		var name: String = _
		var maxGroupSize: JInteger = _
	}

	class NewGroupProperties(val module: Module, val set: SmallGroupSet) extends GroupProperties {
		maxGroupSize = SmallGroup.DefaultGroupSize
	}

	class ExistingGroupProperties(val module: Module, val set: SmallGroupSet, val group: SmallGroup) extends GroupProperties {
		name = group.name
		maxGroupSize = group.maxGroupSize

		var delete: Boolean = false
	}
}

trait EditSmallGroupsCommandState {
	def module: Module
	def set: SmallGroupSet

	var existingGroups: JMap[String, ExistingGroupProperties] = JHashMap()
	var newGroups: JList[NewGroupProperties] = LazyLists.createWithFactory { () => new NewGroupProperties(module, set) }
	var defaultMaxGroupSizeEnabled: Boolean = true
	var defaultMaxGroupSize: Int = SmallGroup.DefaultGroupSize
}

class EditSmallGroupsCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[Seq[SmallGroup]] with EditSmallGroupsCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal(): mutable.Buffer[SmallGroup] = {
		// Manage existing groups
		existingGroups.asScala.values.filterNot { _.delete }.foreach { props =>
			val group = props.group
			group.name = props.name
			group.maxGroupSize = props.maxGroupSize
		}

		existingGroups.asScala.values.filter { _.delete }.foreach { props =>
			val group = props.group

			group.preDelete()

			set.groups.remove(group)
		}

		// Create new groups
		newGroups.asScala.foreach { props =>
			val group = new SmallGroup(set)
			set.groups.add(group)
			group.name = props.name
			group.maxGroupSize = props.maxGroupSize
		}

		smallGroupService.saveOrUpdate(set)
		set.groups.asScala
	}
}

trait PopulateEditSmallGroupsCommand {
	self: EditSmallGroupsCommandState =>

	def populate() {
		existingGroups.clear()
		newGroups.clear()

		set.groups.asScala.sorted.foreach { group =>
			existingGroups.put(group.id, new ExistingGroupProperties(module, set, group))
		}
	}
}

trait EditSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupsDescription extends Describable[Seq[SmallGroup]] {
	self: EditSmallGroupsCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}

trait EditSmallGroupsValidation extends SelfValidating {
	self: EditSmallGroupsCommandState with SmallGroupServiceComponent =>

	override def validate(errors: Errors) {
		if (set.allocationMethod == SmallGroupAllocationMethod.Linked) {
			errors.reject("smallGroupSet.linked")
		}

		def validateGroupProperties(props: GroupProperties) {
			if (!props.name.hasText) errors.rejectValue("name", "smallGroup.name.NotEmpty")
			else if (props.name.orEmpty.length > 200) errors.rejectValue("name", "smallGroup.name.Length", Array[Object](200: JInteger), "")
		}

		existingGroups.asScala.foreach { case (id, props) =>
			errors.pushNestedPath(s"existingGroups[$id]")

			if (props.delete) {
				val group = props.group

				if (!group.students.isEmpty) {
					errors.rejectValue("delete", "smallGroup.delete.notEmpty")
				} else {
					val hasAttendance =
						group.events.exists { event =>
							smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
								.exists { _.attendance.asScala.exists { attendance =>
								attendance.state != AttendanceState.NotRecorded
							}}
						}

					if (hasAttendance) {
						errors.rejectValue("delete", "smallGroupEvent.delete.hasAttendance")
					}
				}
			} else {
				validateGroupProperties(props)
			}

			errors.popNestedPath()
		}

		newGroups.asScala.zipWithIndex.foreach { case (props, index) =>
			errors.pushNestedPath(s"newGroups[$index]")
			validateGroupProperties(props)
			errors.popNestedPath()
		}
	}
}

trait EditSmallGroupsCommandRemoveTrailingEmptyGroups extends BindListener {
	self: EditSmallGroupsCommandState =>

	override def onBind(result: BindingResult) {
		// If the last element of events is both a Creation and is empty, disregard it
		while (!newGroups.isEmpty && !newGroups.asScala.last.name.hasText) {
			newGroups.remove(newGroups.asScala.last)
		}
	}
}