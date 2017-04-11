package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence._
import javax.validation.constraints.NotNull

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{EntityReference, GeneratedId, ToEntityReference}

import scala.beans.BeanProperty

@Entity(name = "ScheduledTrigger")
@DiscriminatorColumn(name = "TRIGGER_TYPE", discriminatorType = DiscriminatorType.STRING)
abstract class Trigger[A >: Null <: ToEntityReference, B] extends GeneratedId with Serializable with Appliable[B] {

	@Column(name="scheduled_date")
	@NotNull
	var scheduledDate: DateTime = null

	@Access(value=AccessType.PROPERTY)
	@OneToOne(cascade = Array(CascadeType.ALL), targetEntity = classOf[EntityReference[A]], fetch = FetchType.LAZY)
	@BeanProperty
	var target: EntityReference[A] = null

	@Column(name="completed_date")
	var completedDate: DateTime = null

	def updateTarget(targetEntity: ToEntityReference): Unit = {
		target = Option(targetEntity).map { e =>
			e.toEntityReference.asInstanceOf[EntityReference[A]]
		}.orNull
	}
}
