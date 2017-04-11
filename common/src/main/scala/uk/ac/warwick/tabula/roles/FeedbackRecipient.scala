package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._

/*
 * Allow students to receive their own feedback
 */
case class FeedbackRecipient(feedback: model.Feedback) extends BuiltInRole(FeedbackRecipientRoleDefinition, feedback)

case object FeedbackRecipientRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Feedback Recipient"

	GrantsScopedPermission(
		AssignmentFeedback.Read,
		AssignmentFeedback.Rate
	)

}
