package uk.ac.warwick.tabula.commands.coursework.assignments
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Notifies, Description, Command}
import uk.ac.warwick.tabula.data.model.notifications.coursework.RequestAssignmentAccessNotification
import uk.ac.warwick.tabula.data.model.{Notification, Module, Assignment}
import uk.ac.warwick.tabula.helpers.UnicodeEmails
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.userlookup.User

/**
 * Sends a message to one or more admins to let them know that the current
 * user thinks they should have access to an assignment.
 */
class RequestAssignmentAccessCommand(module: Module, assignment: Assignment, user: CurrentUser) extends Command[Seq[User]]
	with Notifies[Seq[User], Assignment] with FreemarkerRendering with UnicodeEmails with Public {

	mustBeLinked(mandatory(assignment), mandatory(module))

	def admins: Seq[User] = {
		// lookup the admin users - used to determine the recipients  for notifications
		module.adminDepartment.owners.users.filter(admin => admin.isFoundUser && admin.getEmail.hasText).toSeq
	}

	// Returns the Seq of admin users
	override def applyInternal(): Seq[User] = admins

	override def describe(d: Description) {
		d.assignment(assignment)
	}

	def emit(admins: Seq[User]): Seq[RequestAssignmentAccessNotification] = {
		Seq(Notification.init(new RequestAssignmentAccessNotification, user.apparentUser, Seq(assignment)))
	}
}
