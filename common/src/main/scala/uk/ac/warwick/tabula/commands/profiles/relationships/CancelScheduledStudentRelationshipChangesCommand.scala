package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles._
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, AutowiringSecurityServiceComponent, RelationshipServiceComponent}

import scala.collection.JavaConverters._

object CancelScheduledStudentRelationshipChangesCommand {
	trait Common extends CommandInternal[Seq[StudentRelationship]]
		with ComposableCommand[Seq[StudentRelationship]]
		with AutowiringRelationshipServiceComponent
		with AutowiringSecurityServiceComponent
		with UpdateScheduledStudentRelationshipChangesValidation
		with CancelScheduledStudentRelationshipChangesDescription
		with UpdateScheduledStudentRelationshipChangesPermissions
		with ManageStudentRelationshipsState
		with UpdateScheduledStudentRelationshipChangesCommandRequest
		with CancelScheduledStudentRelationshipChangesCommandNotifications

	def apply(relationshipType: StudentRelationshipType, department: Department, user: CurrentUser) =
		new CancelScheduledStudentRelationshipChangesCommandInternal(relationshipType, department, user) with Common

	def applyForRelationship(relationshipType: StudentRelationshipType, department: Department, user: CurrentUser, relationship: StudentRelationship) =
		new CancelScheduledStudentRelationshipChangesCommandInternal(relationshipType, department, user) with Common {
			relationships = JArrayList(relationship)
		}
}


class CancelScheduledStudentRelationshipChangesCommandInternal(val relationshipType: StudentRelationshipType, val department: Department, val user: CurrentUser)
	extends CommandInternal[Seq[StudentRelationship]] {

	self: RelationshipServiceComponent with ManageStudentRelationshipsState
		with UpdateScheduledStudentRelationshipChangesCommandRequest =>

	override def applyInternal(): Seq[StudentRelationship] = {
		relationships.asScala.map(relationship =>
			if (relationship.startDate.isAfterNow) {
				if (!relationship.replacesRelationships.isEmpty) {
					previouslyReplacedRelationships.put(relationship, Set(relationship.replacesRelationships.asScala.toSeq:_*))
				}
				relationshipService.removeFutureStudentRelationships(Seq(relationship))
				relationship
			} else {
				relationship.endDate = null
				relationshipService.saveOrUpdate(relationship)
				relationship
			}
		)
	}

}

trait CancelScheduledStudentRelationshipChangesDescription extends Describable[Seq[StudentRelationship]] {

	self: ManageStudentRelationshipsState with UpdateScheduledStudentRelationshipChangesCommandRequest =>

	override lazy val eventName = "CancelScheduledStudentRelationshipChanges"

	override def describe(d: Description) {
		d.studentRelationshipType(relationshipType)
		d.property("relationships", relationships.asScala.map(relationship => Map(
			"studentRelationship" -> relationship.id,
			"studentCourseDetails" -> relationship.studentCourseDetails,
			"agent" -> relationship.agent,
			"action" -> (if (relationship.isCurrent) "cancelRemove" else "cancelAdd")
		)))
	}
}

trait CancelScheduledStudentRelationshipChangesCommandNotifications extends Notifies[Seq[StudentRelationship], StudentRelationship] {

	self: ManageStudentRelationshipsState with UpdateScheduledStudentRelationshipChangesCommandRequest =>

	// If only one relationship has changed show the replacement details
	private def singleRelationshipNotificationDetails(notification: CancelledStudentRelationshipChangeNotification, relationship: StudentRelationship): Unit = {
		relationship.studentMember.foreach(notification.student = _)
		notification.relationshipType = relationship.relationshipType
		if (relationship.startDate.isAfterNow) {
			if (previouslyReplacedRelationships.getOrElse(relationship, Seq()).nonEmpty) {
				// Cancelled replace
				notification.cancelledRemovalsIds.value = previouslyReplacedRelationships(relationship).map(_.agent).toSeq
				notification.cancelledAdditionsIds.value = Seq(relationship.agent)
				notification.scheduledDate = scheduledDate.get
			} else {
				// Cancelled add
				notification.cancelledAdditionsIds.value = Seq(relationship.agent)
				notification.scheduledDate = scheduledDate.get
			}
		} else {
			// Cancelled remove
			notification.cancelledRemovalsIds.value = Seq(relationship.agent)
			notification.scheduledDate = scheduledDate.get
		}
	}

	/**
		* For each student that one has or more change cancelled send a single notification showing the removals and additions no longer happening.
		*
		* If an agent has only 1 change cancelled (either replace, add, or remove) send a single notification that shows the change,
		* including the associated relationship if it was a replacement.
		*
		* If an agent has multiple changes cancelled send a single notification show all the removals and additions separately.
		*/
	def emit(relationships: Seq[StudentRelationship]): Seq[Notification[StudentRelationship, Unit]] = {
		if (relationships.isEmpty) {
			Seq()
		} else {
			// Group the cancelled relationships by student then send a notificaion to each, including the relevant removals and additions (shown as 2 different lists)
			val studentNotifications = if (notifyStudent) {
				relationships.groupBy(_.studentCourseDetails.student).map { case (student, rels) =>
					val notification = Notification.init(new CancelledStudentRelationshipChangeToStudentNotification, user.apparentUser, Seq[StudentRelationship]())
					notification.student = student
					notification.relationshipType = relationshipType
					notification.scheduledDate = scheduledDate.get
					val (removals, additions) = (rels ++ rels.flatMap(previouslyReplacedRelationships.get).flatten).partition(_.isCurrent)
					notification.cancelledRemovalsIds.value = removals.map(_.agent)
					notification.cancelledAdditionsIds.value = additions.map(_.agent)
					notification
				}
			} else {
				Seq()
			}

			// All relationships that were previously going to remove an agent
			// Includes those directly provided (a removal) or those that were previously replaced
			val cancelledRemovalsByOldAgent = (relationships.filter(_.isCurrent) ++ relationships.flatMap(previouslyReplacedRelationships.get).flatten).groupBy(_.agentMember.get)
			// All relationships that were previously going to add an agent
			val cancelledAdditionsByNewAgent = relationships.filter(!_.isCurrent).groupBy(_.agentMember.get)

			val agentNotifications = if (notifyOldAgent || notifyNewAgent) {
				// Providing we should email any agent at all, find all the agents that have some kind of change
				val allAgents = cancelledAdditionsByNewAgent.keySet ++ cancelledRemovalsByOldAgent.keySet
				allAgents.flatMap { agent =>
					// For each agent, what kind of change do they have (can be both)
					val removed = cancelledRemovalsByOldAgent.keySet.contains(agent)
					val added = cancelledAdditionsByNewAgent.keySet.contains(agent)

					if (removed && added) {
						// If it's both, it's probably more than one student, so we must send the bulk notification (showing the removals and additions as 2 separate lists)
						val notification = Notification.init(new CancelledBulkStudentRelationshipChangeToAgentNotification, user.apparentUser, Seq[StudentRelationship]())
						notification.recipientUniversityId = agent.universityId
						notification.relationshipType = relationshipType
						notification.scheduledDate = scheduledDate.get
						notification.cancelledRemovalsIds.value = cancelledRemovalsByOldAgent(agent).flatMap(_.studentMember).map(_.universityId)
						notification.cancelledAdditionsIds.value = cancelledAdditionsByNewAgent(agent).flatMap(_.studentMember).map(_.universityId)
						Option(notification)

					} else if (removed && notifyOldAgent) {
						// If it's just a removal (and we should notify such agents)...
						if (cancelledRemovalsByOldAgent(agent).size == 1) {
							// If there's only one chnage we can show the cancelled replacement (if it exists)
							val notification = Notification.init(new CancelledStudentRelationshipChangeToOldAgentNotification, user.apparentUser, Seq[StudentRelationship]())
							// If the relationship was passed in, use that one. Otherwise use the one that previously replaced it
							val relationship = if (relationships.contains(cancelledRemovalsByOldAgent(agent).head)) {
								cancelledRemovalsByOldAgent(agent).head
							} else {
								previouslyReplacedRelationships.find { case (_, rels) => rels.contains(cancelledRemovalsByOldAgent(agent).head) }.get._1
							}
							singleRelationshipNotificationDetails(notification, relationship)
							Option(notification)
						} else {
							// There's more than one student, so we must send the bulk notification
							val notification = Notification.init(new CancelledBulkStudentRelationshipChangeToAgentNotification, user.apparentUser, Seq[StudentRelationship]())
							notification.recipientUniversityId = agent.universityId
							notification.relationshipType = relationshipType
							notification.scheduledDate = scheduledDate.get
							notification.cancelledRemovalsIds.value = cancelledRemovalsByOldAgent(agent).flatMap(_.studentMember).map(_.universityId)
							Option(notification)
						}
					} else if (added && notifyNewAgent) {
						// If it's just an addition (and we should notify such agents)...
						if (cancelledAdditionsByNewAgent(agent).size == 1) {
							// If there's only one change we can show the cancelled replacement (if it exists)
							val notification = Notification.init(new CancelledStudentRelationshipChangeToNewAgentNotification, user.apparentUser, Seq[StudentRelationship]())
							singleRelationshipNotificationDetails(notification, cancelledAdditionsByNewAgent(agent).head)
							Option(notification)
						} else {
							// There's more than one student, so we must send the bulk notification
							val notification = Notification.init(new CancelledBulkStudentRelationshipChangeToAgentNotification, user.apparentUser, Seq[StudentRelationship]())
							notification.recipientUniversityId = agent.universityId
							notification.relationshipType = relationshipType
							notification.scheduledDate = scheduledDate.get
							notification.cancelledAdditionsIds.value = cancelledAdditionsByNewAgent(agent).flatMap(_.studentMember).map(_.universityId)
							Option(notification)
						}
					} else {
						None
					}
				}
			} else {
				Seq()
			}

			studentNotifications ++ agentNotifications

		}.toSeq
	}
}


