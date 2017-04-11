package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue("RequestAssignmentAccess")
class RequestAssignmentAccessNotification
	extends Notification[Assignment, Unit]
	with SingleItemNotification[Assignment] with AllCompletedActionRequiredNotification {

	def assignment: Assignment = item.entity

	def verb = "request"
	def title: String = "%s: Access requested for \"%s\"".format(assignment.module.code.toUpperCase, assignment.name)

	def content = FreemarkerModel("/WEB-INF/freemarker/emails/requestassignmentaccess.ftl", Map(
		"assignment" -> assignment,
		"student" -> agent)
	)

	def url: String = Routes.admin.assignment.edit(assignment)
	def urlTitle = "review which students are enrolled on the assignment"

	def recipients: Seq[User] = assignment.module.adminDepartment.owners.users
		.filter(admin => admin.isFoundUser && admin.getEmail.hasText).toSeq

	priority = Warning

}
