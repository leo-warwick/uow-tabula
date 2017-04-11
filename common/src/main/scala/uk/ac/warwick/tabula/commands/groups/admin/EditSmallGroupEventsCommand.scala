package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{SmallGroupService, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._
import EditSmallGroupEventsCommand._

object EditSmallGroupEventsCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new EditSmallGroupEventsCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with DeletesSmallGroupEventsWithCommand
			with EditSmallGroupEventsPermissions
			with EditSmallGroupEventsValidation
			with Unaudited
			with AutowiringSmallGroupServiceComponent
			with PopulateEditSmallGroupEventsSubCommands {
			populate()
		}

	class EventProperties(val event: SmallGroupEvent, smallGroupService: SmallGroupService) {
		var delete: Boolean = false
		def hasRecordedAttendance: Boolean = {
			smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
				.exists { _.attendance.asScala.exists { attendance =>
				attendance.state != AttendanceState.NotRecorded
			}}
		}
	}

	class GroupProperties(val module: Module, val set: SmallGroupSet, val group: SmallGroup, smallGroupService: SmallGroupService) {
		var events: JList[EventProperties] = JArrayList()

		group.events.sorted.foreach { event =>
			events.add(new EventProperties(event, smallGroupService))
		}
	}
}

trait EditSmallGroupEventsCommandState {
	def module: Module
	def set: SmallGroupSet

	var groups: JMap[SmallGroup, GroupProperties] = JHashMap()
}

trait PopulateEditSmallGroupEventsSubCommands {
	self: EditSmallGroupEventsCommandState with SmallGroupServiceComponent =>

	def populate() {
		groups.clear()
		set.groups.asScala.sorted.foreach { group =>
			groups.put(group, new GroupProperties(module, set, group, smallGroupService))
		}
	}

}

class EditSmallGroupEventsCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[SmallGroupSet] with EditSmallGroupEventsCommandState {
	self: SmallGroupServiceComponent with DeletesSmallGroupEvents =>

	override def applyInternal(): SmallGroupSet = transactional() {
		groups.asScala.foreach { case (group, props) =>
			props.events.asScala.filter(_.delete).map(_.event).foreach { event =>
				deleteEvent(group, event)
			}
		}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait DeletesSmallGroupEvents {
	def deleteEvent(group: SmallGroup, event: SmallGroupEvent)
}

trait DeletesSmallGroupEventsWithCommand extends DeletesSmallGroupEvents {
	def deleteEvent(group: SmallGroup, event: SmallGroupEvent) {
		DeleteSmallGroupEventCommand(group, event).apply()
	}
}

trait EditSmallGroupEventsValidation extends SelfValidating {
	self: EditSmallGroupEventsCommandState =>

	override def validate(errors: Errors) {
		groups.asScala.foreach { case (group, props) =>
			errors.pushNestedPath(s"groups[${group.id}]")

			props.events.asScala.zipWithIndex.filter { case (props, _) => props.delete }.foreach { case (props, index) =>
				errors.pushNestedPath(s"events[$index]")

				if (props.hasRecordedAttendance) {
					errors.rejectValue("delete", "smallGroupEvent.delete.hasAttendance")
				}

				errors.popNestedPath()
			}

			errors.popNestedPath()
		}
	}
}

trait EditSmallGroupEventsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupEventsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}
