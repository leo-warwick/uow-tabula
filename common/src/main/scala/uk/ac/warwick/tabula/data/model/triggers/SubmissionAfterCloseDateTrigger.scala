package uk.ac.warwick.tabula.data.model.triggers

import javax.persistence.{DiscriminatorValue, Entity, Inheritance, InheritanceType}

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Assignment, Submission, ToEntityReference}

object SubmissionAfterCloseDateTrigger {
	def apply(thisScheduledDate: DateTime, thisTargetEntity: ToEntityReference): SubmissionAfterCloseDateTrigger = {
		val result = new SubmissionAfterCloseDateTrigger
		result.scheduledDate = thisScheduledDate
		result.updateTarget(thisTargetEntity)
		result
	}
}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue(value="SubmissionAfterCloseDate")
class SubmissionAfterCloseDateTrigger extends Trigger[Submission, Unit] with HandlesAssignmentTrigger {

	def submission: Submission = target.entity
	override def assignment: Assignment = submission.assignment

	override def apply(): Unit = transactional() {
		if (assignment.isClosed && (submission.isLate || submission.isAuthorisedLate)) {
			handleAssignment(Seq(submission.usercode))
		}
	}
}
