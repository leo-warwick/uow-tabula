package uk.ac.warwick.tabula.data.model
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class NotificationPriority (val dbValue: String) {
	// When transforming notifications to activity streams priority is represented by a numerical value between 0 and 1
	def toNumericalValue: Double

	def < (other: NotificationPriority): Boolean = toNumericalValue < other.toNumericalValue
	def <= (other: NotificationPriority): Boolean = toNumericalValue <= other.toNumericalValue
	def > (other: NotificationPriority): Boolean = toNumericalValue > other.toNumericalValue
	def >= (other: NotificationPriority): Boolean = toNumericalValue >= other.toNumericalValue
}

object NotificationPriority {

	// Notification is complete
	case object Complete extends NotificationPriority("complete") { def toNumericalValue = 0 }
	// Notification is trivial; by default this will not generate an email
	case object Trivial extends NotificationPriority("trivial") { def toNumericalValue = 0.1 }
	// Notification is for information only
	case object Info extends NotificationPriority("info") { def toNumericalValue = 0.25 }
	// Notification is about an action that the user should take
	case object Warning extends NotificationPriority("warning") { def toNumericalValue = 0.5 }
	// Notification is about an action that is super important
	case object Critical extends NotificationPriority("critical") { def toNumericalValue = 0.75 }

	val values = Set(Complete, Trivial, Info, Warning, Critical)

	def fromDbValue(value: String): NotificationPriority =
		if (value == null) null
		else values.find{_.dbValue == value} match {
			case Some(priority) => priority
			case None =>
				throw new IllegalArgumentException(s"Invalid priority: $value")
		}
}

class NotificationPriorityUserType extends AbstractBasicUserType[NotificationPriority, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): NotificationPriority = NotificationPriority.fromDbValue(string)

	override def convertToValue(state: NotificationPriority): String = state.dbValue
}