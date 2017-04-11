package uk.ac.warwick.tabula.commands.coursework.feedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.JavaImports._
import org.junit.Test
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.model._
import org.springframework.web.bind.WebDataBinder
import org.springframework.validation.DataBinder
import org.springframework.beans.PropertyValues
import org.springframework.beans.MutablePropertyValues
import org.springframework.web.bind.ServletRequestDataBinder
import uk.ac.warwick.tabula.Fixtures

class RateFeedbackCommandTest extends TestBase {
	@Test def nullRating {
		val (feedback,_,_,_) = deepFeedback
		val command = new RateFeedbackCommand(feedback.assignment.module, feedback.assignment, feedback)
		command.features = emptyFeatures

		command.wasPrompt.value = null
		command.wasPrompt.unset = true
		command.wasHelpful.value = null
		command.wasHelpful.unset = true
		val errors = new BindException(command, "command")
		command.validate(errors)
		withClue(errors) { errors.hasErrors should be (false) }
	}

	@Test def invalidRating {
		val (feedback,_,_,_) = deepFeedback
		val command = new RateFeedbackCommand(feedback.assignment.module, feedback.assignment, feedback)
		command.features = emptyFeatures

		command.wasPrompt.value = null
		command.wasPrompt.unset = false
		val errors = new BindException(command, "command")
		command.validate(errors)
		errors.hasFieldErrors("wasPrompt") should be (true)
	}

	def deepFeedback: (AssignmentFeedback, Assignment, Module, Department) = {
		val feedback = Fixtures.assignmentFeedback("1234567")
		val assignment = Fixtures.assignment("my assignment")
		val module = Fixtures.module("cs118", "Introduction to programming")
		val department = Fixtures.department("cs", "Computer Science")
		department.collectFeedbackRatings = true
		feedback.ratingHelpful = None
		feedback.ratingPrompt = None
		feedback.assignment = assignment
		assignment.module = module
		module.adminDepartment = department
		(feedback, assignment, module, department)
	}
}
