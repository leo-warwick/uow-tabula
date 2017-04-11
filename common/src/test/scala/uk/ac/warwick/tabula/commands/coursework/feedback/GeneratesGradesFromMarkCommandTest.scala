package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AssessmentMembershipService, AssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class GeneratesGradesFromMarkCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends GenerateGradesFromMarkCommandRequest with AssessmentMembershipServiceComponent

	trait Fixture {
		val module: Module = Fixtures.module("its01")
		val assignment: Assignment = Fixtures.assignment("Test")
		assignment.academicYear = AcademicYear(2014)
		val mockAssignmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
		val studentUser = new User("student")
		studentUser.setWarwickId("studentUniId")
		val assessmentGroup = new AssessmentGroup
		assessmentGroup.membershipService = mockAssignmentMembershipService
		assessmentGroup.occurrence = "A"
		assignment.assessmentGroups = JList(assessmentGroup)
		val upstreamAssessmentComponent: AssessmentComponent = Fixtures.upstreamAssignment(module, 0)
		assessmentGroup.assessmentComponent = upstreamAssessmentComponent
		val upstreamAssesmentGroup: UpstreamAssessmentGroup = Fixtures.assessmentGroup(AcademicYear(2014), "A", module.code, null)
		assessmentGroup.membershipService.getUpstreamAssessmentGroup(any[UpstreamAssessmentGroup]) returns Option(upstreamAssesmentGroup)
		upstreamAssesmentGroup.members = JArrayList(new UpstreamAssessmentGroupMember(upstreamAssesmentGroup, studentUser.getWarwickId))

		mockAssignmentMembershipService.determineMembershipUsers(assignment) returns Seq(studentUser)

		val command = new GenerateGradesFromMarkCommandInternal(module, assignment) with CommandTestSupport {
			val assessmentMembershipService: AssessmentMembershipService = mockAssignmentMembershipService
		}
	}

	@Test
	def valid(): Unit = new Fixture {
		command.studentMarks = JHashMap(studentUser.getWarwickId -> "100")
		val gb = GradeBoundary(null, "A", 0, 100, null)
		mockAssignmentMembershipService.gradesForMark(upstreamAssessmentComponent, 100) returns Seq(gb)
		val result: Map[String, Seq[GradeBoundary]] = command.applyInternal()
		result should be (Map(studentUser.getWarwickId -> Seq(gb)))
		verify(mockAssignmentMembershipService, times(1)).gradesForMark(upstreamAssessmentComponent, 100)
	}

	@Test
	def studentNotMember(): Unit = new Fixture {
		command.studentMarks = JHashMap("noSuchUser" -> "100")
		val result: Map[String, Seq[GradeBoundary]] = command.applyInternal()
		result should be (Map("noSuchUser" -> Seq()))
	}

	@Test
	def nullMark(): Unit = new Fixture {
		command.studentMarks = JHashMap("noSuchUser" -> null)
		val result: Map[String, Seq[GradeBoundary]] = command.applyInternal()
		result should be (Map("noSuchUser" -> Seq()))
	}

	@Test
	def notIntMark(): Unit = new Fixture {
		command.studentMarks = JHashMap("noSuchUser" -> "fifty")
		val result: Map[String, Seq[GradeBoundary]] = command.applyInternal()
		result should be (Map("noSuchUser" -> Seq()))
	}

	@Test
	def notInAssessmentComponents(): Unit = new Fixture {
		val otherStudentUser = new User("student1")
		mockAssignmentMembershipService.determineMembershipUsers(assignment) returns Seq(otherStudentUser)
		command.studentMarks = JHashMap(otherStudentUser.getUserId -> "100")
		val result: Map[String, Seq[GradeBoundary]] = command.applyInternal()
		result should be (Map(otherStudentUser.getUserId -> Seq()))
	}

	@Test
	def noMarksFromService(): Unit = new Fixture {
		command.studentMarks = JHashMap(studentUser.getWarwickId -> "100")
		mockAssignmentMembershipService.gradesForMark(upstreamAssessmentComponent, 100) returns Seq()
		val result: Map[String, Seq[GradeBoundary]] = command.applyInternal()
		result should be (Map(studentUser.getWarwickId -> Seq()))
		verify(mockAssignmentMembershipService, times(1)).gradesForMark(upstreamAssessmentComponent, 100)
	}

}
