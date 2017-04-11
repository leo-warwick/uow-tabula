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
class MarkerRoleProvider extends RoleProvider with TaskBenchmarking with RequestLevelCaching[(CurrentUser, String), Seq[Assessment]] {

	val assignmentService: MutablePromise[AssessmentService] = promise { Wire[AssessmentService] }

	def getRolesFor(user: CurrentUser, scope: PermissionsTarget): Stream[Role] = benchmarkTask("Get roles for MarkerRoleProvider") {
		def getRoles(assessmentsForMarker: Stream[Assessment]) = assessmentsForMarker.map{ assessment =>
			customRoleFor(assessment.module.adminDepartment)(MarkerRoleDefinition, assessment).getOrElse(Marker(assessment))
		}

		scope match {
			case department: Department =>
				getRoles(cachedBy((user, scope.toString)) {
					assignmentService.get.getAssignmentsByDepartmentAndMarker(department, user)
				}.toStream)

			case module: Module =>
				getRoles(cachedBy((user, scope.toString)) {
					assignmentService.get.getAssignmentsByModuleAndMarker(module, user).toStream
				}.toStream)

			case assessment: Assessment if assessment.isMarker(user.apparentUser) =>
				getRoles(Stream(assessment))

			// We don't need to check for the marker role on any other scopes
			case _ => Stream.empty
		}
	}

	def rolesProvided = Set(classOf[Marker])

}