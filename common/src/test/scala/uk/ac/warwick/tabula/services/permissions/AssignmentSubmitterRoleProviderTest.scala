package uk.ac.warwick.tabula.services.permissions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.roles.AssignmentSubmitter
import uk.ac.warwick.tabula.services.AssessmentService
import org.mockito.Matchers._
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.services.AssessmentMembershipService

class AssignmentSubmitterRoleProviderTest extends TestBase with Mockito {

	val provider = new AssignmentSubmitterRoleProvider
	val assignmentMembershipService: AssessmentMembershipService = mock[AssessmentMembershipService]

	@Test def unrestrictedAssignment = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.module = Fixtures.module("in101")
		assignment.module.adminDepartment = Fixtures.department("in")

		assignment.restrictSubmissions = false

		provider.getRolesFor(currentUser, assignment) should be (Seq(AssignmentSubmitter(assignment)))
	}

	@Test def canSubmit = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.module = Fixtures.module("in101")
		assignment.module.adminDepartment = Fixtures.department("in")

		assignment.assessmentMembershipService = assignmentMembershipService
		assignment.restrictSubmissions = true

		assignmentMembershipService.isStudentMember(
				isEq(currentUser.apparentUser),
				isA[Seq[UpstreamAssessmentGroup]],
				isA[Option[UserGroup]]) returns (true)

		provider.getRolesFor(currentUser, assignment) should be (Seq(AssignmentSubmitter(assignment)))
	}

	@Test def cannotSubmit = withUser("cuscav") {
		val assignment = Fixtures.assignment("my assignment")
		assignment.assessmentMembershipService = assignmentMembershipService
		assignment.restrictSubmissions = true

		assignmentMembershipService.isStudentMember(
				isEq(currentUser.apparentUser),
				isA[Seq[UpstreamAssessmentGroup]],
				isA[Option[UserGroup]]) returns (false)

		provider.getRolesFor(currentUser, assignment) should be (Seq())
	}

	@Test def handlesDefault = withUser("cuscav") {
		provider.getRolesFor(currentUser, Fixtures.department("in", "IT Services")) should be (Seq())
	}

}