package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence.{DiscriminatorValue, Entity, Inheritance, InheritanceType}

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, ToEntityReference}

import scala.collection.JavaConverters._

object AssignmentClosedTrigger {
	def apply(thisScheduledDate: DateTime, thisTargetEntity: ToEntityReference): AssignmentClosedTrigger = {
		val result = new AssignmentClosedTrigger
		result.scheduledDate = thisScheduledDate
		result.updateTarget(thisTargetEntity)
		result
	}
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value="AssignmentClosed")
class AssignmentClosedTrigger extends Trigger[Assignment, Unit] with HandlesAssignmentTrigger {

	override def assignment: Assignment = target.entity

	override def apply(): Unit = transactional() {
		if (assignment.isClosed) {
			handleAssignment(assignment.submissions.asScala.map(_.usercode))
		}

	}
}
