package uk.ac.warwick.tabula.data.model.notifications.coursework

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.userlookup.User

abstract class ExtensionRequestNotification
	extends ExtensionNotification with AllCompletedActionRequiredNotification {

	@transient
	var relationshipService: RelationshipService = Wire.auto[RelationshipService]
	@transient
	var profileService: ProfileService = Wire.auto[ProfileService]

	def template: String

	def url: String = Routes.admin.assignment.extension.expandrow(assignment, student.getUserId)
	def urlTitle = "review this extension request"

	def studentMember: Option[Member] = Option(student.getWarwickId).flatMap(uid => profileService.getMemberByUniversityId(uid))
	def studentRelationships: Seq[StudentRelationshipType] = relationshipService.allStudentRelationshipTypes

	def profileInfo: Map[String, Object] = studentMember.collect { case student: StudentMember => student }.flatMap(_.mostSignificantCourseDetails).map(scd => {
		val relationships = studentRelationships.map(x => (
			x.description,
			relationshipService.findCurrentRelationships(x, scd)
		)).filter{ case (relationshipType,relations) => relations.nonEmpty }.toMap

		Map(
			"relationships" -> relationships,
			"scdCourse" -> scd.course,
			"scdRoute" -> scd.currentRoute,
			"scdAward" -> scd.award
		)
	}).getOrElse(Map())

	def content = FreemarkerModel(template, Map(
		"requestedExpiryDate" -> dateTimeFormatter.print(extension.requestedExpiryDate.orNull),
		"reasonForRequest" -> extension.reason,
		"attachments" -> extension.attachments,
		"assignment" -> assignment,
		"student" -> student,
		"moduleManagers" -> assignment.module.managers.users
	) ++ profileInfo)

	def recipients: Seq[User] = assignment.module.adminDepartment.extensionManagers.users
}

@Entity
@DiscriminatorValue("ExtensionRequestCreated")
class ExtensionRequestCreatedNotification extends ExtensionRequestNotification {
	priority = Warning
	def verb = "create"
	def template = "/WEB-INF/freemarker/emails/new_extension_request.ftl"
	def title: String = titlePrefix + "New extension request made by %s for \"%s\"".format(student.getFullName, assignment.name)
}

@Entity
@DiscriminatorValue("ExtensionRequestModified")
class ExtensionRequestModifiedNotification extends ExtensionRequestNotification {
	priority = Warning
	def verb = "modify"
	def template = "/WEB-INF/freemarker/emails/modified_extension_request.ftl"
	def title: String = titlePrefix + "Extension request modified by %s for \"%s\"".format(student.getFullName, assignment.name)
}

@Entity
@DiscriminatorValue("ExtensionMoreInfoReceived")
class ExtensionInfoReceivedNotification extends ExtensionRequestNotification {
	priority = Warning
	def verb = "reply"
	def template = "/WEB-INF/freemarker/emails/extension_info_received.ftl"
	def title: String = titlePrefix + "Further information provided by %s for \"%s\"".format(student.getFullName, assignment.name)
}