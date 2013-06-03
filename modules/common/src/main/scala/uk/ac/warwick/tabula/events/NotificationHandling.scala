package uk.ac.warwick.tabula.events

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.NotificationService
import uk.ac.warwick.tabula.commands.{NotificationSource, Command}

trait NotificationHandling {

	var notificationService = Wire.auto[NotificationService]

	def notify[A](cmd: Command[A])(f: => A) : A = cmd match {
		case ns:NotificationSource[A] => {
			val notification = ns.emit
			notificationService.push(notification)
			f
		}
		case _ => f
	}
}
