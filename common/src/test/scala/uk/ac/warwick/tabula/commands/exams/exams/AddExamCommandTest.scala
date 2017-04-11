package uk.ac.warwick.tabula.commands.exams.exams

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.exams._
import uk.ac.warwick.tabula.commands.{HasAcademicYear, SpecifiesGroupType}
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.services._

class AddExamCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ExamState
		with AssessmentServiceComponent
		with UserLookupComponent
		with HasAcademicYear
		with SpecifiesGroupType
		with AssessmentMembershipServiceComponent {
			val assessmentService: AssessmentService = smartMock[AssessmentService]
			val userLookup = new MockUserLookup
			var assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
		}

	trait Fixture {
		val module: Module = Fixtures.module("ab123", "Test module")
		val academicYear = new AcademicYear(2014)
		val command = new AddExamCommandInternal(module, academicYear) with CommandTestSupport

		val validator = new ExamValidation with ExamState with AssessmentServiceComponent
			with UserLookupComponent with HasAcademicYear with SpecifiesGroupType
			with AssessmentMembershipServiceComponent {

			def module: Module = command.module
			def academicYear: AcademicYear = command.academicYear

			override val assessmentService: AssessmentService = smartMock[AssessmentService]
			override val assessmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
			override val userLookup: UserLookupService = smartMock[UserLookupService]
			override def existingGroups = None
			override def existingMembers = None
			override def updateAssessmentGroups() = List()
		}
	}

	@Test def apply() { new Fixture {
		command.name = "Some exam"

		val exam: Exam = command.applyInternal()
		exam.name should be ("Some exam")

		verify(command.assessmentService, times(1)).save(exam)
	}}

	@Test def rejectEmptyCode() { new Fixture {

		validator.name = "    "

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("exam.name.empty")
	}}

	@Test def validateValid() { new Fixture {

		def name = "ab123"
		validator.name = name

		validator.assessmentService.getExamByNameYearModule(name, academicYear ,module) returns Seq()

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.getErrorCount should be (0)
		verify(validator.assessmentService, times(1)).getExamByNameYearModule(name, academicYear ,module)
		verify(validator.assessmentService, atMost(1)).getExamByNameYearModule(any[String], any[AcademicYear], any[Module])
	}}

	@Test def rejectIfDuplicateName() { new Fixture {

		def name = "exam1"
		validator.name = name

		validator.assessmentService.getExamByNameYearModule(name, academicYear ,module) returns Seq(Fixtures.exam(name))

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldErrorCount("name") should be (1)
		verify(validator.assessmentService, times(1)).getExamByNameYearModule(name, academicYear ,module)
		verify(validator.assessmentService, atMost(1)).getExamByNameYearModule(any[String], any[AcademicYear], any[Module])
	}}
}
