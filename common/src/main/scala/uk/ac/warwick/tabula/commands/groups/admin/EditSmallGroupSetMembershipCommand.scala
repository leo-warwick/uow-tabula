package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.RemoveUserFromSmallGroupCommand
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._

object EditSmallGroupSetMembershipCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new EditSmallGroupSetMembershipCommandInternal(module, set)
			with AutowiringUserLookupComponent
			with AutowiringAssessmentMembershipServiceComponent
			with ComposableCommand[SmallGroupSet]
			with SmallGroupAutoDeregistration
			with RemovesUsersFromGroupsCommand
			with ModifiesSmallGroupSetMembership
			with EditSmallGroupSetMembershipPermissions
			with EditSmallGroupSetMembershipDescription
			with EditSmallGroupSetMembershipValidation
			with AutowiringSmallGroupServiceComponent
			with PopulateStateWithExistingData

	/*
	 * This is a stub class, which isn't applied, but exposes the student membership (enrolment) for groups
   * to rebuild views within an existing form
	 */
	def stub(module: Module, set: SmallGroupSet) =
		new StubEditSmallGroupSetMembershipCommand(module, set)
			with AutowiringUserLookupComponent
			with AutowiringAssessmentMembershipServiceComponent
			with ComposableCommand[SmallGroupSet]
			with ModifiesSmallGroupSetMembership
			with StubEditSmallGroupSetMembershipPermissions
			with Unaudited with ReadOnly
			with PopulateStateWithExistingData
}

trait PopulateStateWithExistingData {
	self: EditSmallGroupSetMembershipCommandState with ModifiesSmallGroupSetMembership =>

	// linked assessmentGroups
	assessmentGroups = set.assessmentGroups
	if (set.members != null) members.copyFrom(set.members)
	academicYear = set.academicYear
}

class EditSmallGroupSetMembershipCommandInternal(val module: Module, val set: SmallGroupSet, val updateStudentMembershipGroupIsUniversityIds: Boolean = true) extends CommandInternal[SmallGroupSet] with EditSmallGroupSetMembershipCommandState {
	self: SmallGroupServiceComponent with SmallGroupAutoDeregistration with ModifiesSmallGroupSetMembership with UserLookupComponent with AssessmentMembershipServiceComponent =>

	def copyTo(set: SmallGroupSet) {
		set.assessmentGroups.clear()
		set.assessmentGroups.addAll(assessmentGroups)
		for (group <- set.assessmentGroups.asScala if group.smallGroupSet == null) {
			group.smallGroupSet = set
		}

		if (set.members == null) set.members = UserGroup.ofUniversityIds
		set.members.copyFrom(members)
	}

	override def applyInternal(): SmallGroupSet = transactional() {
		if (module.adminDepartment.autoGroupDeregistration) {
			autoDeregister { () =>
				copyTo(set)
				set
			}
		} else {
			copyTo(set)
		}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait EditSmallGroupSetMembershipCommandState extends CurrentSITSAcademicYear {
	def module: Module
	def set: SmallGroupSet
}

trait ModifiesSmallGroupSetMembership extends UpdatesStudentMembership with SpecifiesGroupType {
	self: EditSmallGroupSetMembershipCommandState with UserLookupComponent with AssessmentMembershipServiceComponent =>

	// start complicated membership stuff

	lazy val existingGroups: Option[Seq[UpstreamAssessmentGroup]] = Option(set).map { _.upstreamAssessmentGroups }
	lazy val existingMembers: Option[UnspecifiedTypeUserGroup] = Option(set).map { _.members }

	def copyGroupsFrom(smallGroupSet: SmallGroupSet) {
		// TAB-4848 get all the groups that are linked even if they're marked not in use
		upstreamGroups.addAll(allUpstreamGroups.filter { ug =>
			assessmentGroups.asScala.exists( ag => ug.assessmentComponent == ag.assessmentComponent && ag.occurrence == ug.occurrence )
		}.asJavaCollection)
	}

	/**
	 * Convert Spring-bound upstream group references to an AssessmentGroup buffer
	 */
	def updateAssessmentGroups() {
		assessmentGroups = upstreamGroups.asScala.flatMap ( ug => {
			val template = new AssessmentGroup
			template.assessmentComponent = ug.assessmentComponent
			template.occurrence = ug.occurrence
			template.smallGroupSet = set
			assessmentMembershipService.getAssessmentGroup(template) orElse Some(template)
		}).distinct.asJava
	}

	// end of complicated membership stuff
}

trait SmallGroupAutoDeregistration {
	self: AssessmentMembershipServiceComponent with EditSmallGroupSetMembershipCommandState with ModifiesSmallGroupSetMembership with RemovesUsersFromGroups =>

	def autoDeregister(fn: () => SmallGroupSet): SmallGroupSet = {
		val oldUsers =
			assessmentMembershipService.determineMembershipUsers(set.upstreamAssessmentGroups, Option(set.members)).toSet

		val newUsers =
			assessmentMembershipService.determineMembershipUsers(linkedUpstreamAssessmentGroups, Option(members)).toSet

		val updatedSet = fn()

		// Wrap removal in a sub-command so that we can do auditing
		for {
			user <- oldUsers -- newUsers
			group <- updatedSet.groups.asScala
			if (group.students.includesUser(user))
		} removeFromGroup(user, group)

		updatedSet
	}
}

trait EditSmallGroupSetMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupSetMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupSetMembershipDescription extends Describable[SmallGroupSet] {
	self: EditSmallGroupSetMembershipCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}

trait EditSmallGroupSetMembershipValidation extends SelfValidating {
	self: EditSmallGroupSetMembershipCommandState =>

	override def validate(errors: Errors) {
		if (set.allocationMethod == SmallGroupAllocationMethod.Linked) {
			errors.reject("smallGroupSet.linked")
		}
	}
}

class StubEditSmallGroupSetMembershipCommand(val module: Module, val set: SmallGroupSet, val updateStudentMembershipGroupIsUniversityIds: Boolean = true) extends CommandInternal[SmallGroupSet] with EditSmallGroupSetMembershipCommandState {
	self: ModifiesSmallGroupSetMembership with UserLookupComponent with AssessmentMembershipServiceComponent =>

	override def applyInternal() = throw new UnsupportedOperationException
}

trait StubEditSmallGroupSetMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupSetMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(Seq(
			CheckablePermission(Permissions.SmallGroups.Create, mandatory(module)),
			CheckablePermission(Permissions.SmallGroups.Update, mandatory(module))
		))
	}
}

trait RemovesUsersFromGroups {
	def removeFromGroup(user: User, group: SmallGroup)
}

trait RemovesUsersFromGroupsCommand extends RemovesUsersFromGroups {
	def removeFromGroup(user: User, group: SmallGroup): Unit = new RemoveUserFromSmallGroupCommand(user, group).apply()
}