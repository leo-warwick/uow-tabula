package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.helpers.{LazyLists, LazyMaps}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty
import scala.jdk.CollectionConverters._

case class SetAllocation(set: SmallGroupSet, allocations: Map[String, Seq[GroupAllocation]])

case class GroupAllocation(group: SmallGroup, name: String, tutors: Seq[User], students: Seq[User], otherTutors: Seq[User])

class GroupMarkerAllocation {
  @BeanProperty var group: SmallGroup = _
  @BeanProperty var stages: JList[MarkingWorkflowStage] = _
  @BeanProperty var marker: User = _
}

object GroupMarkerAllocation {
  def apply(group: SmallGroup, stage: MarkingWorkflowStage, marker: User): GroupMarkerAllocation = {
    val allocation = new GroupMarkerAllocation
    allocation.group = group
    allocation.stages = JArrayList(stage)
    allocation.marker = marker
    allocation
  }
}

object AssignMarkersSmallGroupsCommand {
  def apply(assignment: Assignment) = new AssignMarkersSmallGroupsCommandInternal(assignment)
    with ComposableCommand[Assignment]
    with AssignMarkersSmallGroupsState
    with AssignMarkersSmallGroupsPermissions
    with AutowiringSmallGroupServiceComponent
    with AutowiringAssessmentMembershipServiceComponent
    with Unaudited
    with AssignMarkersSmallGroupsCommandRequest
    with AssignMarkersSmallGroupsCommandPopulate
    with AssignMarkersSmallGroupsValidation
}

class AssignMarkersSmallGroupsCommandInternal(val assignment: Assignment) extends CommandInternal[Assignment] {
  self: SmallGroupServiceComponent with AssessmentMembershipServiceComponent with AssignMarkersSmallGroupsState with AssignMarkersSmallGroupsCommandRequest =>

  def applyInternal(): Assignment = {

    val command: Appliable[Assignment] with AssignMarkersState = AssignMarkersBySmallGroupsCommand(assignment, markerAllocations)

    command.allowSameMarkerForSequentialStages = allowSameMarkerForSequentialStages

    command.apply()
  }
}

trait AssignMarkersSmallGroupsValidation extends SelfValidating {
  self: AssignMarkersSmallGroupsState with AssignMarkersSmallGroupsCommandRequest =>
  override def validate(errors: Errors): Unit = {
    val command: SelfValidating with AssignMarkersState = AssignMarkersBySmallGroupsCommand(assignment, markerAllocations)

    command.allowSameMarkerForSequentialStages = allowSameMarkerForSequentialStages

    command.validate(errors)

    allocationWarnings = command.allocationWarnings
  }
}

trait AssignMarkersSmallGroupsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AssignMarkersSmallGroupsState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    notDeleted(assignment)

    p.PermissionCheck(Permissions.SmallGroups.ReadMembership, mandatory(assignment))
    p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
  }
}

trait AssignMarkersSmallGroupsCommandPopulate extends PopulateOnForm {
  self: AssignMarkersSmallGroupsState with AssignMarkersSmallGroupsCommandRequest with SmallGroupServiceComponent with AssessmentMembershipServiceComponent =>

  val module: Module = assignment.module
  val academicYear: AcademicYear = assignment.academicYear

  override def populate(): Unit = {
    val sets = smallGroupService.getSmallGroupSets(module, academicYear)
    val validStudents = assessmentMembershipService.determineMembershipUsers(assignment)

    val setAllocations = sets.map(set => {
      def getGroupAllocations(markers: Seq[User]): Seq[GroupAllocation] = {
        val validMarkers: Seq[User] = (for {
          group <- set.groups.asScala.toSeq
          event <- group.events
          user <- event.tutors.users if markers.contains(user)
        } yield user).distinct

        val groupAllocations = set.groups.asScala.toSeq.map(group => {
          val students = group.students.users.filter(validStudents.contains).toSeq.sortBy { u => (u.getLastName, u.getFirstName) }
          val markers = group.events.flatMap(_.tutors.users).filter(validMarkers.contains)
          val otherMarkers = validMarkers.diff(markers)
          GroupAllocation(group, group.name, markers, students, otherMarkers)
        })

        // ignore groups that don't have valid tutor/markers
        groupAllocations.filter(_.students.nonEmpty)
      }

      val allocations = assignment.cm2MarkingWorkflow.markers.map { case (s, m) => s.allocationName -> getGroupAllocations(m) }

      SetAllocation(set, allocations)
    })

    // if any of a sets allocations have no valid tutors/markers then ignore the set entirely
    val filteredSetAllocations: Seq[SetAllocation] = setAllocations.filter(_.allocations.values.flatten.toSeq.forall(_.tutors.nonEmpty))

    this.setAllocations = filteredSetAllocations
    if (setIdWithGroupMarkerAllocations.isEmpty) {
      setIdWithGroupMarkerAllocations = filteredSetAllocations.map { setAllocation =>
        setAllocation.set.id  -> setAllocation.allocations.flatMap { case (stageName, groups) =>
          groups.map {group =>
            GroupMarkerAllocation(group.group, MarkingWorkflowStage.fromCode(stageName, assignment.cm2MarkingWorkflow.workflowType), group.tutors.headOption.orNull)
          }
        }.toSeq.asJava
      }.toMap.asJava
    }
  }
}

trait AssignMarkersSmallGroupsState {
  val assignment: Assignment
  var setAllocations: Seq[SetAllocation] = Nil
}

trait AssignMarkersSmallGroupsCommandRequest extends AssignMarkersSequentialStageValidationState {
  var smallGroupSet: SmallGroupSet = _
  var setIdWithGroupMarkerAllocations: JMap[String, JList[GroupMarkerAllocation]] =
    LazyMaps.create { _: String =>
      LazyLists.create(): JList[GroupMarkerAllocation]
    }.asJava
  def markerAllocations: Seq[GroupMarkerAllocation] = setIdWithGroupMarkerAllocations.get(smallGroupSet.id).asScala.toSeq
}

