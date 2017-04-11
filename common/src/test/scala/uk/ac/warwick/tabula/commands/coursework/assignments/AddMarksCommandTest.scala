package uk.ac.warwick.tabula.commands.coursework.assignments

import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{Assignment, GradeBoundary, Module}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.coursework.docconversion.MarkItem
import uk.ac.warwick.tabula.{CurrentUser, Mockito, RequestInfo, TestBase}

import scala.collection.JavaConversions._

// scalastyle:off magic.number
class AddMarksCommandTest extends TestBase with Mockito {

	val thisAssignment: Assignment = newDeepAssignment()

	/**
	 * Check that validation marks an empty mark as an invalid row
	 * so that the apply method skips it.
	 */

	@Test
	def emptyMarkField() {
		withUser("cusebr") {
			val currentUser = RequestInfo.fromThread.get.user
			val validator = new PostExtractValidation with AdminAddMarksCommandState with ValidatesMarkItem with UserLookupComponent with AdminAddMarksCommandValidation {
				val module: Module = thisAssignment.module
				val assessment: Assignment = thisAssignment
				val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]
				val userLookup: UserLookupService = smartMock[UserLookupService]
				val submitter: CurrentUser = null
			}
			validator.userLookup.getUserByWarwickUniId("0672088") answers { id =>
				currentUser.apparentUser
			}

			val errors = new BindException(validator, "command")

			val marks1 = validator.marks.get(0)
			marks1.universityId = "0672088"
			marks1.actualMark = ""

			val marks2 = validator.marks.get(1)
			marks2.universityId = "1235"
			marks2.actualMark = "5"

			validator.postExtractValidation(errors)
		}
	}

	/**
	 * Check that validation disallows grade to be non-empty when mark is empty,
	 * if grade validation is set.
	 */
	@Transactional @Test
	def gradeButEmptyMarkFieldGradeValidation() {
		withUser("cusebr") {
			val currentUser = RequestInfo.fromThread.get.user
			val validator = new PostExtractValidation with AdminAddMarksCommandState with ValidatesMarkItem with UserLookupComponent with AdminAddMarksCommandValidation {
				val module: Module = thisAssignment.module
				module.adminDepartment.assignmentGradeValidation = true
				val assessment: Assignment = thisAssignment
				val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]
				val userLookup: UserLookupService = smartMock[UserLookupService]
				val submitter: CurrentUser = null
			}
			validator.userLookup.getUserByWarwickUniId("0672088") answers { id =>
				currentUser.apparentUser
			}

			val errors = new BindException(validator, "command")

			val marks1 = validator.marks.get(0)
			marks1.universityId = "0672088"
			marks1.actualMark = ""
			marks1.actualGrade = "EXCELLENT"

			val marks2 = validator.marks.get(1)
			marks2.universityId = "55555"
			marks2.actualMark = "65"

			val marks3 = validator.marks.get(2)
			marks3.universityId = "1235"
			marks3.actualMark = "A"

			val marks4 = validator.marks.get(3)
			marks4.universityId = ""
			marks4.actualMark = "80"
			marks4.actualGrade = "EXCELLENT"

			val marks5 = validator.marks.get(4)
			marks5.universityId = "0672088"
			marks5.actualMark = ""
			marks5.actualGrade = ""

			validator.postExtractValidation(errors)
			validator.marks.count(_.isValid) should be (0)
		}
	}

	/**
	 * Check that validation allows grade to be non-empty when mark is empty,
	 * if grade validation is not set.
	 */
	@Transactional @Test
	def gradeButEmptyMarkFieldNoGradeValidation() {
		withUser("cusebr") {
			val currentUser = RequestInfo.fromThread.get.user
			val validator = new PostExtractValidation with AdminAddMarksCommandState with ValidatesMarkItem with UserLookupComponent with AdminAddMarksCommandValidation {
				val module: Module = thisAssignment.module
				module.adminDepartment.assignmentGradeValidation = false
				val assessment: Assignment = thisAssignment
				val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]
				val userLookup: UserLookupService = smartMock[UserLookupService]
				val submitter: CurrentUser = null
			}
			validator.userLookup.getUserByWarwickUniId("0672088") answers { id =>
				currentUser.apparentUser
			}

			val errors = new BindException(validator, "command")

			val marks1 = validator.marks.get(0)
			marks1.universityId = "0672088"
			marks1.actualMark = ""
			marks1.actualGrade = "EXCELLENT"

			val marks2 = validator.marks.get(1)
			marks2.universityId = "55555"
			marks2.actualMark = "65"

			val marks3 = validator.marks.get(2)
			marks3.universityId = "1235"
			marks3.actualMark = "A"

			val marks4 = validator.marks.get(3)
			marks4.universityId = ""
			marks4.actualMark = "80"
			marks4.actualGrade = "EXCELLENT"

			val marks5 = validator.marks.get(4)
			marks5.universityId = "0672088"
			marks5.actualMark = ""
			marks5.actualGrade = ""

			validator.postExtractValidation(errors)
			validator.marks.count(_.isValid) should be (1)
		}
	}

	@Transactional @Test
	def gradeValidation() {
		trait Fixture {
			val currentUser: CurrentUser = RequestInfo.fromThread.get.user
			val newAssignment: Assignment = newDeepAssignment()
			val validator = new PostExtractValidation with AdminAddMarksCommandState with ValidatesMarkItem with UserLookupComponent with AdminAddMarksCommandValidation {
				val module: Module = newAssignment.module
				val assessment: Assignment = newAssignment
				val gradeGenerator: GeneratesGradesFromMarks = smartMock[GeneratesGradesFromMarks]
				val userLookup: UserLookupService = smartMock[UserLookupService]
				val submitter: CurrentUser = null
			}
			validator.userLookup.getUserByWarwickUniId("0672088") answers { id =>
				currentUser.apparentUser
			}
			validator.gradeGenerator.applyForMarks(Map("0672088" -> 100)) returns Map("0672088" -> Seq(GradeBoundary(null, "A", 0, 100, null)))
			newAssignment.module.adminDepartment.assignmentGradeValidation = true
		}

		withUser("cusebr") { new Fixture {
			val errors = new BindException(validator, "command")

			val marks1: MarkItem = validator.marks.get(0)
			marks1.universityId = "0672088"
			marks1.actualMark = "100"
			marks1.actualGrade = "F"

			validator.postExtractValidation(errors)

			errors.hasErrors should be {true}
		}}

		withUser("cusebr") { new Fixture {
			val errors = new BindException(validator, "command")

			val marks1: MarkItem = validator.marks.get(0)
			marks1.universityId = "0672088"
			marks1.actualMark = "100"
			marks1.actualGrade = "A"

			validator.postExtractValidation(errors)

			errors.hasErrors should be {false}
		}}
	}

}