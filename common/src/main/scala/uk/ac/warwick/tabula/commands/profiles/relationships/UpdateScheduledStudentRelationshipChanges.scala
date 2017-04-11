package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports.{JArrayList, JList}
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.SecurityServiceComponent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.mutable

trait UpdateScheduledStudentRelationshipChangesValidation extends SelfValidating {

	self: ManageStudentRelationshipsState with UpdateScheduledStudentRelationshipChangesCommandRequest
		with SecurityServiceComponent =>

	override def validate(errors: Errors) {
		relationships.asScala.foreach { relationship =>
			errors.pushNestedPath(s"relationships[${relationship.id}]")
			if (!securityService.can(user, Permissions.Profiles.StudentRelationship.Manage(relationshipType), relationship.studentCourseDetails)) {
				errors.reject("profiles.relationship.updateScheduled.noPermission")
			} else if (relationship.startDate == null && relationship.endDate == null) {
				errors.reject("profiles.relationship.updateScheduled.noChange")
			} else if (relationship.isCurrent && relationship.endDate == null) {
				errors.reject("profiles.relationship.updateScheduled.noEnd")
			} else if (relationship.endDate != null && relationship.endDate.isBeforeNow) {
				errors.reject("profiles.relationship.updateScheduled.alreadyEnded")
			}
			errors.popNestedPath()
		}
		if (scheduledDate.isEmpty) {
			errors.rejectValue("relationships", "profiles.relationship.updateScheduled.differentDates")
		}
	}

}

trait UpdateScheduledStudentRelationshipChangesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ManageStudentRelationshipsState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), mandatory(department))
		// Need to check Manage permission on each of the requested relationships, but that can't be done before binding
		// so do in the validator
	}

}

trait UpdateScheduledStudentRelationshipChangesCommandRequest extends ManageStudentRelationshipsRequest {
	var relationships: JList[StudentRelationship] = JArrayList()
	val previouslyReplacedRelationships: mutable.Map[StudentRelationship, Set[StudentRelationship]] = mutable.ListMap()
	lazy val scheduledDate: Option[DateTime] = {
		val dates = relationships.asScala.map(relationship =>
			if (relationship.startDate.isAfter(DateTime.now))
				relationship.startDate
			else
				relationship.endDate
		).distinct
		if (dates.size == 1) {
			dates.headOption
		} else {
			None
		}
	}

	// Actual date doesn't matter; as long as it's in the past is won't be printed
	override lazy val scheduledDateToUse: DateTime = DateTime.now.minusDays(1)
	override def previouslyScheduledDate: Option[DateTime] = scheduledDate
}
