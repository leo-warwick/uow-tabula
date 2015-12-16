package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.commands.{SchedulesNotifications, SelfValidating, Command, Description}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventOccurrence, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{ToEntityReference, ScheduledNotification, Module}
import collection.JavaConverters._

class DeleteSmallGroupSetCommand(val module: Module, val set: SmallGroupSet)
	extends Command[SmallGroupSet] with SelfValidating with SchedulesNotifications[SmallGroupSet, SmallGroupEventOccurrence] {

	mustBeLinked(set, module)
	PermissionCheck(Permissions.SmallGroups.Delete, set)

	var service = Wire[SmallGroupService]

	var confirm = false

	override def applyInternal() = transactional() {
		set.markDeleted()
		service.saveOrUpdate(set)
		set
	}

	def validate(errors: Errors) {
		if (!confirm) {
			errors.rejectValue("confirm", "smallGroupSet.delete.confirm")
		} else validateCanDelete(errors)
	}

	def validateCanDelete(errors: Errors) {
		if (set.deleted) {
			errors.reject("smallGroupSet.delete.deleted")
		} else if (set.releasedToStudents || set.releasedToTutors) {
			errors.reject("smallGroupSet.delete.released")
		}
	}

	override def describe(d: Description) = d.smallGroupSet(set)

	override def transformResult(set: SmallGroupSet): Seq[SmallGroupEventOccurrence] =
		set.groups.asScala.flatMap(
			_.events.flatMap(service.getAllSmallGroupEventOccurrencesForEvent)
		)

	override def scheduledNotifications(notificationTarget: SmallGroupEventOccurrence): Seq[ScheduledNotification[_ >: Null <: ToEntityReference]] = Seq()
}