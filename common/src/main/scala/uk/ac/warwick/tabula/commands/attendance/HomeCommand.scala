package uk.ac.warwick.tabula.commands.attendance

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, RuntimeMember, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.Public

import scala.collection.JavaConverters._

case class HomeInformation(
	hasProfile: Boolean,
	viewPermissions: Seq[Department],
	managePermissions: Seq[Department],
	allRelationshipTypes: Seq[StudentRelationshipType],
	relationshipTypesMap: Map[StudentRelationshipType, Boolean]
)

object HomeCommand {
	def apply(user: CurrentUser) =
		new HomeCommand(user)
		with Command[HomeInformation]
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringCourseAndRouteServiceComponent
		with AutowiringRelationshipServiceComponent
		with AutowiringAttendanceMonitoringServiceComponent
		with Public with ReadOnly with Unaudited
}

abstract class HomeCommand(val user: CurrentUser) extends CommandInternal[HomeInformation] with HomeCommandState {
	self: ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent with RelationshipServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal(): HomeInformation = {
		val optionalCurrentMember = user.profile
		val currentMember = optionalCurrentMember getOrElse new RuntimeMember(user)
		val hasProfile = currentMember match {
			case student: StudentMember =>
				student.mostSignificantCourseDetails match {
					case Some(scd) => true
					case None => false
				}
			case _ => false
		}

		val viewDepartments = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.View)
		val manageDepartments = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.MonitoringPoints.Manage)

		val viewRoutes = courseAndRouteService.routesWithPermission(user, Permissions.MonitoringPoints.View)

		def withSubDepartments(d: Department) = (Set(d) ++ d.children.asScala.toSet).filter { d =>
			d.routes.asScala.nonEmpty || d.modules.asScala.nonEmpty || attendanceMonitoringService.listAllSchemes(d).nonEmpty
		}

		val allViewDepartments = (viewDepartments ++ viewRoutes.map(_.adminDepartment)).flatMap(withSubDepartments).toSeq.sortBy(_.name)
		val allManageDepartments = manageDepartments.flatMap(withSubDepartments).toSeq.sortBy(_.name)

		// These return Sets so no need to distinct the result

		val allRelationshipTypes = relationshipService.allStudentRelationshipTypes
		val downwardRelationships = relationshipService.listCurrentStudentRelationshipsWithMember(currentMember)
		val relationshipTypesMap = allRelationshipTypes.map { t =>
			(t, downwardRelationships.exists(_.relationshipType == t))
		}.toMap

		HomeInformation(
			hasProfile,
			allViewDepartments,
			allManageDepartments,
			allRelationshipTypes,
			relationshipTypesMap
		)
	}
}

trait HomeCommandState {
	def user: CurrentUser
}
