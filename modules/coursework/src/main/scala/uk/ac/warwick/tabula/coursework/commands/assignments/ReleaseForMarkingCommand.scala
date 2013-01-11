package uk.ac.warwick.tabula.coursework.commands.assignments

import collection.JavaConversions._
import reflect.BeanProperty
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{StateService, AssignmentService}
import uk.ac.warwick.tabula.helpers.ArrayList
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.Daoisms

class ReleaseForMarkingCommand(val assignment: Assignment, currentUser: CurrentUser) extends Command[Unit]
	with SelfValidating with Daoisms {

	var assignmentService = Wire.auto[AssignmentService]
	var stateService = Wire.auto[StateService]

	@BeanProperty var students: JList[String] = ArrayList()
	@BeanProperty var confirm: Boolean = false
	@BeanProperty var invalidFeedback: JList[Feedback] = ArrayList()

	var feedbacksUpdated = 0

	def applyInternal() {

		// get the parent feedback or create one if none exist
		val feedbacks = students.map{ uniId:String =>
			val parentFeedback = assignment.feedbacks.find(_.universityId == uniId).getOrElse({
				val newFeedback = new Feedback
				newFeedback.assignment = assignment
				newFeedback.uploaderId = currentUser.apparentId
				newFeedback.universityId = uniId
				newFeedback.released = false
				session.saveOrUpdate(newFeedback)
				newFeedback
			})
			parentFeedback
		}

		val feedbackToUpdate:Seq[Feedback] = feedbacks -- invalidFeedback
		feedbackToUpdate foreach (f => stateService.updateState(f.retrieveFirstMarkerFeedback, ReleasedForMarking))
		feedbacksUpdated = feedbackToUpdate.size
	}

	override def describe(d: Description){
		d.assignment(assignment)
		.property("students" -> students)
	}

	override def describeResult(d: Description){
		d.assignment(assignment)
		.property("submissionCount" -> feedbacksUpdated)
	}

	def preSubmitValidation() {
		invalidFeedback = for {
			universityId <- students
			parentFeedback <- assignment.feedbacks.find(_.universityId == universityId)
			if parentFeedback.firstMarkerFeedback != null
		} yield parentFeedback
	}

	def validate(errors: Errors) {
		if (!confirm) errors.rejectValue("confirm", "submission.mark.plagiarised.confirm")
	}

}
