package uk.ac.warwick.tabula.commands.scheduling.imports

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, AutowiringRelationshipServiceComponent, MeetingRecordServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object MigrateMeetingRecordsFromOldRelationshipsCommand {
	def apply(student: StudentMember) =
		new MigrateMeetingRecordsFromOldRelationshipsCommandInternal(student)
			with AutowiringRelationshipServiceComponent
			with AutowiringMeetingRecordServiceComponent
			with ComposableCommand[Unit]
			with MigrateMeetingRecordsFromOldRelationshipsValidation
			with MigrateMeetingRecordsFromOldRelationshipsPermissions
			with MigrateMeetingRecordsFromOldRelationshipsCommandState
			with Unaudited
}


class MigrateMeetingRecordsFromOldRelationshipsCommandInternal(val student: StudentMember) extends CommandInternal[Unit] {

	self: MigrateMeetingRecordsFromOldRelationshipsCommandState with MeetingRecordServiceComponent =>

	override def applyInternal(): Unit = {
		migrations.foreach{ case(from, to) => meetingRecordService.migrate(from, to) }
	}

}

trait MigrateMeetingRecordsFromOldRelationshipsValidation extends SelfValidating {

	self: MigrateMeetingRecordsFromOldRelationshipsCommandState =>

	override def validate(errors: Errors) {
		if (migrations.isEmpty) {
			errors.reject("No meetings to migrate")
		}
	}

}

trait MigrateMeetingRecordsFromOldRelationshipsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: MigrateMeetingRecordsFromOldRelationshipsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

}

trait MigrateMeetingRecordsFromOldRelationshipsCommandState {

	self: MeetingRecordServiceComponent with RelationshipServiceComponent =>

	def student: StudentMember

	lazy val personalTutorRelationshipType: StudentRelationshipType = relationshipService.getStudentRelationshipTypeByUrlPart("tutor").getOrElse(
		throw new ItemNotFoundException("Could not find personal tutor relationship type")
	)

	lazy val studentRelationships: Seq[StudentRelationship] = relationshipService.getRelationships(personalTutorRelationshipType, student)

	lazy val migrations: Map[StudentRelationship, StudentRelationship] = {
		studentRelationships.groupBy(_.agent).flatMap{case(agent, agentRelationships) =>
			agentRelationships.filter(!_.isCurrent).flatMap(nonCurrentRelationship => {
				val meetingRecords = meetingRecordService.listAll(nonCurrentRelationship)
				val correspondingRelationship = agentRelationships.find(r =>
					r.isCurrent && sprMatch(r, nonCurrentRelationship) && departmentsMatch(r, nonCurrentRelationship)
				)
				if (meetingRecords.isEmpty || correspondingRelationship.isEmpty)
					None
				else {
					Option(nonCurrentRelationship -> correspondingRelationship.get)
				}
			})
		}
	}

	private def sprMatch(r1: StudentRelationship, r2: StudentRelationship) =
		r1.studentCourseDetails.sprCode == r2.studentCourseDetails.sprCode

	private def departmentsMatch(r1: StudentRelationship, r2: StudentRelationship) =
		r1.studentCourseDetails.latestStudentCourseYearDetails.enrolmentDepartment == r2.studentCourseDetails.latestStudentCourseYearDetails.enrolmentDepartment
}
