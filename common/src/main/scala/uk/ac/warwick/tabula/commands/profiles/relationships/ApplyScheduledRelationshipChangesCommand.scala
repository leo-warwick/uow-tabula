package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, AutowiringSecurityServiceComponent, RelationshipServiceComponent}

import scala.collection.JavaConverters._

object ApplyScheduledStudentRelationshipChangesCommand {
	def apply(relationshipType: StudentRelationshipType, department: Department, user: CurrentUser) =
		new ApplyScheduledStudentRelationshipChangesCommandInternal(relationshipType, department, user)
			with ComposableCommand[Seq[StudentRelationship]]
			with AutowiringSecurityServiceComponent
			with AutowiringRelationshipServiceComponent
			with UpdateScheduledStudentRelationshipChangesValidation
			with ApplyScheduledStudentRelationshipChangesDescription
			with UpdateScheduledStudentRelationshipChangesPermissions
			with ManageStudentRelationshipsState
			with UpdateScheduledStudentRelationshipChangesCommandRequest
			with ApplyScheduledStudentRelationshipChangesNotifications
}


class ApplyScheduledStudentRelationshipChangesCommandInternal(val relationshipType: StudentRelationshipType, val department: Department, val user: CurrentUser)
	extends CommandInternal[Seq[StudentRelationship]] {

	self: UpdateScheduledStudentRelationshipChangesCommandRequest with RelationshipServiceComponent =>

	override def applyInternal(): Seq[StudentRelationship] = {
		val applyDate = DateTime.now
		relationships.asScala.map { relationship =>
			if (!relationship.isCurrent) {
				// Future add or replace
				relationshipService.endStudentRelationships(relationship.replacesRelationships.asScala.toSeq, applyDate)
				relationshipService.saveStudentRelationship(
					relationshipType,
					relationship.studentCourseDetails,
					relationship.agentMember.map(Left(_)).getOrElse(Right(relationship.agent)),
					applyDate,
					relationship.replacesRelationships.asScala.toSeq
				)
			} else {
				// Future remove
				relationshipService.endStudentRelationships(Seq(relationship), applyDate)
				relationship
			}
		}
	}

}

trait ApplyScheduledStudentRelationshipChangesDescription extends Describable[Seq[StudentRelationship]] {

	self: ManageStudentRelationshipsState with UpdateScheduledStudentRelationshipChangesCommandRequest =>

	override lazy val eventName = "ApplyScheduledStudentRelationshipChanges"

	override def describe(d: Description) {
		d.studentRelationshipType(relationshipType)
		d.property("relationships", relationships.asScala.map(relationship => Map(
			"studentRelationship" -> relationship.id,
			"studentCourseDetails" -> relationship.studentCourseDetails,
			"agent" -> relationship.agent
		)))
	}
}

trait ApplyScheduledStudentRelationshipChangesNotifications extends BulkRelationshipChangeNotifier[Seq[StudentRelationship], Seq[StudentRelationship]] {

	self: ManageStudentRelationshipsState with UpdateScheduledStudentRelationshipChangesCommandRequest =>

	override def emit(relationships: Seq[StudentRelationship]): Seq[Notification[_, _]] = {
		if (relationships.isEmpty) {
			Seq()
		} else {
			val (removedRelationships, addedRelationships) = relationships.partition(!_.isCurrent)
			sharedEmit(removedRelationships ++ addedRelationships.flatMap(_.replacesRelationships.asScala), addedRelationships)
		}
	}
}


