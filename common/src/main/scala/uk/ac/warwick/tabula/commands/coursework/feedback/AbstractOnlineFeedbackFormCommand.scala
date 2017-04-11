package uk.ac.warwick.tabula.commands.coursework.feedback

import uk.ac.warwick.tabula.services.{ProfileServiceComponent, GeneratesGradesFromMarks}

import collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.commands.SelfValidating
import org.springframework.validation.{Errors, BindingResult}
import uk.ac.warwick.userlookup.User

import scala.util.Try

abstract class AbstractOnlineFeedbackFormCommand(val module: Module, val assignment: Assignment, val student: User, val marker: User, val gradeGenerator: GeneratesGradesFromMarks)
	extends OnlineFeedbackState with OnlineFeedbackStudentState with SubmissionState with BindListener with SelfValidating with ProfileServiceComponent {

	def submission: Option[Submission] = assignment.findSubmission(student.getUserId)

	override def onBind(result:BindingResult) {
		if (fields != null) {
			for ((key, field) <- fields.asScala) {
				field.onBind(result)
			}
		}
		file.onBind(result)
	}

	override def validate(errors: Errors) {
		if(!hasContent) {
			errors.reject("feedback.empty")
		}

		fieldValidation(errors)
	}

	def fieldValidation(errors:Errors) {
		// Individually validate all the custom fields
		if(fields != null){
			assignment.feedbackFields.foreach { field =>
				errors.pushNestedPath("fields[%s]".format(field.id))
				fields.asScala.get(field.id).foreach(field.validate(_, errors))
				errors.popNestedPath()
			}
		}

		if (mark.hasText) {
			try {
				val asInt = mark.toInt
				if (asInt < 0 || asInt > 100) {
					errors.rejectValue("mark", "actualMark.range")
				}
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
					errors.rejectValue("mark", "actualMark.format")
			}
		}

		// validate grade is department setting is true
		if (!errors.hasErrors && grade.hasText && module.adminDepartment.assignmentGradeValidation) {
			val validGrades = Try(mark.toInt).toOption.toSeq.flatMap { m => gradeGenerator.applyForMarks(Map(student.getWarwickId -> m))(student.getWarwickId) }
			if (validGrades.nonEmpty && !validGrades.exists(_.grade == grade)) {
				errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
			}
		}
	}
}

trait SubmissionState {

	self: ProfileServiceComponent =>

	def assignment: Assignment
	def submission: Option[Submission]
	def student: User

	def submissionState: String = {
		submission match {
			case Some(s) if s.isAuthorisedLate => "workflow.Submission.authorisedLate"
			case Some(s) if s.isLate => "workflow.Submission.late"
			case Some(_) => "workflow.Submission.onTime"
			case None if !assignment.isClosed => "workflow.Submission.unsubmitted.withinDeadline"
			case None if assignment.extensions.asScala.exists(e => e.usercode == student.getUserId && e.expiryDate.exists(_.isBeforeNow))
				=> "workflow.Submission.unsubmitted.withinExtension"
			case None => "workflow.Submission.unsubmitted.late"
		}
	}

	def disability: Option[Disability] = {
		if (submission.exists(s => s.useDisability)) {
			profileService.getMemberByUniversityId(student.getWarwickId).flatMap {
				case student: StudentMember => Option(student)
				case _ => None
			}.flatMap(s => s.disability)
		} else {
			None
		}
	}
}