package uk.ac.warwick.tabula.scheduling.commands.imports

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ExpireRelationshipsOnOldCoursesCommand {
	def apply(student: StudentMember) =
		new ExpireRelationshipsOnOldCoursesCommandInternal(student)
			with ComposableCommand[Unit]
			with AutowiringRelationshipServiceComponent
			with ExpireRelationshipsOnOldCoursesValidation
			with ExpireRelationshipsOnOldCoursesPermissions
			with ExpireRelationshipsOnOldCoursesCommandState
			with Unaudited
}


class ExpireRelationshipsOnOldCoursesCommandInternal(val student: StudentMember) extends CommandInternal[Unit] {

	self: ExpireRelationshipsOnOldCoursesCommandState with RelationshipServiceComponent =>

	override def applyInternal() = {
		studentRelationships.groupBy(_.relationshipType).foreach { case(relType, relationships) =>
			if (hasOnlyVeryOldRelationships(relationships) || hasCurrentRelationship(relationships)) {
				val relationshipsToEnd = relationships.filter(rel => rel.isCurrent && rel.studentCourseDetails.isEnded)
				relationshipService.endStudentRelationships(relationshipsToEnd)
			}
		}
	}
}

trait ExpireRelationshipsOnOldCoursesValidation extends SelfValidating {

	self: ExpireRelationshipsOnOldCoursesCommandState with RelationshipServiceComponent =>

	override def validate(errors: Errors) {
		if (!student.freshStudentCourseDetails.exists(_.isEnded)) {
			errors.reject("No old courses for this student")
		} else {
			val hasExpirable = studentRelationships.groupBy(_.relationshipType).exists {
				case(relType, relationships) =>
					// Has a current relationship on a non-ended course or all the courses ended more than three months ago
					(hasOnlyVeryOldRelationships(relationships) || hasCurrentRelationship(relationships)) &&
						// Has some relationships to expire
						relationships.exists(rel => rel.isCurrent && rel.studentCourseDetails.isEnded)
			}
			if (!hasExpirable) {
				errors.reject("No relationships to expire")
			}
		}
	}

}

trait ExpireRelationshipsOnOldCoursesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ExpireRelationshipsOnOldCoursesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

trait ExpireRelationshipsOnOldCoursesCommandState {

	self: RelationshipServiceComponent =>

	def student: StudentMember

	lazy val relationshipTypes = relationshipService.allStudentRelationshipTypes
	lazy val studentRelationships = relationshipTypes.flatMap(relationshipService.getRelationships(_, student))

	def hasOnlyVeryOldRelationships(relationships: Seq[StudentRelationship]) =
		relationships.forall(rel => rel.studentCourseDetails.isEnded && !rel.studentCourseDetails.hasEndedRecently)

	def hasCurrentRelationship(relationships: Seq[StudentRelationship]) =
		relationships.exists(rel => rel.isCurrent && !rel.studentCourseDetails.isEnded)
}
