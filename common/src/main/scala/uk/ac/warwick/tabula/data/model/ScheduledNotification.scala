package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.joda.time.DateTime
import scala.beans.BeanProperty
import scala.annotation.meta.field

@Entity(name = "Scheduled_Notification")
class ScheduledNotification[A >: Null <: ToEntityReference](

		// holds the discriminator value for the notification that will be spawned when this is completed
		@(Column @field)(name="notification_type")
		var notificationType: String,

		targetEntity: ToEntityReference,

		@(Column @field)(name="scheduled_date")
		var scheduledDate: DateTime

	) extends GeneratedId with Serializable {

	def this() {
		this(null, null, null)
	}

	@Access(value=AccessType.PROPERTY)
	@OneToOne(cascade = Array(CascadeType.ALL), targetEntity = classOf[EntityReference[A]], fetch = FetchType.LAZY)
	@BeanProperty
	var target: EntityReference[A] = Option(targetEntity).map { e =>
		e.toEntityReference.asInstanceOf[EntityReference[A]]
	}.orNull

	var completed: Boolean = false
}