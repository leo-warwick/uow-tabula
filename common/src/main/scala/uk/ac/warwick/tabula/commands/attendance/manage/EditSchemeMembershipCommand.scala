package uk.ac.warwick.tabula.commands.attendance.manage

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.{CurrentUser, UniversityId}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.StudentMember
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.data.{SchemeMembershipIncludeType, SchemeMembershipExcludeType}
import uk.ac.warwick.tabula.data.SchemeMembershipItem

case class EditSchemeMembershipCommandResult(
	includedStudentIds: JList[String],
	excludedStudentIds: JList[String],
	membershipItems: Seq[SchemeMembershipItem]
)

case class AddUsersToEditSchemeMembershipCommandResult(
	missingMembers: Seq[String],
	noPermissionMembers: Seq[StudentMember]
)

object EditSchemeMembershipCommand {
	def apply(scheme: AttendanceMonitoringScheme, user: CurrentUser) =
		new EditSchemeMembershipCommandInternal(scheme, user)
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringSecurityServiceComponent
			with ComposableCommand[EditSchemeMembershipCommandResult]
			with PopulateEditSchemeMembershipCommand
			with AddsUsersToEditSchemeMembershipCommand
			with RemovesUsersFromEditSchemeMembershipCommand
			with ResetsMembershipInEditSchemeMembershipCommand
			with EditSchemeMembershipPermissions
			with EditSchemeMembershipCommandState
			with Unaudited with ReadOnly
}

/**
 * Not persisted, just used to validate users entered and render student table
 */
class EditSchemeMembershipCommandInternal(val scheme: AttendanceMonitoringScheme, val user: CurrentUser)
	extends CommandInternal[EditSchemeMembershipCommandResult] {

	self: EditSchemeMembershipCommandState with UserLookupComponent with ProfileServiceComponent
		with AttendanceMonitoringServiceComponent with SecurityServiceComponent =>

	override def applyInternal(): EditSchemeMembershipCommandResult = {
		val membershipItems: Seq[SchemeMembershipItem] = {
			val excludedMemberItems = attendanceMonitoringService.findSchemeMembershipItems(excludedStudentIds.asScala, SchemeMembershipExcludeType, scheme.academicYear)
			val includedMemberItems = attendanceMonitoringService.findSchemeMembershipItems(includedStudentIds.asScala, SchemeMembershipIncludeType, scheme.academicYear)
			// TAB-4242 If a member has gone missing they can't be removed from the excluded or included students unless we add some stubs in
			val missingExcludedItems = excludedStudentIds.asScala.diff(excludedMemberItems.map(_.universityId)).map(universityId => SchemeMembershipItem(
				itemType = SchemeMembershipExcludeType,
				firstName = "(unknown)",
				lastName = "(unknown)",
				universityId = universityId,
				userId = "(unknown)",
				existingSchemes = Seq()
			))
			val missingIncludedItems = includedStudentIds.asScala.diff(includedMemberItems.map(_.universityId)).map(universityId => SchemeMembershipItem(
				itemType = SchemeMembershipIncludeType,
				firstName = "(unknown)",
				lastName = "(unknown)",
				universityId = universityId,
				userId = "(unknown)",
				existingSchemes = Seq()
			))
			(excludedMemberItems ++ includedMemberItems ++ missingExcludedItems ++ missingIncludedItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
		}

		EditSchemeMembershipCommandResult(
			includedStudentIds,
			excludedStudentIds,
			membershipItems
		)
	}

}

trait PopulateEditSchemeMembershipCommand extends PopulateOnForm {

	self: EditSchemeMembershipCommandState =>

	override def populate(): Unit = {
		includedStudentIds = scheme.members.includedUserIds.asJava
		excludedStudentIds = scheme.members.excludedUserIds.asJava
	}

}

trait AddsUsersToEditSchemeMembershipCommand {

	self: EditSchemeMembershipCommandState with ProfileServiceComponent with UserLookupComponent with SecurityServiceComponent =>

	def addUsers(): AddUsersToEditSchemeMembershipCommandResult = {
		def getStudentMemberForString(entry: String): Option[StudentMember] = {
			if (UniversityId.isValid(entry)) {
				profileService.getMemberByUniversityId(entry) match {
					case Some(student: StudentMember) => Some(student)
					case _ => None
				}
			} else {
				val user = userLookup.getUserByUserId(entry)
				if (user.isFoundUser) {
					profileService.getMemberByUser(user) match {
						case Some(student: StudentMember) => Some(student)
						case _ => None
					}
				} else {
					None
				}
			}
		}

		val massAddedUserMap: Map[String, Option[StudentMember]] = massAddUsersEntries.map{ entry =>
			entry -> getStudentMemberForString(entry)
		}.toMap

		val missingMembers = massAddedUserMap.filter(!_._2.isDefined).keys.toSeq
		val validMembers = massAddedUserMap.filter(_._2.isDefined).values.flatten.toSeq
		val permissionsMap = validMembers.map{member => member -> securityService.can(user, Permissions.MonitoringPoints.Manage, member)}
		val noPermissionsMembers = permissionsMap.filter(!_._2).toMap.keySet.toSeq
		val validPermissionMembers = permissionsMap.filter(_._2).toMap.keySet

		includedStudentIds = (includedStudentIds.asScala.toSeq ++ validPermissionMembers.map(_.universityId)).distinct.asJava
		excludedStudentIds = (excludedStudentIds.asScala.toSeq diff includedStudentIds.asScala.toSeq).distinct.asJava

		// Users processed, so reset fields
		massAddUsers = ""

		AddUsersToEditSchemeMembershipCommandResult(missingMembers, noPermissionsMembers)
	}

}

trait RemovesUsersFromEditSchemeMembershipCommand {

	self: EditSchemeMembershipCommandState =>

	def removeUsers(): Unit = {
		excludedStudentIds = (excludedStudentIds.asScala ++ excludeIds.asScala).distinct.asJava
	}
}

trait ResetsMembershipInEditSchemeMembershipCommand {

	self: EditSchemeMembershipCommandState =>

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


trait EditSchemeMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditSchemeMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, scheme)
	}

}

trait EditSchemeMembershipCommandState {

	self: ProfileServiceComponent =>

	def scheme: AttendanceMonitoringScheme
	def user: CurrentUser

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
