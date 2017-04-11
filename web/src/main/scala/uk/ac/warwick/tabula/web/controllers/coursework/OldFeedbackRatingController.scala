package uk.ac.warwick.tabula.web.controllers.coursework

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.commands.coursework.feedback.RateFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.Features
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.spring.Wire

@RequestMapping(Array("/${cm1.prefix}/module/{module}/{assignment}/rate"))
@Profile(Array("cm1Enabled")) @Controller
class OldFeedbackRatingController extends OldCourseworkController {

	var feedbackDao: FeedbackDao = Wire.auto[FeedbackDao]

	hideDeletedItems

	@ModelAttribute def cmd(
		@PathVariable assignment: Assignment,
		@PathVariable module: Module,
		user: CurrentUser) =
		new RateFeedbackCommand(module, assignment, mandatory(feedbackDao.getAssignmentFeedbackByUsercode(assignment, user.userId).filter(_.released)))

	@RequestMapping(method = Array(GET, HEAD))
	def form(command: RateFeedbackCommand): Mav =
		Mav(s"$urlPrefix/submit/rating").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(command: RateFeedbackCommand, errors: Errors): Mav = {
		command.validate(errors)
		if (errors.hasErrors) {
			form(command)
		} else {
			command.apply()
			Mav(s"$urlPrefix/submit/rating", "rated" -> true).noLayoutIf(ajax)
		}
	}

}