package uk.ac.warwick.tabula.data.model.notifications.groups

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent

object OpenSmallGroupSetsNotification {
	@transient val templateLocation = "/WEB-INF/freemarker/notifications/groups/open_small_group_student_notification.ftl"
}

abstract class AbstractOpenSmallGroupSetsNotification
	extends Notification[SmallGroupSet, Unit]
	with UserIdRecipientNotification
	with AutowiringUserLookupComponent
	with MyWarwickActivity {

	def verb = "Opened"

	def formats: List[String] = entities.map(_.format.description).distinct.toList

	def formatsString: String = formats match {
		case singleFormat :: Nil => singleFormat
		case _ => Seq(formats.init.mkString(", "), formats.last).mkString(" and ")
	}

	def title: String = {
		val moduleCodes = entities.map(_.module.code.toUpperCase).distinct.toList.sorted
		val moduleCodesString = moduleCodes match {
			case singleModuleCode :: Nil => singleModuleCode
			case _ => Seq(moduleCodes.init.mkString(", "), moduleCodes.last).mkString(" and ")
		}

		val pluralFormats = entities.map(_.format.plural.toLowerCase).distinct.toList
		val pluralFormatsString = pluralFormats match {
			case singleFormat :: Nil => singleFormat
			case _ => Seq(pluralFormats.init.mkString(", "), pluralFormats.last).mkString(" and ")
		}

		"%s %s are now open for sign up".format(moduleCodesString, pluralFormatsString)
	}

	def content = FreemarkerModel(OpenSmallGroupSetsNotification.templateLocation, Map(
		"groupsets" -> entities,
		"profileUrl" -> url,
		"formatsString" -> formatsString
	))

	def url: String = "/groups"
	def urlTitle = s"sign up for these $formatsString groups"

}

@Entity
@DiscriminatorValue(value="OpenSmallGroupSets")
class OpenSmallGroupSetsStudentSignUpNotification
	extends AbstractOpenSmallGroupSetsNotification with AllCompletedActionRequiredNotification {

	priority = Warning

}

@Entity
@DiscriminatorValue(value="OpenSmallGroupSetsOtherSignUp")
class OpenSmallGroupSetsOtherSignUpNotification extends AbstractOpenSmallGroupSetsNotification
