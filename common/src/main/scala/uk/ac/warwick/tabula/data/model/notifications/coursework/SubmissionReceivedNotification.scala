package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.data.model.permissions.{GrantedPermission, RoleOverride}
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{SecurityService, UserSettingsService}
import uk.ac.warwick.userlookup.User

import scala.collection.immutable.Seq
import scala.reflect.ClassTag

@Entity
@DiscriminatorValue("SubmissionReceived")
class SubmissionReceivedNotification extends SubmissionNotification {

	override def onPreSave(isNew: Boolean) {
		// if this submission was noteworthy then the priority is higher
		if (submission.isNoteworthy) {
			priority = Warning
		}
	}

	@transient
	var userSettings: UserSettingsService = Wire[UserSettingsService]

	@transient
	var permissionsService: PermissionsService = Wire[PermissionsService]

	@transient
	var securityService: SecurityService = Wire[SecurityService]

	def templateLocation = "/WEB-INF/freemarker/emails/submissionnotify.ftl"
	def submissionTitle: String =
		if (submission == null) "Submission"
		else if (submission.isAuthorisedLate) "Authorised late submission"
		else if (submission.isLate) "Late submission"
		else "Submission"

	def title: String = "%s: %s received for \"%s\"".format(moduleCode, submissionTitle, assignment.name)

	def canEmailUser(user: User): Boolean = {
		// Alert on noteworthy submissions by default
		val setting = userSettings.getByUserId(user.getUserId).map { _.alertsSubmission }.getOrElse(UserSettings.AlertsNoteworthySubmissions)

		setting match {
			case UserSettings.AlertsAllSubmissions => true
			case UserSettings.AlertsNoteworthySubmissions => submission.isNoteworthy
			case _ => false
		}
	}

	def url: String = Routes.admin.assignment.submissionsandfeedback(assignment)
	def urlTitle = "view all submissions for this assignment"

	def recipients: Seq[User] = {
		// TAB-2333 Get any user that has Submission.Delete over the submission, or any of its permission parents
		// Look at Assignment, Module and Department (can't grant explicitly over one Submission atm)
		val requiredPermission = Permissions.Submission.Delete
		def usersWithPermission[A <: PermissionsTarget: ClassTag](scope: A) = {
			val roleGrantedUsers =
				permissionsService.getAllGrantedRolesFor[A](scope)
					.filter { _.mayGrant(requiredPermission) }
					.flatMap { _.users.users }
					.toSet

			val explicitlyGrantedUsers =
				permissionsService.getGrantedPermission(scope, requiredPermission, RoleOverride.Allow)
					.toSet
					.flatMap { permission: GrantedPermission[A] => permission.users.users }

			roleGrantedUsers ++ explicitlyGrantedUsers
		}

		def withParents(target: PermissionsTarget): Stream[PermissionsTarget] = target #:: target.permissionsParents.flatMap(withParents)

		val adminsWithPermission = withParents(assignment).flatMap(usersWithPermission)
			.filter { user => securityService.can(new CurrentUser(user, user), requiredPermission, submission) }

		// Contact the current marker, if there is one, and the submission has already been released
		val feedback = assignment.findFeedback(submission.usercode)
		val currentMarker = if (assignment.hasWorkflow && feedback.exists(_.isPlaceholder)) {
			feedback.flatMap { f => f.getCurrentWorkflowFeedback }
				.flatMap { _.getMarkerUser }
				.toSeq
		} else {
			Seq()
		}
		(adminsWithPermission ++ currentMarker).distinct.filter(canEmailUser)
	}

}
