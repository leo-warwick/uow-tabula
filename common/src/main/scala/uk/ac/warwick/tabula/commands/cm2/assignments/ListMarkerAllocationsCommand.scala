package uk.ac.warwick.tabula.commands.cm2.assignments

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.ListMarkerAllocationsCommand._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.Allocations
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, CM2MarkingWorkflowServiceComponent, UserLookupComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.SortedSet
import scala.collection.mutable


case class MarkerAllocations(
  allStudents: SortedSet[Student],
  keys: List[String], // ordered keys for all of the other maps - getting freemarker to iterate across maps in order is dumb
  unallocatedStudents: Map[String, SortedSet[Student]],
  allocations: Map[String, Map[Marker, SortedSet[Student]]],
  markers: Map[String, SortedSet[Marker]],
  allocateByStage: Map[String, Boolean]
)

trait FetchMarkerAllocations {
  self: UserLookupComponent with CM2MarkingWorkflowServiceComponent
    with AssessmentMembershipServiceComponent =>

  import ListMarkerAllocationsCommand.userOrdering

  def fetchAllocations(assignment: Assignment, selectedAllocations: Map[MarkingWorkflowStage, Allocations] = Map.empty): MarkerAllocations = {

    // If seat number is empty we use the uni ID instead. This will be larger than a real seat number and unique
    def seatNumberOrUniId = (user: User) => assignment.getSeatNumber(user) match {
      case Some(seatNo) => (seatNo, user.getWarwickId.toInt)
      case _ => (0, user.getWarwickId.toInt)
    }

    val workflow = assignment.cm2MarkingWorkflow
    val stagesByRole = workflow.markerStages.groupBy(_.roleName)
    val users: Seq[User] = assessmentMembershipService.determineMembershipUsers(assignment)
    val allStudents = if (assignment.showSeatNumbers) {
      SortedSet(users: _*)(Ordering.by(seatNumberOrUniId))
    } else {
      SortedSet(users: _*)
    }

    val allocations = mutable.Map[String, Map[Marker, SortedSet[Student]]]()
    val markers = mutable.Map[String, SortedSet[Marker]]()
    val allocateByStage = mutable.Map[String, Boolean]()

    def toSortedSet(allocations: Allocations) = allocations.map { case (marker, students) =>
      marker -> (if (assignment.showSeatNumbers) {
        SortedSet(students.toSeq: _*)(Ordering.by(seatNumberOrUniId))
      } else {
        SortedSet(students.toSeq: _*)
      })
    }

    for {
      (role, stages) <- stagesByRole
      stage <- stages
    } if (workflow.workflowType.rolesShareAllocations) {
      // key by role as the allocations are the same
      allocations += role -> toSortedSet(selectedAllocations.getOrElse(MarkingWorkflowStage.fromCode(role, workflow.workflowType), cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage)))
      markers += role -> SortedSet(workflow.markers(stage): _*)
      allocateByStage += role -> false
    } else {
      allocations += stage.allocationName -> toSortedSet(selectedAllocations.getOrElse(stage, cm2MarkingWorkflowService.getMarkerAllocations(assignment, stage)))
      markers += stage.allocationName -> SortedSet(workflow.markers(stage): _*)
      allocateByStage += stage.allocationName -> stage.stageAllocation
    }

    val unallocatedStudents = allocations.map { case (key, allocation) =>
      val allocated = allocation.view.filterKeys(_.isFoundUser).values.flatten.toSet
      // BEWARE - Don't try and use "allStudents -- allocated" or  "allStudents.diff(allocated)" here - some stupid Heron magic prevents that from working.
      // Probably some hashCode equality nonsense that I couldn't figure out - allStudents are Member.asSsoUser(s) but I think hashCode and equals should
      // compare with regular SSO Users just fine ¯\_(ツ)_/¯
      key -> allStudents.filterNot(allocated)
    }

    MarkerAllocations(
      allStudents = allStudents,
      keys = workflow.allocationOrder,
      unallocatedStudents = unallocatedStudents.toMap,
      allocations = allocations.toMap,
      markers = markers.toMap,
      allocateByStage = allocateByStage.toMap
    )
  }
}

object ListMarkerAllocationsCommand {

  type Student = User
  type Marker = User

  implicit val userOrdering: Ordering[User] = Ordering.by { u: User => (u.getLastName, u.getFirstName, u.getWarwickId, u.getUserId) }

  def apply(assignment: Assignment, allocations: Map[MarkingWorkflowStage, Allocations] = Map.empty) = new ListMarkerAllocationsCommandInternal(assignment, allocations)
    with ComposableCommand[MarkerAllocations]
    with ListMarkerAllocationsPermissions
    with Unaudited
    with ReadOnly
    with AutowiringUserLookupComponent
    with AutowiringCM2MarkingWorkflowServiceComponent
    with AutowiringAssessmentMembershipServiceComponent
}

class ListMarkerAllocationsCommandInternal(val assignment: Assignment, allocations: Map[MarkingWorkflowStage, Allocations]) extends CommandInternal[MarkerAllocations]
  with ListMarkerAllocationsState with FetchMarkerAllocations {

  this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent =>

  def applyInternal(): MarkerAllocations = fetchAllocations(assignment, allocations)
}

trait ListMarkerAllocationsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListMarkerAllocationsState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
  }
}

trait ListMarkerAllocationsState {
  val assignment: Assignment
}
