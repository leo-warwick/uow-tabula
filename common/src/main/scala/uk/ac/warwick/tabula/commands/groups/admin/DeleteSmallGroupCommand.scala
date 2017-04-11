package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object DeleteSmallGroupCommand {
	type Command = Appliable[SmallGroup] with SelfValidating with DeleteSmallGroupCommandState

	def apply(set: SmallGroupSet, group: SmallGroup, isSpreadsheetUpload: Boolean = false): Command =
		new DeleteSmallGroupCommandInternal(set, group, isSpreadsheetUpload)
			with ComposableCommand[SmallGroup]
			with DeleteSmallGroupPermissions
			with DeleteSmallGroupDescription
			with DeleteSmallGroupValidation
			with AutowiringSmallGroupServiceComponent
}

trait DeleteSmallGroupCommandState {
	def set: SmallGroupSet
	def group: SmallGroup
	def isSpreadsheetUpload: Boolean
}

class DeleteSmallGroupCommandInternal(val set: SmallGroupSet, val group: SmallGroup, val isSpreadsheetUpload: Boolean)
	extends CommandInternal[SmallGroup] with DeleteSmallGroupCommandState {
	self: SmallGroupServiceComponent =>


	override def applyInternal(): SmallGroup = transactional() {
		group.preDelete()

		set.groups.remove(group)
		group
	}

}

trait DeleteSmallGroupPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteSmallGroupCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(group, set)
		p.PermissionCheck(Permissions.SmallGroups.Delete, mandatory(group))
	}
}

trait DeleteSmallGroupDescription extends Describable[SmallGroup] {
	self: DeleteSmallGroupCommandState =>

	override def describe(d: Description) {
		d.smallGroup(group)
	}
}

trait DeleteSmallGroupValidation extends SelfValidating {
	self: DeleteSmallGroupCommandState with SmallGroupServiceComponent =>

	def validate(errors: Errors) {
		// when modifying ther groups via a spreadsheet allow groups with students to be deleted
		if (!group.students.isEmpty && !isSpreadsheetUpload) {
			errors.reject("smallGroup.delete.notEmpty")
		} else {
			// Can't delete events that have attendance recorded against them
			val hasAttendance =
				group.events.exists { event =>
					smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
						.exists {
							_.attendance.asScala.exists { attendance =>
								attendance.state != AttendanceState.NotRecorded
							}
						}
				}

			if (hasAttendance) {
				errors.reject("smallGroupEvent.delete.hasAttendance")
			}
		}
	}
}
