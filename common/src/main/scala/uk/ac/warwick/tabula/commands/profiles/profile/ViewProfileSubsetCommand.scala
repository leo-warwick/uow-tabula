package uk.ac.warwick.tabula.commands.profiles.profile

import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.permissions.Permissions.UserPicker
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

object ViewProfileSubsetCommand {
	def apply(student: User, profileService: ProfileService) =
		new ViewProfileSubsetCommandInternal(student, profileService)
			with ComposableCommand[ProfileSubset]
			with ViewProfileSubsetCommandPermissions
			with Unaudited
			with ReadOnly
}

abstract class ViewProfileSubsetCommandInternal(student: User, profileService: ProfileService)
	extends CommandInternal[ProfileSubset] with ViewProfileSubsetCommandState {

	val studentMember: Option[StudentMember] = Option(student.getWarwickId)
			.map(uid => profileService.getMemberByUniversityId(uid))
			.collect{ case Some(sm: StudentMember) => sm }

	// only try to get the user via lookup if no student member is found
	val user: Option[User] = studentMember match {
		case Some(_) => None
		case None => Option(student).filter(_.isFoundUser)
	}

	def applyInternal(): ProfileSubset = {
		if (studentMember.isDefined || user.isDefined)
			ProfileSubset(studentMember.isDefined, user, studentMember, studentMember.flatMap(_.mostSignificantCourseDetails))
		else
			throw new ItemNotFoundException()
	}
}

trait ViewProfileSubsetCommandState {
	val studentMember: Option[StudentMember]
	val user: Option[User]
}

trait ViewProfileSubsetCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ViewProfileSubsetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		studentMember.foreach(p.PermissionCheck(Permissions.Profiles.Read.Core, _))
		if (user.isDefined) p.PermissionCheck(UserPicker)
	}
}

case class ProfileSubset (
	isMember: Boolean,
	user: Option[User],
	profile: Option[StudentMember],
	courseDetails: Option[StudentCourseDetails]
)