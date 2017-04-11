package uk.ac.warwick.tabula.data.model.notifications.profiles

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, _}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}
import uk.ac.warwick.userlookup.User

import scala.util.Try

abstract class CancelledStudentRelationshipChangeNotification
	extends Notification[StudentRelationship, Unit] with SingleItemNotification[StudentRelationship] {

	var profileService: ProfileService = Wire[ProfileService]
	var relationshipService: RelationshipService = Wire[RelationshipService]

	def templateLocation: String

	def verb = "change"

	@transient private val studentId = StringSetting("student", "")

	def student: Option[Member] = studentId.value.maybeText.flatMap(id => profileService.getMemberByUniversityId(id))
	def student_=(s: Member): Unit = studentId.value = s.universityId

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

	def content =
		FreemarkerModel(templateLocation, Map(
			"student" -> student,
			"relationshipType" -> relationshipType,
			"scheduledDate" -> scheduledDate,
			"cancelledAdditions" -> cancelledAdditions,
			"cancelledRemovals" -> cancelledRemovals
		))

	def url: String = Routes.Profile.relationshipType(student.get, relationshipType.get)
}

@Entity
@DiscriminatorValue("CancelledStudentRelationshipChangeToStudent")
class CancelledStudentRelationshipChangeToStudentNotification extends CancelledStudentRelationshipChangeNotification {
	def title: String = s"Scheduled ${relationshipType.get.agentRole} allocation change cancelled"
	def templateLocation = CancelledStudentRelationshipChangeNotification.StudentTemplate
	def recipients: Seq[User] = student.map { _.asSsoUser }.toSeq
	def urlTitle = "view your student profile"
}

@Entity
@DiscriminatorValue("CancelledStudentRelationshipChangeToOldAgent")
class CancelledStudentRelationshipChangeToOldAgentNotification extends CancelledStudentRelationshipChangeNotification {

	def title: String = s"Scheduled change to ${relationshipType.get.studentRole}s cancelled"
	def templateLocation = CancelledStudentRelationshipChangeNotification.OldAgentTemplate
	def recipients: Seq[User] = cancelledRemovals.map { _.asSsoUser }
	private def profileName = student match {
		case Some(sm) if sm.fullName.nonEmpty => " for " + sm.fullName.get
		case _ => ""
	}
	override def urlTitle = s"view the student profile$profileName"
}

@Entity
@DiscriminatorValue("CancelledStudentRelationshipChangeToNewAgent")
class CancelledStudentRelationshipChangeToNewAgentNotification extends CancelledStudentRelationshipChangeNotification {

	def title = s"Scheduled change to ${relationshipType.get.studentRole}s cancelled"
	def templateLocation = CancelledStudentRelationshipChangeNotification.NewAgentTemplate
	def recipients: Seq[User] = cancelledAdditions.map { _.asSsoUser }
	override def url: String = Routes.students(relationshipType.get)
	override def urlTitle = s"view your ${relationshipType.get.studentRole}s"
}

object CancelledStudentRelationshipChangeNotification {
	val NewAgentTemplate = "/WEB-INF/freemarker/notifications/profiles/cancel_new_agent_notification.ftl"
	val OldAgentTemplate = "/WEB-INF/freemarker/notifications/profiles/cancel_old_agent_notification.ftl"
	val StudentTemplate = "/WEB-INF/freemarker/notifications/profiles/cancel_student_change_relationship_notification.ftl"
}