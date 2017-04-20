package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity, Notification, UserIdRecipientNotification}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Routes

object ReleaseSmallGroupSetsNotification {
	val templateLocation = "/WEB-INF/freemarker/notifications/groups/release_small_group_notification.ftl"
}

@Entity
@DiscriminatorValue("ReleaseSmallGroupSets")
class ReleaseSmallGroupSetsNotification extends Notification[SmallGroup, Unit]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent
	with MyWarwickActivity {

	def verb: String = "Release"

	override def onPreSave(newRecord: Boolean) {
		if (entities.isEmpty) {
			throw new IllegalArgumentException("Attempted to create a ReleaseSmallGroupSetsNotification with no SmallGroups!")
		}
	}

	def groups: Seq[SmallGroup] = entities

	def isStudent: Boolean = getBooleanSetting("isStudent", default=false)
	def isStudent_= (b:Boolean) { settings += ("isStudent" -> b) }

	def formats: List[String] = groups.map(_.groupSet.format.description).distinct.toList
	def formatString: String = formats match {
		case singleFormat :: Nil => singleFormat
		case _ => Seq(formats.init.mkString(", "), formats.last).mkString(" and ")
	}

	def title: String = {
		val moduleCodes = entities.map(_.groupSet.module.code.toUpperCase).distinct.toList.sorted
		val moduleCodesString = moduleCodes match {
			case singleModuleCode :: Nil => singleModuleCode
			case _ => Seq(moduleCodes.init.mkString(", "), moduleCodes.last).mkString(" and ")
		}

		"%s %s %s".format(moduleCodesString, formatString.toLowerCase, if (entities.size == 1) "allocation" else "allocations")
	}

	def content =
		FreemarkerModel(ReleaseSmallGroupSetsNotification.templateLocation,
			Map("user" -> recipient, "groups" -> groups, "profileUrl" -> url)
		)

	def url: String = {
		if (isStudent) {
			Routes.profiles.Profile.seminars(recipient.getWarwickId)
		} else {
			Routes.groups.tutor.mygroups
		}
	}

	def urlTitle = s"view your $formatString groups"

}
