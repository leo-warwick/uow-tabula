package uk.ac.warwick.tabula.commands.coursework.feedback

import org.mockito.Mockito._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.forms.{AssignmentFormField, SavedFormValue, StringFormValue}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{SavedFormValueDao, SavedFormValueDaoComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{CurrentUser, Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._

class OnlineFeedbackFormCommandTest extends TestBase with Mockito {

	trait Fixture {

		val assignment = new Assignment
		assignment.collectMarks = true
		assignment.addDefaultFeedbackFields()
		val module = new Module
		module.adminDepartment = new Department

		val student = new User("student"){setWarwickId("student")}
		val marker = new User("marker")
		val currentUser = new CurrentUser(marker, marker)

		val feedbackField: AssignmentFormField = assignment.feedbackFields.find(_.name == Assignment.defaultFeedbackTextFieldName).get
		feedbackField.id = Assignment.defaultFeedbackTextFieldName
		val feedbackValue = new StringFormValue(feedbackField)
		val heronRamble = "You would have got a first if you didn't mention Herons so much. I hate herons"
		feedbackValue.value = heronRamble

		val savedFormValue = new SavedFormValue()
		savedFormValue.id = Assignment.defaultFeedbackTextFieldName
		savedFormValue.name = Assignment.defaultFeedbackTextFieldName
		savedFormValue.value = heronRamble

		val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]
		gradeGenerator.applyForMarks(Map("student" -> 67)) returns Map("student" -> Seq())

		val command = new OnlineFeedbackFormCommand(module, assignment, student, currentUser.apparentUser, currentUser, gradeGenerator)
			with OnlineFeedbackFormCommandTestSupport
	}

	@Test
	def copyTo() {
		new Fixture {

			command.mark = "67"
			command.grade = "2:1"
			command.fields = JMap(Assignment.defaultFeedbackTextFieldName -> feedbackValue)

			val feedback: Feedback = command.apply()

			feedback.actualMark should be(Some(67))
			feedback.actualGrade should be(Some("2:1"))
			feedback.customFormValues.size should be(1)
		}
	}

	@Test
	def copyToWithBlankMarkAndGrade(){
		new Fixture {

			command.mark = ""
			command.grade = ""
			command.fields = JMap(Assignment.defaultFeedbackTextFieldName -> feedbackValue)

			val feedback: Feedback = command.apply()

			feedback.actualMark should be(None)
			feedback.actualGrade should be(None)
			feedback.customFormValues.size should be(1)
		}

	}

	@Test
	def invalidateZipsWhenUpdatingFeedback() {
		new Fixture {
			val existingFeedback: AssignmentFeedback = Fixtures.assignmentFeedback("student")
			existingFeedback.id = "existingFeedback"
			assignment.feedbacks.add(existingFeedback)

			when(command.savedFormValueDao.get(feedbackField, existingFeedback)) thenReturn None

			val feedback: Feedback = command.apply()

			verify(command.zipService, times(1)).invalidateIndividualFeedbackZip(existingFeedback)
		}
	}

	@Test
	def fieldsCopiedWhenUpdatingFeedback() {
		new Fixture {
			val existingFeedback: AssignmentFeedback = Fixtures.assignmentFeedback("student")
			existingFeedback.assignment = assignment
			existingFeedback.actualGrade = Option("2:2")
			existingFeedback.actualMark = Option(55)
			existingFeedback.id = "existingFeedback"
			savedFormValue.feedback = existingFeedback
			existingFeedback.customFormValues.add(savedFormValue)

			assignment.feedbacks = Seq(existingFeedback)

			val command2 = new OnlineFeedbackFormCommand(module, assignment, student, currentUser.apparentUser, currentUser, gradeGenerator)
				with OnlineFeedbackFormCommandTestSupport
			command2.mark should be("55")
			command2.grade should be("2:2")
			command2.fields.size should be (1)
			val field: StringFormValue = command2.fields.get(Assignment.defaultFeedbackTextFieldName).asInstanceOf[StringFormValue]
			field.value should be(heronRamble)
		}
	}

	@Test
	def validation() {
		new Fixture {
			var errors = new BindException(command, "command")
			command.mark = "heron"
			errors.hasErrors should be {false}
			command.validate(errors)
			errors.hasErrors should be {true}

			errors = new BindException(command, "command")
			errors.hasErrors should be {false}
			command.mark = "101"
			command.validate(errors)
			errors.hasErrors should be {true}

			errors = new BindException(command, "command")
			errors.hasErrors should be {false}
			command.mark = "-5"
			command.validate(errors)
			errors.hasErrors should be {true}

			errors = new BindException(command, "command")
			errors.hasErrors should be {false}
			command.mark = "77"
			command.validate(errors)
			errors.hasErrors should be {false}

			errors = new BindException(command, "command")
			errors.hasErrors should be {false}
			command.mark = "77.2"
			command.validate(errors)
			errors.hasErrors should be {true}

		}
	}

	@Test
	def gradeValidation() {
		trait GradeFixture extends Fixture {
			module.adminDepartment.assignmentGradeValidation = true
			val errors = new BindException(command, "command")
			command.mark = "77"
			command.gradeGenerator.applyForMarks(Map(student.getWarwickId -> command.mark.toInt)) returns Map(student.getWarwickId -> Seq(GradeBoundary(null, "A", 0, 100, null)))
		}
		new GradeFixture {
			command.grade = "F"
			command.validate(errors)
			errors.hasErrors should be {true}
		}
		new GradeFixture {
			command.grade = "A"
			command.validate(errors)
			errors.hasErrors should be {false}
		}
	}
}

trait OnlineFeedbackFormCommandTestSupport extends FileAttachmentServiceComponent with FeedbackServiceComponent
	with ZipServiceComponent with SavedFormValueDaoComponent with Mockito with ProfileServiceComponent {

	this : OnlineFeedbackFormCommand =>

	val fileAttachmentService: FileAttachmentService = smartMock[FileAttachmentService]
	val feedbackService: FeedbackService = smartMock[FeedbackService]
	val zipService: ZipService = smartMock[ZipService]
	val savedFormValueDao: SavedFormValueDao = smartMock[SavedFormValueDao]
	val profileService: ProfileService = smartMock[ProfileService]

	def apply(): Feedback = this.applyInternal()
}