package uk.ac.warwick.tabula.data.model.notifications.profiles

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.userlookup.User

import scala.annotation.meta.getter
import scala.util.Try

abstract class BulkRelationshipChangeNotification extends Notification[StudentRelationship, Unit]
	with MyWarwickActivity {

	@(transient @getter) val templateLocation: String

	def relationshipType: StudentRelationshipType = entities.head.relationshipType

	var relationshipService: RelationshipService = Wire[RelationshipService]
	var profileService: ProfileService = Wire[ProfileService]

	def verb: String = "change"

	@transient val oldAgentIds = StringSeqSetting("oldAgents", Nil)

	def oldAgents: Seq[Member] = oldAgentIds.value.flatMap {
		id => profileService.getMemberByUniversityId(id)
	}

	@transient private val scheduledDateString = StringSetting("scheduledDate", "")

	def scheduledDate: Option[DateTime] = scheduledDateString.value.maybeText.flatMap(s => Try(new DateTime(s.toLong)).toOption)
	def scheduledDate_=(date: DateTime): Unit = scheduledDateString.value = date.getMillis.toString

	@transient private val previouslyScheduledDateString = StringSetting("previouslyScheduledDate", "")

	def previouslyScheduledDate: Option[DateTime] = previouslyScheduledDateString.value.maybeText.flatMap(s => Try(new DateTime(s.toLong)).toOption)
	def previouslyScheduledDate_=(date: DateTime): Unit = previouslyScheduledDateString.value = date.getMillis.toString

	def content: FreemarkerModel = {
		FreemarkerModel(templateLocation, Map(
			"relationshipType" -> relationshipType,
			"modifiedRelationships" -> entities,
			"scheduledDate" -> scheduledDate,
			"previouslyScheduledDate" -> previouslyScheduledDate
		) ++ extraModel)
	}

	def extraModel: Map[String, Any]
}

/**
 * notification for a student letting them know of any change to their tutors following
 * e.g. drag and drop tutor allocation
 */
@Entity
@DiscriminatorValue(value="BulkStudentRelationship")
class BulkStudentRelationshipNotification() extends BulkRelationshipChangeNotification {
	@transient val templateLocation = BulkRelationshipChangeNotification.StudentTemplate

	def title: String = s"${relationshipType.agentRole.capitalize} allocation change"

	def newAgents: Seq[Member] = entities.flatMap(_.agentMember)

	def student: StudentMember = entities.head.studentCourseDetails.student
	def recipients = Seq(student.asSsoUser)

	def url: String = Routes.Profile.relationshipType(student, relationshipType)

	def urlTitle: String = "view this information on your student profile"

	def extraModel = Map("student" -> student,
		"oldAgents" -> oldAgents,
		"newAgents" -> newAgents
	)

}

/**
 * notification to a new tutor letting them know all their new tutees
 */
@Entity
@DiscriminatorValue(value="BulkNewAgentRelationship")
class BulkNewAgentRelationshipNotification extends BulkRelationshipChangeNotification {
	@transient val templateLocation = BulkRelationshipChangeNotification.NewAgentTemplate

	def newAgent: Option[Member] = entities.headOption.flatMap { _.agentMember}

	def title: String = s"Allocation of new ${relationshipType.studentRole}s"

	def recipients: Seq[User] = newAgent.map { _.asSsoUser }.toSeq

	def url: String = Routes.students(relationshipType)

	def urlTitle: String = s"view all of your ${relationshipType.studentRole}s"

	def extraModel = Map(
		"newAgent" -> newAgent
	)

	// Doesn't make sense here as there will be a different set of old agents for each tutee
	override def oldAgents = throw new UnsupportedOperationException("No sensible value for new agent notification")
}


/*
 * notification to an old tutor letting them know which tutees they have been unassigned
 */
@Entity
@DiscriminatorValue(value="BulkOldAgentRelationship")
class BulkOldAgentRelationshipNotification extends BulkRelationshipChangeNotification{
	@transient val templateLocation = BulkRelationshipChangeNotification.OldAgentTemplate

	def title: String = s"Change to ${relationshipType.studentRole}s"

	// this should be a sequence of 1 since one notification is created for each old agent
	def recipients: Seq[User] = oldAgents.map { _.asSsoUser	}

	def url: String = Routes.students(relationshipType)

	def urlTitle: String = s"view your ${relationshipType.studentRole}s"

	def extraModel = Map()
}

object BulkRelationshipChangeNotification {
	val NewAgentTemplate = "/WEB-INF/freemarker/notifications/profiles/bulk_new_agent_notification.ftl"
	val OldAgentTemplate = "/WEB-INF/freemarker/notifications/profiles/bulk_old_agent_notification.ftl"
	val StudentTemplate = "/WEB-INF/freemarker/notifications/profiles/student_change_relationship_notification.ftl"
}