package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired

import uk.ac.warwick.tabula.data.model.{ToEntityReference, Notification}
import uk.ac.warwick.tabula.services.NotificationService
import uk.ac.warwick.tabula.system.TwoWayConverter

class NotificationIdConverter extends TwoWayConverter[String, Notification[_  >: Null <: ToEntityReference,_]] {

	@Autowired var service: NotificationService = _

	override def convertRight(id: String): Notification[_ >: Null <: ToEntityReference, _] = (Option(id) flatMap { service.getNotificationById(_) }).orNull
	override def convertLeft(notification: Notification[_  >: Null <: ToEntityReference ,_]): String = (Option(notification) map {_.id}).orNull

}