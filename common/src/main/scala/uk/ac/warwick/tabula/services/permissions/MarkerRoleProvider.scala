package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Promises._
import uk.ac.warwick.tabula.helpers.{MutablePromise, RequestLevelCaching}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.{Marker, MarkerRoleDefinition, Role}
import uk.ac.warwick.tabula.services.AssessmentService

@Component
class MarkerRoleProvider extends RoleProvider with TaskBenchmarking {

  val assignmentService: MutablePromise[AssessmentService] = promise {
    Wire[AssessmentService]
  }

  def getRolesFor(user: CurrentUser, scope: PermissionsTarget): LazyList[Role] = benchmarkTask("Get roles for MarkerRoleProvider") {
    def getRoles(assessmentsForMarker: LazyList[Assessment]) = assessmentsForMarker.map { assessment =>
      customRoleFor(assessment.module.adminDepartment)(MarkerRoleDefinition, assessment).getOrElse(Marker(assessment))
    }

    scope match {
      case assignment: Assignment if assignment.cm2Assignment && Option(assignment.cm2MarkingWorkflow).exists(_.allMarkers.contains(user.apparentUser)) =>
        getRoles(LazyList(assignment))

      case assignment: Assignment if !assignment.cm2Assignment && assignment.isMarker(user.apparentUser) =>
        getRoles(LazyList(assignment))

      case exam: Exam if exam.isMarker(user.apparentUser) =>
        getRoles(LazyList(exam))

      // We don't need to check for the marker role on any other scopes
      case _ => LazyList.empty
    }
  }

  def rolesProvided: Set[Class[_ <: Role]] = Set(classOf[Marker])

}
