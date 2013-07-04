package uk.ac.warwick.tabula.profiles.commands

import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Command,Unaudited}
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.CurrentUser
import scala.Some

class ViewMeetingRecordCommand(val studentCourseDetails: StudentCourseDetails, val currentUser: CurrentUser) extends Command[Seq[MeetingRecord]] with Unaudited {

  /**
   * Only implemented for tutors at the moment, though it may Just Work (tm) for supervisors if you pass the
   * relationshipType in as a constructor arg.
   */
  val relationshipType = RelationshipType.PersonalTutor

  PermissionCheck(MeetingPermissions.Read.permissionFor(relationshipType), studentCourseDetails.student)
	var dao = Wire.auto[MeetingRecordDao]
	var relationshipService = Wire.auto[RelationshipService]
	var profileService = Wire.auto[ProfileService]

	def applyInternal() = {
		val rels = relationshipService.getRelationships(relationshipType, studentCourseDetails.sprCode)

		val currentMember = profileService.getMemberByUniversityId(currentUser.universityId)

		currentMember match {
			case None => Seq()
			case Some(mem)=> dao.list(rels.toSet, mem)
		}
	}
}
