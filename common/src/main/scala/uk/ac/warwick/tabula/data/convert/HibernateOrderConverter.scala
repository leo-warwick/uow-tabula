package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.helpers.StringUtils._

class HibernateOrderConverter extends TwoWayConverter[String, Order] {

	override def convertLeft(order: Order): String = Option(order).map { order =>
		val direction = if (order.isAscending()) "asc" else "desc"
		"%s(%s)".format(direction, order.getPropertyName)
	}.orNull

	override def convertRight(s: String): Order = (s match {
		case r"""(asc|desc)${direction}\(("?[^"\)]+"?)${propertyName}\)""" => direction match {
			case "asc" => Some(Order.asc(propertyName))
			case "desc" => Some(Order.desc(propertyName))
		}
		case _ => None
	}).orNull

}