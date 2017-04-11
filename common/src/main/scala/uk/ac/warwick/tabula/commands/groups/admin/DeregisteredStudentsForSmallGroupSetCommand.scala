package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.groups.admin.DeregisteredStudentsForSmallGroupSetCommand.StudentNotInMembership
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSmallGroupServiceComponent, ProfileServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.mutable

object DeregisteredStudentsForSmallGroupSetCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new DeregisteredStudentsForSmallGroupSetCommandInternal(module, set)
			with ComposableCommand[Seq[StudentNotInMembership]]
			with DeregisteredStudentsForSmallGroupSetPermissions
			with DeregisteredStudentsForSmallGroupSetDescription
			with AutowiringSmallGroupServiceComponent
			with AutowiringProfileServiceComponent
			with PopulateDeregisteredStudentsForSmallGroupSetCommandState

	case class StudentNotInMembership(
		student: MemberOrUser,
		group: SmallGroup
	)
}

trait DeregisteredStudentsForSmallGroupSetCommandState {
	def module: Module
	def set: SmallGroupSet

	var students: JList[User] = JArrayList()
}

trait PopulateDeregisteredStudentsForSmallGroupSetCommandState extends PopulateOnForm {
	self: DeregisteredStudentsForSmallGroupSetCommandState =>

	def populate() {
		students.clear()
		students.addAll(set.studentsNotInMembership.sortBy { user => (user.getLastName, user.getFirstName, user.getWarwickId) }.asJavaCollection)
	}
}

class DeregisteredStudentsForSmallGroupSetCommandInternal(val module: Module, val set: SmallGroupSet)
	extends CommandInternal[Seq[StudentNotInMembership]]
		with DeregisteredStudentsForSmallGroupSetCommandState {
	self: SmallGroupServiceComponent with ProfileServiceComponent =>

	override def applyInternal(): mutable.Buffer[StudentNotInMembership] =
		for {
			student <- students.asScala
			if !set.members.includesUser(student) // Prevent irrelevant students being sent

			group <- set.groups.asScala
			if group.students.includesUser(student)
		} yield {
			smallGroupService.removeUserFromGroup(student, group)
			StudentNotInMembership(
				MemberOrUser(profileService.getMemberByUser(student, disableFilter = true), student),
				group
			)
		}

}

trait DeregisteredStudentsForSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeregisteredStudentsForSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait DeregisteredStudentsForSmallGroupSetDescription extends Describable[Seq[StudentNotInMembership]] {
	self: DeregisteredStudentsForSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

	override def describeResult(d: Description, result: Seq[StudentNotInMembership]) {
		d.smallGroupSet(set)
		 .studentIds(result.flatMap { _.student.universityId.maybeText })
	}

}
