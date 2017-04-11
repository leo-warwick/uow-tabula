package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent, SortableAgentIdentifier}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.immutable.TreeMap

object ViewStudentRelationshipsCommand {

	case class Result(
		studentMap: TreeMap[SortableAgentIdentifier, Seq[StudentRelationship]],
		missingCount: Int,
		scheduledCount: Int,
		courseMap: Map[StudentCourseDetails, Course],
		yearOfStudyMap: Map[StudentCourseDetails, Int]
	)
	
	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new ViewStudentRelationshipsCommandInternal(department, relationshipType)
			with ComposableCommand[ViewStudentRelationshipsCommand.Result]
			with AutowiringRelationshipServiceComponent
			with ViewStudentRelationshipsPermissions
			with ViewStudentRelationshipsCommandState
			with ReadOnly with Unaudited
}


class ViewStudentRelationshipsCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[ViewStudentRelationshipsCommand.Result] with TaskBenchmarking {

	self: RelationshipServiceComponent =>

	override def applyInternal(): ViewStudentRelationshipsCommand.Result = {
		// get all agent/student relationships by dept
		val sortedAgentRelationships = relationshipService.listAgentRelationshipsByDepartment(relationshipType, department)

		val departmentStudentsWithoutAgentCount = relationshipService.listStudentsWithoutCurrentRelationship(relationshipType, department).distinct.size

		val departmentStudentsWitScheduledChangesCount = relationshipService.listScheduledRelationshipChanges(relationshipType, department)
			.distinct.count(_.replacedBy == null)

		val courseMap: Map[StudentCourseDetails, Course] = benchmarkTask("courseDetails") {
			relationshipService.coursesForStudentCourseDetails(sortedAgentRelationships.values.flatten.map(_.studentCourseDetails).toSeq)
		}

		val yearOfStudyMap: Map[StudentCourseDetails, Int] = benchmarkTask("yearsOfStudy") {
			relationshipService.latestYearsOfStudyForStudentCourseDetails(sortedAgentRelationships.values.flatten.map(_.studentCourseDetails).toSeq)
		}

		ViewStudentRelationshipsCommand.Result(
			sortedAgentRelationships,
			departmentStudentsWithoutAgentCount,
			departmentStudentsWitScheduledChangesCount,
			courseMap,
			yearOfStudyMap
		)
	}

}

trait ViewStudentRelationshipsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewStudentRelationshipsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)
	}

}

trait ViewStudentRelationshipsCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType
}
