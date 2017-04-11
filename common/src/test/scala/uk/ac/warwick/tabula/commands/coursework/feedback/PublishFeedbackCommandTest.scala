package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.services.GeneratesGradesFromMarks
import uk.ac.warwick.tabula.{CurrentUser, Mockito, TestBase}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentFeedback, Feedback}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number
class PublishFeedbackCommandTest extends TestBase with Mockito {

	@Test
	def validation() {
		val now = dateTime(2012, 12).withDayOfMonth(25)
		val closeDateBefore = now.minusDays(2)
		val closeDateAfter = now.plusDays(2)

		withFakeTime(now) {

			/* a whole */ new World {
				assignment.closeDate = closeDateAfter
				assignment.openEnded = false
				command.validate(errors)
				errors.hasGlobalErrors should be {true}
				errors.getGlobalError.getCode should be ("feedback.publish.notclosed")
			}

			/* a whole */ new World {
				assignment.closeDate = closeDateBefore
				assignment.openEnded = false
				command.validate(errors)
				errors.hasGlobalErrors should be {false}
			}

			/* Brave */ new World {
				assignment.openEnded = true
				command.validate(errors)
				errors.hasGlobalErrors should be {false}
			}

		}
	}


	// reusable environment
	trait World {
		val user = new User("admin")
		val currentUser = new CurrentUser(user, user)
		val assignment: Assignment = newDeepAssignment(moduleCode = "IN101")
		val command = PublishFeedbackCommand(assignment.module, assignment, currentUser, smartMock[GeneratesGradesFromMarks])
		val errors = new BindException(command, "command")
		val feedback = new AssignmentFeedback
		feedback.actualMark = Option(41)
		feedback.assignment = assignment
		assignment.feedbacks = JArrayList( feedback )
	}

}