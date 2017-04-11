package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.HasSettings.BooleanSetting
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, UserLookupService}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

object FinaliseFeedbackNotification {
	val templateLocation = "/WEB-INF/freemarker/emails/finalise_feedback_notification.ftl"
}

@Entity
@DiscriminatorValue("FinaliseFeedback")
class FinaliseFeedbackNotification
	extends NotificationWithTarget[Feedback, Assignment]
		with ConfigurableNotification
		with AllCompletedActionRequiredNotification {

	def assignment: Assignment = target.entity

	override def verb = "finalise"

	override def title: String = {
		val moduleCode = assignment.module.code.toUpperCase
		val numberOfItems = entities.size
		val submissionPlural = if (entities.size != 1) "submissions" else "submission"
		val assignmentName = assignment.name
		val pastTensePlural = if (entities.size != 1) "have" else "has"

		s"""$moduleCode: $numberOfItems $submissionPlural for "$assignmentName" $pastTensePlural been marked"""
	}

	override def content = FreemarkerModel(FinaliseFeedbackNotification.templateLocation,
		Map(
			"assignment" -> assignment,
			"finalisedFeedbacks" -> entities
		))

	override def url: String = Routes.coursework.admin.assignment.submissionsandfeedback(assignment)
	override def urlTitle = s"publish ${if (entities.length > 1) "these items of" else "this item of"} feedback"

	@transient
	final lazy val configuringDepartment: Department = assignment.module.adminDepartment

	override def allRecipients: Seq[User] = {
		val finalisedFeedbacks = entities
		if (finalisedFeedbacks.forall(_.checkedReleased)) {
			Seq()
		} else {
			var users: Seq[User] = Seq()

			val settings = new FinaliseFeedbackNotificationSettings(departmentSettings)
			val notifyAllGroups = !settings.notifyFirstNonEmptyGroupOnly.value

			val moduleAndDepartmentService = Wire[ModuleAndDepartmentService]
			val module =
				moduleAndDepartmentService.getModuleByCode(assignment.module.code)
					.getOrElse(throw new IllegalStateException("No such module"))

			if (settings.notifyNamedUsers.value && settings.notifyNamedUsersFirst.value) {
				users ++= settings.namedUsers.value
			}

			if (settings.notifyModuleManagers.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.managers.users
			}

			if (settings.notifyDepartmentAdministrators.value && (users.isEmpty || notifyAllGroups)) {
				users ++= module.adminDepartment.owners.users
			}

			if (settings.notifyNamedUsers.value && !settings.notifyNamedUsersFirst.value && (users.isEmpty || notifyAllGroups)) {
				users ++= settings.namedUsers.value
			}

			users.distinct
		}
	}
}

class FinaliseFeedbackNotificationSettings(departmentSettings: NotificationSettings) {
	@transient private val userLookup = Wire[UserLookupService]
	// Configuration settings specific to this type of notification
	def enabled: BooleanSetting = departmentSettings.enabled
	def notifyModuleManagers = departmentSettings.BooleanSetting("notifyModuleManagers", default = false)
	def notifyDepartmentAdministrators = departmentSettings.BooleanSetting("notifyDepartmentAdministrators", default = false)
	def notifyNamedUsers = departmentSettings.BooleanSetting("notifyNamedUsers", default = false)
	def notifyNamedUsersFirst = departmentSettings.BooleanSetting("notifyNamedUsersFirst", default = false)
	def namedUsers = departmentSettings.UserSeqSetting("namedUsers", default = Seq(), userLookup)
	def notifyFirstNonEmptyGroupOnly = departmentSettings.BooleanSetting("notifyFirstNonEmptyGroupOnly", default = true)
}