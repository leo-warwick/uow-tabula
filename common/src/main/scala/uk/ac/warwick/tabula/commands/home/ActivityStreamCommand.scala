package uk.ac.warwick.tabula.commands.home

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.ActivityService.PagedActivities
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

object ActivityStreamCommand {
	def apply(request: ActivityStreamRequest) =
		new ActivityStreamCommandInternal(request)
			with ComposableCommand[PagedActivities]
			with Unaudited
			with PubliclyVisiblePermissions
			with ActivityStreamCommandValidation
			with ReadOnly
}

abstract class ActivityStreamCommandInternal(
	val request: ActivityStreamRequest
) extends CommandInternal[PagedActivities]
	with ActivityStreamCommandState
	with NotificationServiceComponent {

	def applyInternal(): PagedActivities = transactional(readOnly = true) {
		val results = notificationService.stream(request)
		PagedActivities(
			results.items,
			results.lastUpdatedDate,
			results.totalHits
		)
	}
}

trait ActivityStreamCommandValidation extends SelfValidating {
		self: ActivityStreamCommandState =>
	def validate(errors: Errors) {

	}
}

trait ActivityStreamCommandState {
	def request: ActivityStreamRequest
}
