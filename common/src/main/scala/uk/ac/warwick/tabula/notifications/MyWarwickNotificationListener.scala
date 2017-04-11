package uk.ac.warwick.tabula.notifications

import com.fasterxml.jackson.databind.ObjectMapper
import dispatch.classic.url
import org.hibernate.ObjectNotFoundException
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, Notification, NotificationWithTarget, ToEntityReference}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{AutowiringDispatchHttpClientComponent, DispatchHttpClientComponent, NotificationListener}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, TextRendererComponent}
import uk.ac.warwick.util.mywarwick.MyWarwickService
import uk.ac.warwick.util.mywarwick.model.request.{Activity, Tag}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.existentials

trait MyWarwickNotificationListener extends NotificationListener {
	self: AutowiredTextRendererComponent with AutowiringFeaturesComponent with AutowiringMyWarwickServiceComponent =>

	private def toMyWarwickActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Option[Activity] = try {
		val recipients = notification.recipientNotificationInfos.asScala
			.filterNot(_.dismissed) // Not if the user has dismissed the notificaiton already
			.map(_.recipient)
			.filter(_.isFoundUser) // Only users found in SSO
			.map(_.getUserId)

		if (recipients.isEmpty) None
		else {
			val allEntities = notification match {
				case targetNotification: NotificationWithTarget[_, _] => targetNotification.items.asScala :+ targetNotification.target
				case _ => notification.items.asScala
			}

			val permissionsTargets = allEntities.map {
				_.entity
			}.collect { case pt: PermissionsTarget => pt }

			def withParents(target: PermissionsTarget): Stream[PermissionsTarget] = target #:: target.permissionsParents.flatMap(withParents)

			val tags = permissionsTargets.flatMap(withParents).distinct.map { target =>
				val tag = new Tag()
				tag.setName(target.urlCategory)
				tag.setValue(target.urlSlug)
				tag.setDisplay_value(target.humanReadableId)
				tag
			}

			val activity = new Activity(
				recipients.toSet.asJava,
				notification.title,
				topLevelUrl + notification.url,
				textRenderer.renderTemplate(notification.content.template, notification.content.model),
				notification.notificationType
			)

			activity.setTags(tags.toSet.asJava)

			Some(activity)
		}
	} catch {
		// referenced entity probably missing, oh well.
		case e: ObjectNotFoundException => None
	}

	private def postActivity(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
		toMyWarwickActivity(notification).map { a =>
			notification.notificationType match {
				case "SubmissionReceipt" => myWarwickService.sendAsActivity(a)
				case _ => myWarwickService.sendAsNotification(a)
			}
		}
	}

	override def listen(notification: Notification[_ >: Null <: ToEntityReference, _]): Unit = {
		if (features.myWarwickNotificationListener) {
			postActivity(notification)
		}
	}

}

@Component
class AutowiringMyWarwickNotificationListener extends MyWarwickNotificationListener
	with AutowiredTextRendererComponent
	with AutowiringFeaturesComponent
	with AutowiringMyWarwickServiceComponent

trait AutowiringMyWarwickServiceComponent extends MyWarwickServiceComponent {
	var myWarwickService: MyWarwickService = Wire[MyWarwickService]
	var topLevelUrl: String = Wire.property("${toplevel.url}")
}

trait MyWarwickServiceComponent {
	var myWarwickService: MyWarwickService
}