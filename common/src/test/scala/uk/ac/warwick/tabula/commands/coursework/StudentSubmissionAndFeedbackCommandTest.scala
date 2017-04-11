package uk.ac.warwick.tabula.commands.coursework

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.coursework.StudentSubmissionAndFeedbackCommand.StudentSubmissionInformation
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class StudentSubmissionAndFeedbackCommandTest extends TestBase with Mockito {

	private trait CommandTestSupport extends StudentSubmissionAndFeedbackCommandState
		with SubmissionServiceComponent with FeedbackServiceComponent with ProfileServiceComponent {
		val submissionService: SubmissionService = smartMock[SubmissionService]
		val feedbackService: FeedbackService = smartMock[FeedbackService]
		val profileService: ProfileService = smartMock[ProfileService]
	}

	private trait Fixture {
		val module: Module = Fixtures.module("in101")
		val assignment: Assignment = Fixtures.assignment("Writing test")
		assignment.allowResubmission = false
		assignment.openEnded = true
		assignment.openDate = DateTime.now.minusDays(1)
		assignment.collectSubmissions = true

		val user = new User("cuscav")
		user.setWarwickId("0672089")

		val student: StudentMember = Fixtures.student(user.getWarwickId, user.getUserId)
		student.disability = Fixtures.disability("Test")
	}

	private trait CommandFixture extends Fixture {
		val command = new StudentSubmissionAndFeedbackCommandInternal(module, assignment) with CommandTestSupport {
			val studentUser: User = user
			val viewer: User = user
		}
	}

	@Test def apply() { new CommandFixture {
		val submission: Submission = Fixtures.submission()
		submission.submitted = true

		val feedback: AssignmentFeedback = Fixtures.assignmentFeedback()
		feedback.released = true

		val extension: Extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = DateTime.now.plusDays(5)

		assignment.extensions.add(extension)

		command.submissionService.getSubmissionByUsercode(assignment, "cuscav") returns Some(submission)
		command.feedbackService.getAssignmentFeedbackByUsercode(assignment, "cuscav") returns Some(feedback)
		command.profileService.getMemberByUser(user, disableFilter = false, eagerLoad = false) returns None

		val info: StudentSubmissionInformation = command.applyInternal()
		info.submission should be (Some(submission))
		info.feedback should be (Some(feedback))
		info.extension should be (Some(extension))
		info.isExtended should be {true}
		info.extensionRequested should be {false}
		info.canSubmit should be {true}
		info.canReSubmit should be {false}
		info.hasDisability should be {false}
	}}

	@Test def applyWithDisability() { new CommandFixture {
		val submission: Submission = Fixtures.submission()
		submission.submitted = true

		val feedback: AssignmentFeedback = Fixtures.assignmentFeedback()
		feedback.released = true

		val extension: Extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = DateTime.now.plusDays(5)

		assignment.extensions.add(extension)

		command.submissionService.getSubmissionByUsercode(assignment, "cuscav") returns Some(submission)
		command.feedbackService.getAssignmentFeedbackByUsercode(assignment, "cuscav") returns Some(feedback)
		command.profileService.getMemberByUser(user, disableFilter = false, eagerLoad = false) returns Option(student)

		val info: StudentSubmissionInformation = command.applyInternal()
		info.submission should be (Some(submission))
		info.feedback should be (Some(feedback))
		info.extension should be (Some(extension))
		info.isExtended should be {true}
		info.extensionRequested should be {false}
		info.canSubmit should be {true}
		info.canReSubmit should be {false}
		info.hasDisability should be {true}
	}}

	@Test def currentUserPermissions() { withUser("cuscav", "0672089") {
		val u = currentUser
		val command = new CurrentUserSubmissionAndFeedbackCommandPermissions with CurrentUserSubmissionAndFeedbackCommandState with CommandTestSupport {
			val module: Module = Fixtures.module("in101")
			val assignment: Assignment = Fixtures.assignment("Writing")
			assignment.module = module

			val currentUser: CurrentUser = u
		}

		command.submissionService.getSubmissionByUsercode(command.assignment, "cuscav") returns None
		command.feedbackService.getAssignmentFeedbackByUsercode(command.assignment, "cuscav") returns None

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheckAny(Seq(CheckablePermission(Permissions.Submission.Create, Some(command.assignment))))
	}}

	@Test def memberPermissions() {
		val m = Fixtures.student("0672089", "cuscav")
		val command = new StudentMemberSubmissionAndFeedbackCommandPermissions with StudentMemberSubmissionAndFeedbackCommandState with CommandTestSupport {
			val module: Module = Fixtures.module("in101")
			val assignment: Assignment = Fixtures.assignment("Writing")
			assignment.module = module

			val studentMember: StudentMember = m
			val currentUser: CurrentUser = smartMock[CurrentUser]
		}

		val checking = mock[PermissionsChecking]
		command.permissionsCheck(checking)

		verify(checking, times(1)).PermissionCheck(Permissions.Submission.Read, m)
		verify(checking, times(1)).PermissionCheck(Permissions.AssignmentFeedback.Read, m)
	}

}
