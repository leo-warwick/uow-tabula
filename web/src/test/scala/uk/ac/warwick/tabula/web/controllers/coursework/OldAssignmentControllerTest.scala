package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, CurrentUser, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.coursework.assignments._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.controllers.TestControllerOverrides
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.commands.coursework.{CurrentUserSubmissionAndFeedbackCommandState, StudentSubmissionAndFeedbackCommandInternal}
import uk.ac.warwick.tabula.commands.{Appliable, ComposableCommand}
import uk.ac.warwick.tabula.commands.coursework.StudentSubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent
import uk.ac.warwick.tabula.web.Mav

class OldAssignmentControllerTest extends TestBase with Mockito {

	/** Reusable set of test objects. Put your code in a new Fixtures { } */
	trait Fixtures {
		val user: CurrentUser
		val controller = new OldAssignmentController with TestControllerOverrides
		val assignment: Assignment = newDeepAssignment("CS101")
		val module: Module = assignment.module
		val form: SubmitAssignmentCommandInternal with ComposableCommand[Submission] with SubmitAssignmentBinding with SubmitAssignmentAsSelfPermissions with SubmitAssignmentDescription with SubmitAssignmentValidation with SubmitAssignmentNotifications with SubmitAssignmentTriggers with AutowiringSubmissionServiceComponent with AutowiringFeaturesComponent with AutowiringZipServiceComponent with AutowiringAttendanceMonitoringCourseworkSubmissionServiceComponent = SubmitAssignmentCommand.self(module, assignment, currentUser)
		val errors = new BindException(form, "command")

		val feedbackService: FeedbackService = smartMock[FeedbackService]
		val feedback = new AssignmentFeedback()
		feedbackService.getAssignmentFeedbackByUsercode(assignment, "cusebr") returns Some(feedback) thenThrows new Error("I TOLD YOU ABOUT STAIRS BRO")

		val submissionService: SubmissionService = smartMock[SubmissionService]
		submissionService.getSubmissionByUsercode(assignment, "cusebr") returns None

		val profileService: ProfileService = smartMock[ProfileService]

		val infoCommand = new StudentSubmissionAndFeedbackCommandInternal(module, assignment)
			with CurrentUserSubmissionAndFeedbackCommandState with FeedbackServiceComponent with ProfileServiceComponent
			with SubmissionServiceComponent with Appliable[StudentSubmissionInformation] {

			def currentUser: CurrentUser = user
			def feedbackService: FeedbackService = Fixtures.this.feedbackService
			def submissionService: SubmissionService = Fixtures.this.submissionService
			def profileService: ProfileService = Fixtures.this.profileService

			def apply(): StudentSubmissionInformation = applyInternal()
		}
	}

	@Test
	def feedbackAccess() {
		withUser("cusebr", "0123456") {
			new Fixtures {
				val user: CurrentUser = currentUser
				profileService.getMemberByUser(user.apparentUser, disableFilter = false, eagerLoad = false) returns None
				val mav: Mav = controller.view(infoCommand, form, errors)
				withClue(mav) { mav.map should contain key "feedback" }
			}
		}
	}

}