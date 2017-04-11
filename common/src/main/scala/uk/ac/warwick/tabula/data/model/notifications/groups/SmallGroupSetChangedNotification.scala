package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, NotificationWithTarget, UserIdRecipientNotification}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes

object SmallGroupSetChangedNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/groups/small_group_modified_notification.ftl"
}

abstract class SmallGroupSetChangedNotification(recipientRole: UserRoleOnGroup)
	extends NotificationWithTarget[SmallGroup, SmallGroupSet]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent {

	def verb = "Modify"

	def title: String = "%s: Your %s allocation has changed".format(target.entity.module.code.toUpperCase, target.entity.format.description.toLowerCase)

	@transient val oldSmallGroupSizes = StringMapSetting("oldSmallGroupSizes", Map())

	def content =
		FreemarkerModel(SmallGroupSetChangedNotification.templateLocation, Map(
			"groups" -> entities,
			"groupSet" -> target.entity,
			"profileUrl" -> url
		) ++ extraModel)

	def url: String = {
		recipientRole match {
			case UserRoleOnGroup.Student => Routes.profiles.Profile.seminars(recipient.getWarwickId)
			case UserRoleOnGroup.Tutor => Routes.groups.tutor.mygroups
		}
	}

	def urlTitle = "view this small group"

	def extraModel: Map[String, Any] = Map()
}

@Entity
@DiscriminatorValue(value="SmallGroupSetChangedStudent")
class SmallGroupSetChangedStudentNotification extends SmallGroupSetChangedNotification(UserRoleOnGroup.Student)

@Entity
@DiscriminatorValue(value="SmallGroupSetChangedTutor")
class SmallGroupSetChangedTutorNotification extends SmallGroupSetChangedNotification(UserRoleOnGroup.Tutor) {

	private def changedGroupInfo = {
		entities.map { newSmallGroup =>
			(newSmallGroup, oldSmallGroupSizes.value.get(newSmallGroup.id).getOrElse("0")) }.filter { case (newGroup,oldSize) => newGroup.students.size != oldSize.toInt }
	}

	override  def extraModel = Map("groupsWithOldSizeInfo" -> changedGroupInfo)
}

sealed trait UserRoleOnGroup
object UserRoleOnGroup {
	case object Student extends UserRoleOnGroup
	case object Tutor extends UserRoleOnGroup
}

