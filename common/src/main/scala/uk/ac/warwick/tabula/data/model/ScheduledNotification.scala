package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime

import scala.annotation.meta.field

@Entity(name = "Scheduled_Notification")
@Proxy
class ScheduledNotification[A >: Null <: ToEntityReference](
  // holds the discriminator value for the notification that will be spawned when this is completed
  @(Column@field)(name = "notification_type")
  var notificationType: String,

  targetEntity: ToEntityReference,

  @(Column@field)(name = "scheduled_date")
  var scheduledDate: DateTime
) extends GeneratedId with Serializable {

  def this() = {
    this(null, null, null)
  }

  @transient private[this] var _target: EntityReference[A] = Option(targetEntity).map { e =>
    e.toEntityReference.asInstanceOf[EntityReference[A]]
  }.orNull

  @Access(value = AccessType.PROPERTY)
  @OneToOne(cascade = Array(CascadeType.ALL), targetEntity = classOf[EntityReference[_]], fetch = FetchType.LAZY)
  def getTarget: EntityReference[A] = _target

  def target: EntityReference[A] = getTarget

  def setTarget(target: EntityReference[A]): Unit = _target = target

  def target_=(target: EntityReference[A]): Unit = setTarget(target)

  var completed: Boolean = false
}
