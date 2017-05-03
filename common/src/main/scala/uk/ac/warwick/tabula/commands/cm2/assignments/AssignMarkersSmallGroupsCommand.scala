package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

case class SetAllocation(set: SmallGroupSet, allocations: Map[String, Seq[GroupAllocation]])
case class GroupAllocation(name: String, tutors: Seq[User], students: Seq[User])

object AssignMarkersSmallGroupsCommand {
	def apply(assignment: Assignment) = new AssignMarkersSmallGroupsCommandInternal(assignment)
		with ComposableCommand[Seq[SetAllocation]]
		with AssignMarkersSmallGroupsPermissions
		with AutowiringSmallGroupServiceComponent
		with AutowiringAssessmentMembershipServiceComponent
		with Unaudited
}

class AssignMarkersSmallGroupsCommandInternal(val assignment: Assignment) extends CommandInternal[Seq[SetAllocation]]
	with AssignMarkersSmallGroupsState {

	self : SmallGroupServiceComponent with AssessmentMembershipServiceComponent =>

	val module: Module = assignment.module
	val academicYear: AcademicYear = assignment.academicYear

	def applyInternal(): Seq[SetAllocation] = {

		val sets = smallGroupService.getSmallGroupSets(module, academicYear)
		val validStudents = assessmentMembershipService.determineMembershipUsers(assignment)

		val setAllocations = sets.map(set => {

			def getGroupAllocations(markers: Seq[User]): Seq[GroupAllocation] = set.groups.asScala.map(group => {
				val validMarkers = group.events
					.flatMap(_.tutors.users)
					.filter(markers.contains)
					.distinct
				val students = group.students.users.filter(validStudents.contains)
				GroupAllocation(group.name, validMarkers, students)
			})

			val allocations = if(assignment.cm2MarkingWorkflow.workflowType.rolesShareAllocations) {
				assignment.cm2MarkingWorkflow.markers.map{case (s, m) => s.allocationName -> getGroupAllocations(m)}
			} else {
				assignment.cm2MarkingWorkflow.markersByRole.mapValues(getGroupAllocations)
			}

			SetAllocation(set, allocations)
		})

		// if any of a sets allocations have no valid tutors/markers then ignore the set entirely
		setAllocations.filter(_.allocations.values.flatten.toSeq.forall(_.tutors.nonEmpty))
	}
}

trait AssignMarkersSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AssignMarkersSmallGroupsState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, mandatory(assignment))
	}
}

trait AssignMarkersSmallGroupsState {
	val assignment: Assignment
}