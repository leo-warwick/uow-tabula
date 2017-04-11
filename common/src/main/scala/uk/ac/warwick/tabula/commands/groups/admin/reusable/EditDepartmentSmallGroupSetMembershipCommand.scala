package uk.ac.warwick.tabula.commands.groups.admin.reusable

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists

case class EditDepartmentSmallGroupSetMembershipCommandResult(
	includedStudentIds: JList[String],
	excludedStudentIds: JList[String],
	membershipItems: Seq[DepartmentSmallGroupSetMembershipItem]
)

case class AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult(
	missingUsers: Seq[String]
)

object EditDepartmentSmallGroupSetMembershipCommand {
	def apply(department: Department, set: DepartmentSmallGroupSet) =
		new EditDepartmentSmallGroupSetMembershipCommandInternal(department, set)
			with AutowiringUserLookupComponent
			with ComposableCommand[EditDepartmentSmallGroupSetMembershipCommandResult]
			with PopulateEditDepartmentSmallGroupSetMembershipCommand
			with AddsUsersToEditDepartmentSmallGroupSetMembershipCommand
			with RemovesUsersFromEditDepartmentSmallGroupSetMembershipCommand
			with ResetsMembershipInEditDepartmentSmallGroupSetMembershipCommand
			with EditDepartmentSmallGroupSetMembershipPermissions
			with Unaudited with ReadOnly
}

/**
 * Not persisted, just used to validate users entered and render student table
 */
class EditDepartmentSmallGroupSetMembershipCommandInternal(val department: Department, val set: DepartmentSmallGroupSet)
	extends CommandInternal[EditDepartmentSmallGroupSetMembershipCommandResult] with EditDepartmentSmallGroupSetMembershipCommandState {
	self: UserLookupComponent =>

	override def applyInternal(): EditDepartmentSmallGroupSetMembershipCommandResult = {
		def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
			val user = userLookup.getUserByWarwickUniId(universityId)
			DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
		}

		val membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
			val excludedMemberItems = excludedStudentIds.asScala.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipExcludeType))
			val includedMemberItems = includedStudentIds.asScala.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipIncludeType))
			(excludedMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
		}

		EditDepartmentSmallGroupSetMembershipCommandResult(
			includedStudentIds,
			excludedStudentIds,
			membershipItems
		)
	}

}

trait PopulateEditDepartmentSmallGroupSetMembershipCommand extends PopulateOnForm {

	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	override def populate(): Unit = {
		includedStudentIds = set.members.knownType.includedUserIds.asJava
		excludedStudentIds = set.members.knownType.excludedUserIds.asJava
	}

}

trait AddsUsersToEditDepartmentSmallGroupSetMembershipCommand {
	self: EditDepartmentSmallGroupSetMembershipCommandState with UserLookupComponent =>

	def addUsers(): AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult = {
		def getUserForString(entry: String): Option[User] = {
			if (UniversityId.isValid(entry)) {
				val user = userLookup.getUserByWarwickUniId(entry)

				if (user.isFoundUser) Some(user)
				else None
			} else {
				val user = userLookup.getUserByUserId(entry)

				if (user.isFoundUser) Some(user)
				else None
			}
		}

		val massAddedUserMap: Map[String, Option[User]] = massAddUsersEntries.map{ entry =>
			entry -> getUserForString(entry)
		}.toMap

		val missingUsers = massAddedUserMap.filter(!_._2.isDefined).keys.toSeq
		val validUsers = massAddedUserMap.filter(_._2.isDefined).values.flatten.toSeq

		includedStudentIds = (includedStudentIds.asScala.toSeq ++ validUsers.map(_.getWarwickId)).distinct.asJava
		excludedStudentIds = (excludedStudentIds.asScala.toSeq diff includedStudentIds.asScala.toSeq).distinct.asJava

		// Users processed, so reset fields
		massAddUsers = ""

		AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult(missingUsers)
	}

}

trait RemovesUsersFromEditDepartmentSmallGroupSetMembershipCommand {

	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	def removeUsers(): Unit = {
		excludedStudentIds = (excludedStudentIds.asScala ++ excludeIds.asScala).distinct.asJava
	}
}

trait ResetsMembershipInEditDepartmentSmallGroupSetMembershipCommand {

	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	def resetMembership(): Unit = {
		includedStudentIds = (includedStudentIds.asScala diff resetStudentIds.asScala).asJava
		excludedStudentIds = (excludedStudentIds.asScala diff resetStudentIds.asScala).asJava
	}

	def resetAllIncluded(): Unit = {
		includedStudentIds.clear()
	}

	def resetAllExcluded(): Unit = {
		excludedStudentIds.clear()
	}
}


trait EditDepartmentSmallGroupSetMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, department)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}

}

trait EditDepartmentSmallGroupSetMembershipCommandState {
	def set: DepartmentSmallGroupSet
	def department: Department

	// Bind variables

	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()

	var massAddUsers: String = _
	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split "(\\s|[^A-Za-z\\d\\-_\\.])+" map (_.trim) filterNot (_.isEmpty)

	var excludeIds: JList[String] = LazyLists.create()
	var resetStudentIds: JList[String] = LazyLists.create()
}
