package uk.ac.warwick.tabula.helpers.coursework

import org.joda.time.DateTime
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.forms.Extension

case class ExtensionGraph(
	user: User,
	deadline: DateTime,
	isAwaitingReview: Boolean,
	hasApprovedExtension: Boolean,
	hasRejectedExtension: Boolean,
	duration: Int,
	requestedExtraDuration: Int,
	extension: Option[Extension])

object ExtensionGraph {
	def apply(extension: Extension, user: User) = new ExtensionGraph(
		user,
		extension.assignment.submissionDeadline(user),
		extension.awaitingReview,
		extension.approved,
		extension.rejected,
		extension.duration,
		extension.requestedExtraDuration,
		Some(extension))
}
