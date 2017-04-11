package uk.ac.warwick.tabula.commands.profiles.relationships

import org.joda.time.DateTime

trait ManageStudentRelationshipsRequest {
	var notifyStudent: Boolean = false
	var notifyOldAgent: Boolean = false
	var notifyNewAgent: Boolean = false

	def scheduledDateToUse: DateTime
	def previouslyScheduledDate: Option[DateTime] = None
}
