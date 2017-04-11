package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class ViewAgentsCommandResult(
	agent: String,
	agentMember: Member,
	students: Seq[StudentMember],
	unrecordedCount: Int,
	missedCount: Int
)

object ViewAgentsCommand {
	def apply(department: Department, academicYear: AcademicYear, relationshipType: StudentRelationshipType) =
		new ViewAgentsCommandInternal(department, academicYear, relationshipType)
			with AutowiringRelationshipServiceComponent
			with ComposableCommand[Seq[ViewAgentsCommandResult]]
			with ViewAgentsPermissions
			with ViewAgentsCommandState
			with ReadOnly with Unaudited
}


class ViewAgentsCommandInternal(val department: Department, val academicYear: AcademicYear, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Seq[ViewAgentsCommandResult]] {

	self: RelationshipServiceComponent =>

	override def applyInternal(): Seq[ViewAgentsCommandResult] = {
		val relationships = relationshipService.listCurrentStudentRelationshipsByDepartment(relationshipType, department)
		relationships.groupBy(_.agent).map{case(agent, agentRelationships) =>
			val tutees = agentRelationships.flatMap(_.studentMember)
			val tuteesDepartmentCounts = tutees.flatMap(_.attendanceCheckpointTotals.asScala).filter(t => t.department == department && t.academicYear == academicYear)
			ViewAgentsCommandResult(
				agent,
				agentRelationships.head.agentMember.orNull,
				tutees,
				tuteesDepartmentCounts.map(_.unrecorded).sum,
				tuteesDepartmentCounts.map(_.unauthorised).sum
			)
		}.toSeq
	}

}

trait ViewAgentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewAgentsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, department)
	}

}

trait ViewAgentsCommandState {
	def department: Department
	def academicYear: AcademicYear
	def relationshipType: StudentRelationshipType
}
