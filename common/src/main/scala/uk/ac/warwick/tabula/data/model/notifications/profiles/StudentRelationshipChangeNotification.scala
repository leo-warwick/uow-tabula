package uk.ac.warwick.tabula.data.model.notifications.profiles

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.userlookup.User

import scala.util.Try

abstract class StudentRelationshipChangeNotification
	extends Notification[StudentRelationship, Unit] with SingleItemNotification[StudentRelationship] {

	var profileService: ProfileService = Wire[ProfileService]

	def templateLocation: String

	def verb = "change"

	def relationship: StudentRelationship = item.entity
	def relationshipType: StudentRelationshipType = relationship.relationshipType

	def newAgent: Option[Member] = if (relationship.endDate != null && relationship.endDate.isBeforeNow) {
		None
	} else {
		relationship.agentMember
	}

	@transient val oldAgentIds = StringSeqSetting("oldAgents", Nil)

	def oldAgents: Seq[Member] = oldAgentIds.value.flatMap { id => profileService.getMemberByUniversityId(id)}

	@transient private val scheduledDateString = StringSetting("scheduledDate", "")

	def scheduledDate: Option[DateTime] = scheduledDateString.value.maybeText.flatMap(s => Try(new DateTime(s.toLong)).toOption)
	def scheduledDate_=(date: DateTime): Unit = scheduledDateString.value = date.getMillis.toString

	def content =
		FreemarkerModel(templateLocation, Map(
			"student" -> relationship.studentMember,
			"newAgents" -> newAgent.toSeq,
			"relationshipType" -> relationship.relationshipType,
			"path" -> url,
			"oldAgents" -> oldAgents,
			"scheduledDate" -> scheduledDate
		))

	def url: String = Routes.Profile.relationshipType(relationship.studentMember.get, relationshipType)
}


trait RelationshipChangeAgent {
	self: StudentRelationshipChangeNotification =>

	private def profileName = relationship.studentMember match {
		case Some(sm) if sm.fullName.nonEmpty => " for " + sm.fullName.get
		case _ => ""
	}

	override def urlTitle = s"view the student profile$profileName"
}

@Entity
@DiscriminatorValue("StudentRelationshipChangeToStudent")
class StudentRelationshipChangeToStudentNotification extends StudentRelationshipChangeNotification {
	def title: String = s"${relationshipType.agentRole.capitalize} allocation change"
	def templateLocation = StudentRelationshipChangeNotification.StudentTemplate
	def recipients: Seq[User] = relationship.studentMember.map { _.asSsoUser }.toSeq
	def urlTitle = "view your student profile"
}

@Entity
@DiscriminatorValue("StudentRelationshipChangeToOldAgent")
class StudentRelationshipChangeToOldAgentNotification extends StudentRelationshipChangeNotification
	with RelationshipChangeAgent{

	def title: String = s"Change to ${relationshipType.studentRole}s"
	def templateLocation = StudentRelationshipChangeNotification.OldAgentTemplate
	def recipients: Seq[User] = oldAgents.map { _.asSsoUser }
}

@Entity
@DiscriminatorValue("StudentRelationshipChangeToNewAgent")
class StudentRelationshipChangeToNewAgentNotification extends StudentRelationshipChangeNotification
	with RelationshipChangeAgent {

	def title = s"Allocation of new ${relationshipType.studentRole}s"
	def templateLocation = StudentRelationshipChangeNotification.NewAgentTemplate
	def recipients: Seq[User] = relationship.agentMember.map { _.asSsoUser }.toSeq
}

object StudentRelationshipChangeNotification {
	val NewAgentTemplate = "/WEB-INF/freemarker/notifications/profiles/new_agent_notification.ftl"
	val OldAgentTemplate = "/WEB-INF/freemarker/notifications/profiles/old_agent_notification.ftl"
	val StudentTemplate = "/WEB-INF/freemarker/notifications/profiles/student_change_relationship_notification.ftl"
}