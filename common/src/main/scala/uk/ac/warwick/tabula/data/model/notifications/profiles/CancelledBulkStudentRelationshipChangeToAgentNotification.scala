package uk.ac.warwick.tabula.data.model.notifications.profiles

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, ProfileService, RelationshipService}

import scala.util.Try

@Entity
@DiscriminatorValue("CancelledBulkStudentRelationshipChangeToAgent")
class CancelledBulkStudentRelationshipChangeToAgentNotification extends Notification[StudentRelationship, Unit]
	with SingleRecipientNotification with UniversityIdRecipientNotification with AutowiringUserLookupComponent {

	@transient var profileService: ProfileService = Wire[ProfileService]
	@transient var relationshipService: RelationshipService = Wire[RelationshipService]

	@transient private val relationshipTypeId = StringSetting("relationshipType", "")

	def relationshipType: Option[StudentRelationshipType] = relationshipTypeId.value.maybeText.flatMap(id => relationshipService.getStudentRelationshipTypeById(id))
	def relationshipType_=(r: StudentRelationshipType): Unit = relationshipTypeId.value = r.id

	@transient val cancelledAdditionsIds = StringSeqSetting("cancelledAdditions", Nil)

	def cancelledAdditions: Seq[Member] = cancelledAdditionsIds.value.flatMap { id => profileService.getMemberByUniversityId(id)}

	@transient val cancelledRemovalsIds = StringSeqSetting("cancelledRemovals", Nil)

	def cancelledRemovals: Seq[Member] = cancelledRemovalsIds.value.flatMap { id => profileService.getMemberByUniversityId(id)}

	@transient private val scheduledDateString = StringSetting("scheduledDate", "")

	def scheduledDate: Option[DateTime] = scheduledDateString.value.maybeText.flatMap(s => Try(new DateTime(s.toLong)).toOption)
	def scheduledDate_=(date: DateTime): Unit = scheduledDateString.value = date.getMillis.toString

	def title: String = s"Scheduled change to ${relationshipType.get.studentRole}s cancelled"
	def templateLocation = "/WEB-INF/freemarker/notifications/profiles/cancel_bulk_agent_notification.ftl"
	def verb = "change"
	override def url: String = Routes.students(relationshipType.get)
	override def urlTitle = s"view your ${relationshipType.get.studentRole}s"

	def content =
		FreemarkerModel(templateLocation, Map(
			"relationshipType" -> relationshipType,
			"scheduledDate" -> scheduledDate,
			"cancelledAdditions" -> cancelledAdditions,
			"cancelledRemovals" -> cancelledRemovals
		))

}
