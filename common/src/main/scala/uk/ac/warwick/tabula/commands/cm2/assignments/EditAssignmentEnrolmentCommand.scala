package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object EditAssignmentMembershipCommand {
	/*
	 * This is a stub class, which isn't applied, but exposes the student membership (enrolment) for assignment
   * to rebuild views within an existing form
	 */
	def stub(assignment: Assignment) =
		new StubEditAssignmentMembershipCommand(assignment)
			with AutowiringUserLookupComponent
			with AutowiringAssessmentMembershipServiceComponent
			with ComposableCommand[Assignment]
			with ModifiesAssignmentMembership
			with StubEditAssignmentMembershipPermissions
			with Unaudited with ReadOnly

}


trait EditAssignmentMembershipCommandState extends CurrentSITSAcademicYear {
	def assignment: Assignment

	def module: Module = assignment.module
}


class StubEditAssignmentMembershipCommand(val assignment: Assignment, val updateStudentMembershipGroupIsUniversityIds: Boolean = false) extends CommandInternal[Assignment]
	with EditAssignmentMembershipCommandState {
	self: ModifiesAssignmentMembership with UserLookupComponent with AssessmentMembershipServiceComponent =>

	override def applyInternal() = throw new UnsupportedOperationException
}

trait StubEditAssignmentMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditAssignmentMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheckAny(Seq(CheckablePermission(Permissions.Assignment.Create, assignment.module),
			CheckablePermission(Permissions.Assignment.Update, assignment.module)))
	}
}

trait ModifiesAssignmentMembership extends UpdatesStudentMembership with SpecifiesGroupType {
	self: EditAssignmentMembershipCommandState with HasAcademicYear with UserLookupComponent with AssessmentMembershipServiceComponent =>

	lazy val existingGroups: Option[Seq[UpstreamAssessmentGroup]] = Option(assignment).map {
		_.upstreamAssessmentGroups
	}
	lazy val existingMembers: Option[UnspecifiedTypeUserGroup] = None

	def copyMembers(assignment: Assignment) {
		if (assignment.members != null) {
			members.copyFrom(assignment.members)
		}

	}

	def updateAssessmentGroups() {
		assessmentGroups = upstreamGroups.asScala.flatMap(ug => {
			val template = new AssessmentGroup
			template.assessmentComponent = ug.assessmentComponent
			template.occurrence = ug.occurrence
			template.assignment = assignment
			assessmentMembershipService.getAssessmentGroup(template).orElse(Some(template))
		}).distinct.asJava
	}
}







