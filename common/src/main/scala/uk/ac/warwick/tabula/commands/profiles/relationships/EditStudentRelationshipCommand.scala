package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.{StudentRelationshipChangeToNewAgentNotification, StudentRelationshipChangeToOldAgentNotification, StudentRelationshipChangeToStudentNotification}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException}

object EditStudentRelationshipCommand {
	def apply(studentCourseDetails: StudentCourseDetails, relationshipType: StudentRelationshipType, user: CurrentUser) =
		new EditStudentRelationshipCommandInternal(studentCourseDetails, relationshipType, user)
			with ComposableCommand[Seq[StudentRelationship]]
			with AutowiringRelationshipServiceComponent
			with EditStudentRelationshipValidation
			with EditStudentRelationshipDescription
			with EditStudentRelationshipPermissions
			with EditStudentRelationshipCommandState
			with EditStudentRelationshipCommandRequest
			with EditStudentRelationshipCommandNotifications
}


class EditStudentRelationshipCommandInternal(
	val studentCourseDetails: StudentCourseDetails,
	val relationshipType: StudentRelationshipType,
	val user: CurrentUser
)	extends CommandInternal[Seq[StudentRelationship]] with Logging {

	self: EditStudentRelationshipCommandRequest with RelationshipServiceComponent =>

	override def applyInternal(): Seq[StudentRelationship] = {
		// Throw this request out if the relationship can't be edited in Tabula for this department
		if (relationshipType.readOnly(studentCourseDetails.department)) {
			logger.info("Denying access to EditStudentRelationshipCommand since relationship %s is read-only".format(relationshipType))
			throw new ItemNotFoundException()
		}

		if (Option(oldAgent).isEmpty) {
			val newRelationship = relationshipService.saveStudentRelationship(relationshipType, studentCourseDetails, Left(newAgent), scheduledDateToUse)
			Seq(newRelationship)
		} else if (removeAgent) {
			// remove the relationship for the specified agent and return the ended relationship
			val preexistingRelationships = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails)

			val relationshipsToRemove = preexistingRelationships.filter(_.agentMember.contains(oldAgent))
			relationshipService.endStudentRelationships(relationshipsToRemove, scheduledDateToUse)
			relationshipsToRemove
		}	else if (oldAgent != newAgent) {
			// we've been given a new agent -
			// replace the current agents with the new one and return the new relationship
			val relationshipsToReplace = relationshipService.findCurrentRelationships(relationshipType, studentCourseDetails).filter(rel => rel.agentMember.contains(oldAgent))
			relationshipService.endStudentRelationships(relationshipsToReplace, scheduledDateToUse)
			relationshipsToReplace ++ Seq(relationshipService.saveStudentRelationship(relationshipType, studentCourseDetails, Left(newAgent), scheduledDateToUse, relationshipsToReplace))
		} else {
			Seq()
		}
	}

}

trait EditStudentRelationshipValidation extends SelfValidating {

	self: EditStudentRelationshipCommandRequest =>

	override def validate(errors: Errors) {
		if (oldAgent == null && newAgent == null) {
			errors.rejectValue("newAgent", "profiles.relationship.add.noAgent")
		}
	}

}

trait EditStudentRelationshipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: EditStudentRelationshipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Manage(mandatory(relationshipType)), mandatory(studentCourseDetails))
	}

}

trait EditStudentRelationshipDescription extends Describable[Seq[StudentRelationship]] {

	self: EditStudentRelationshipCommandState with EditStudentRelationshipCommandRequest =>

	override lazy val eventName = "EditStudentRelationship"

	override def describe(d: Description) {
		d.studentIds(Seq(studentCourseDetails.student.universityId)).properties(
			"sprCode" -> studentCourseDetails.sprCode,
			"oldAgent" -> Option(oldAgent).map(_.universityId).getOrElse(""),
			"newAgent" -> Option(newAgent).map(_.universityId).getOrElse("")
		)
	}
}

trait EditStudentRelationshipCommandState {
	def studentCourseDetails: StudentCourseDetails
	def relationshipType: StudentRelationshipType
	def user: CurrentUser
}

trait EditStudentRelationshipCommandRequest {
	var oldAgent: Member = _
	var newAgent: Member = _
	var removeAgent: Boolean = false
	var specificScheduledDate: Boolean = false
	var scheduledDate: DateTime = DateTime.now
	lazy val scheduledDateToUse: DateTime = if (specificScheduledDate) {
		scheduledDate
	} else {
		DateTime.now
	}
	var notifyStudent: Boolean = false
	var notifyOldAgent: Boolean = false
	var notifyNewAgent: Boolean = false
}

trait EditStudentRelationshipCommandNotifications extends Notifies[Seq[StudentRelationship], StudentRelationship] {

	self: EditStudentRelationshipCommandState with EditStudentRelationshipCommandRequest =>

	def emit(relationships: Seq[StudentRelationship]): Seq[Notification[StudentRelationship, Unit]] = {
		val notifications = relationships.filter(_.replacedBy == null).flatMap(relationship => {

			val studentNotification = if (notifyStudent) {
				val notification = Notification.init(new StudentRelationshipChangeToStudentNotification, user.apparentUser, Seq(relationship))
				notification.oldAgentIds.value = Seq(Option(oldAgent).map(_.universityId)).flatten
				if (scheduledDateToUse.isAfterNow) notification.scheduledDate = scheduledDateToUse
				Some(notification)
			} else None

			val oldAgentNotification = if (notifyOldAgent && Option(oldAgent).isDefined) {
				val notification = Notification.init(new StudentRelationshipChangeToOldAgentNotification, user.apparentUser, Seq(relationship))
				notification.oldAgentIds.value = Seq(oldAgent.universityId)
				if (scheduledDateToUse.isAfterNow) notification.scheduledDate = scheduledDateToUse
				Some(notification)
			} else None

			val newAgentNotification = if (notifyNewAgent) {
				relationship.agentMember.map(_ => {
					val notification = Notification.init(new StudentRelationshipChangeToNewAgentNotification, user.apparentUser, Seq(relationship))
					Option(oldAgent).foreach(agent => notification.oldAgentIds.value = Seq(agent.universityId))
					if (scheduledDateToUse.isAfterNow) notification.scheduledDate = scheduledDateToUse
					notification
				})
			} else None

			studentNotification ++ oldAgentNotification ++ newAgentNotification
		})

		notifications
	}

}
