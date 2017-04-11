package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringRelationshipServiceComponent, RelationshipServiceComponent}

import scala.collection.JavaConverters._

object FetchDepartmentRelationshipsForReallocationCommand {

	def apply(department: Department, relationshipType: StudentRelationshipType, agentId: String) =
		new FetchDepartmentRelationshipInformationCommandInternal(department, relationshipType)
			with AutowiringRelationshipServiceComponent
			with AutowiringProfileServiceComponent
			with ComposableCommand[StudentAssociationResult]
			with FetchDepartmentRelationshipInformationPermissions
			with FetchDepartmentRelationshipsForReallocationCommandState
			with FetchDepartmentRelationshipInformationCommandRequest
			with FetchDepartmentRelationshipInformationCommandBindListener
			with ReadOnly with Unaudited {

			override val agent: String = agentId

		}

}

trait FetchDepartmentRelationshipsForReallocationCommandState extends FetchDepartmentRelationshipInformationCommandState {

	self: FetchDepartmentRelationshipInformationCommandRequest with RelationshipServiceComponent =>

	def agent: String

	lazy val agentEntityData: StudentAssociationEntityData = relationshipService.getStudentAssociationEntityData(
		department,
		relationshipType,
		Option(additionalEntities).map(_.asScala).getOrElse(Seq())
	).find(entityData =>
		entityData.entityId == agent
	).getOrElse(throw new IllegalArgumentException)

	override lazy val dbUnallocated: Seq[StudentAssociationData] = agentEntityData.students

	override def updateDbAllocated(): Unit = {
		dbAllocated = relationshipService.getStudentAssociationEntityData(
			department,
			relationshipType,
			Option(additionalEntities).map(_.asScala).getOrElse(Seq())
		).filterNot(_.entityId == agent)
	}
}
