package uk.ac.warwick.tabula.helpers.coursework

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils

import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.CaseObjectEqualityFixes
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand.Student
import uk.ac.warwick.tabula.data.convert.JodaDateTimeConverter
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted

/**
 * Filters a set of "Student" case objects (which are a representation of the current
 * state of a single student's submission workflow on an assignment, containing the
 * submission, extension and feedback where available). Provides a predicate for
 * filtering Student objects, and an applies() method to see whether it is even relevant
 * for an assigment (for example, if an assignment doesn't take submissions, there's no
 * point offering a filter for Unsubmitted students).
 */
sealed abstract class CourseworkFilter extends CaseObjectEqualityFixes[CourseworkFilter] {
	def getName: String = CourseworkFilters.shortName(getClass)
	def getDescription: String
	def predicate(parameters: Map[String, String])(student: Student): Boolean
	def applies(assignment: Assignment): Boolean
	def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors): Unit
	def parameters: Seq[(String, String, String)]
}

abstract class ParameterlessCourseworkFilter extends CourseworkFilter {
	def predicate(student: Student): Boolean
	final def predicate(parameters: Map[String, String])(student: Student): Boolean = predicate(student)
	final def validate(parameters: Map[String, String], fieldName: String)(errors: Errors) {}
	final override def parameters = Seq()
}

object CourseworkFilters {
	private val ObjectClassPrefix = CourseworkFilters.getClass.getName
	lazy val AllFilters = Seq(
		AllStudents, Submitted, SubmittedBetweenDates, OnTime, Late, WithExtension, WithinExtension, WithWordCount,
		SubmissionNotDownloaded, Unsubmitted, NotReleasedForMarking, NotMarked, MarkedByFirst, MarkedBySecond,
		CheckedForPlagiarism, NotCheckedForPlagiarism, MarkedPlagiarised, WithOverlapPercentage, NoFeedback,
		FeedbackNotReleased, FeedbackNotDownloaded
	)

	/**
	 * Create a Filter from a name (e.g. "AllStudents").
	 * Most likely useful in view templates.
	 *
	 * Note that, like the templates they're used in, the correctness isn't
	 * checked at runtime.
	 */
	def of(name: String): CourseworkFilter = {
		try {
			// Go through the magical hierarchy
			val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
			clz.getDeclaredField("MODULE$").get(null).asInstanceOf[CourseworkFilter]
		} catch {
			case e: ClassNotFoundException =>
				throw new IllegalArgumentException("Filter " + name + " not recognised")
			case e: ClassCastException =>
				throw new IllegalArgumentException("Filter " + name + " is not an endpoint of the hierarchy")
		}
	}

	def shortName(clazz: Class[_ <: CourseworkFilter]): String
	= clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')

	case object AllStudents extends ParameterlessCourseworkFilter {
		def getDescription = "students"
		def predicate(item: Student): Boolean = {
			true
		}
		def applies(assignment: Assignment) = true
	}

	case object SubmissionNotDownloaded extends ParameterlessCourseworkFilter {
		def getDescription = "submissions not downloaded by staff"
		def predicate(item: Student): Boolean = item.coursework.enhancedSubmission.exists(!_.downloaded)
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions
	}

	case object SubmittedBetweenDates extends CourseworkFilter {
		final val converter = new JodaDateTimeConverter

		def getDescription = "students who submitted between..."

		def parameters = Seq(
			("startDate", "Start date", "datetime"),
			("endDate", "End date", "datetime")
		)
		def predicate(parameters: Map[String, String])(item: Student): Boolean = {
			val start = converter.convertRight(parameters("startDate"))
			val end = converter.convertRight(parameters("endDate"))

			def betweenDates(dt: DateTime) =
				dt != null &&
				(dt == start || dt.isAfter(start)) &&
				(dt == end || dt.isBefore(end))

			item.coursework.enhancedSubmission.exists(item => betweenDates(item.submission.submittedDate))
		}
		def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[startDate]".format(fieldName), "NotEmpty")
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[endDate]".format(fieldName), "NotEmpty")

			if (!errors.hasErrors) {
				val start = converter.convertRight(parameters("startDate"))
				if (start == null) errors.rejectValue("%s[startDate]".format(fieldName), "typeMismatch.org.joda.time.DateTime")

				val end = converter.convertRight(parameters("endDate"))
				if (end == null) errors.rejectValue("%s[endDate]".format(fieldName), "typeMismatch.org.joda.time.DateTime")

				if (start != null && end != null && !end.isAfter(start))
					errors.rejectValue("%s[endDate]".format(fieldName), "filters.SubmittedBetweenDates.end.beforeStart")
			}
		}
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions
	}

	case object OnTime extends ParameterlessCourseworkFilter {
		def getDescription = "students who submitted on time"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedSubmission.exists(item => !item.submission.isLate && !item.submission.isAuthorisedLate)
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions
	}

	case object Late extends ParameterlessCourseworkFilter {
		def getDescription = "students who submitted late"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedSubmission.exists(item => item.submission.isLate && !item.submission.isAuthorisedLate)
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions
	}

	case object WithExtension extends ParameterlessCourseworkFilter {
		def getDescription = "students with extensions"
		def predicate(item: Student): Boolean = {
			item.coursework.enhancedExtension.isDefined
		}
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions && assignment.allowExtensions
	}

	case object WithinExtension extends ParameterlessCourseworkFilter {
		def getDescription = "students who submitted within extension"
		def predicate(item: Student): Boolean = {
			item.coursework.enhancedSubmission.exists(_.submission.isAuthorisedLate)
		}
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions && assignment.allowExtensions
	}

	case object WithWordCount extends CourseworkFilter {
		private def toInt(text: String) =
			if (text.hasText) try {	Some(text.toInt) } catch { case e: NumberFormatException => None }
			else None

		def getDescription = "students who submitted with word count between..."

		def parameters = Seq(
			("minWords", "Min word count", "number"),
			("maxWords", "Max word count", "number")
		)
		def predicate(parameters: Map[String, String])(item: Student): Boolean = {
			val min = toInt(parameters("minWords")).get
			val max = toInt(parameters("maxWords")).get

			item.coursework.enhancedSubmission.flatMap(item => {
				val submission = item.submission
				val assignment = submission.assignment
				assignment.wordCountField
					.flatMap(field => submission.valuesByFieldName.get(field.name).flatMap(toInt))
					.map(words => words >= min && words <= max)
			}).getOrElse(false)
		}
		def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[minWords]".format(fieldName), "NotEmpty")
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[maxWords]".format(fieldName), "NotEmpty")

			if (!errors.hasErrors) {
				val min = toInt(parameters("minWords"))
				if (min.isEmpty) errors.rejectValue("%s[minWords]".format(fieldName), "typeMismatch")

				val max = toInt(parameters("maxWords"))
				if (max.isEmpty) errors.rejectValue("%s[maxWords]".format(fieldName), "typeMismatch")

				if (min.isDefined && max.isDefined) {
					if (max.get < min.get)
						errors.rejectValue("%s[maxWords]".format(fieldName), "filters.WithWordCount.max.lessThanMin")

					if (min.get < 0)
						errors.rejectValue("%s[minWords]".format(fieldName), "filters.WithWordCount.min.lessThanZero")

					if (max.get < 0)
						errors.rejectValue("%s[maxWords]".format(fieldName), "filters.WithWordCount.max.lessThanZero")
				}
			}
		}
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions && assignment.wordCountField.isDefined
	}

	case object Submitted extends ParameterlessCourseworkFilter {
		def getDescription = "students who have submitted an assignment"
		def predicate(item: Student): Boolean = {
			item.coursework.enhancedSubmission.isDefined
		}
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions
	}

	case object Unsubmitted extends ParameterlessCourseworkFilter {
		def getDescription = "students who have not submitted an assignment"
		def predicate(item: Student): Boolean = {
			item.coursework.enhancedSubmission.isEmpty
		}
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions
	}

	case object NotReleasedForMarking extends ParameterlessCourseworkFilter {
		def getDescription = "submissions that have not been released for marking"
		def predicate(student: Student): Boolean = !student.assignment.isReleasedForMarking(student.user.getUserId)
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions && assignment.markingWorkflow != null
	}

	case object NotMarked extends ParameterlessCourseworkFilter {
		def getDescription = "submissions not marked"
		def predicate(student: Student): Boolean = {
			val releasedForMarking = student.assignment.isReleasedForMarking(student.user.getUserId)
			val hasFirstMarker = student.assignment.getStudentsFirstMarker(student.user.getUserId).isDefined
			releasedForMarking && hasFirstMarker
		}

		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions && assignment.markingWorkflow != null
	}

	case object MarkedByFirst extends ParameterlessCourseworkFilter {
		def getDescription = "submissions marked by first marker"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedFeedback.exists(_.feedback.getFirstMarkerFeedback.exists(_.state == MarkingCompleted))
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions && assignment.markingWorkflow != null
	}

	case object MarkedBySecond extends ParameterlessCourseworkFilter {
		def getDescription = "submissions marked by second marker"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedFeedback.exists(_.feedback.getSecondMarkerFeedback.exists(_.state == MarkingCompleted))

		// Only applies to seen second marking
		def applies(assignment: Assignment): Boolean =
			assignment.collectSubmissions &&
			assignment.markingWorkflow != null &&
			assignment.markingWorkflow.hasSecondMarker
	}

	case object CheckedForPlagiarism extends ParameterlessCourseworkFilter {
		def getDescription = "submissions checked for plagiarism"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedSubmission.exists(_.submission.hasOriginalityReport.booleanValue)
		def applies(assignment: Assignment): Boolean =
			assignment.collectSubmissions && assignment.module.adminDepartment.plagiarismDetectionEnabled
	}

	case object NotCheckedForPlagiarism extends ParameterlessCourseworkFilter {
		def getDescription = "submissions not checked for plagiarism"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedSubmission.exists(!_.submission.hasOriginalityReport.booleanValue)
		def applies(assignment: Assignment): Boolean =
			assignment.collectSubmissions && assignment.module.adminDepartment.plagiarismDetectionEnabled
	}

	case object MarkedPlagiarised extends ParameterlessCourseworkFilter {
		def getDescription = "submissions marked as plagiarised"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedSubmission.exists(_.submission.suspectPlagiarised.booleanValue)
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions
	}

	case object WithOverlapPercentage extends CourseworkFilter {
		private def toInt(text: String) =
			if (text.hasText) try {	Some(text.toInt) } catch { case e: NumberFormatException => None }
			else None

		def getDescription = "submissions with a plagiarism overlap percentage between..."

		def parameters = Seq(
			("minOverlap", "Min overlap %", "percentage"),
			("maxOverlap", "Max overlap %", "percentage")
		)
		def predicate(parameters: Map[String, String])(item: Student): Boolean = {
			val min = toInt(parameters("minOverlap")).get
			val max = toInt(parameters("maxOverlap")).get

			item.coursework.enhancedSubmission.exists(item => {
				item.submission.allAttachments
					.flatMap(a=> Option(a.originalityReport))
					.flatMap(_.overlap)
					.map(overlap=> overlap >= min && overlap <= max)
					.exists(b => b)
			})
		}

		def validate(parameters: Map[String, String], fieldName: String = "filterParameters")(errors: Errors) {
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[minOverlap]".format(fieldName), "NotEmpty")
			ValidationUtils.rejectIfEmptyOrWhitespace(errors, "%s[maxOverlap]".format(fieldName), "NotEmpty")

			if (!errors.hasErrors) {
				val min = toInt(parameters("minOverlap"))
				if (min.isEmpty) errors.rejectValue("%s[minOverlap]".format(fieldName), "typeMismatch")

				val max = toInt(parameters("maxOverlap"))
				if (max.isEmpty) errors.rejectValue("%s[maxOverlap]".format(fieldName), "typeMismatch")

				if (min.isDefined && max.isDefined) {
					if (max.get < min.get)
						errors.rejectValue("%s[maxOverlap]".format(fieldName), "filters.WithOverlapPercentage.max.lessThanMin")

					if (min.get < 0 || min.get > 100)
						errors.rejectValue("%s[minOverlap]".format(fieldName), "filters.WithOverlapPercentage.min.notInRange")

					if (max.get < 0 || max.get > 100)
						errors.rejectValue("%s[maxOverlap]".format(fieldName), "filters.WithOverlapPercentage.max.notInRange")
				}
			}
		}
		def applies(assignment: Assignment): Boolean = assignment.collectSubmissions && assignment.module.adminDepartment.plagiarismDetectionEnabled
	}

	case object NoFeedback extends ParameterlessCourseworkFilter {
		def getDescription = "students with no feedback"
		def predicate(item: Student): Boolean = item.coursework.enhancedFeedback.forall(_.feedback.isPlaceholder)
		def applies(assignment: Assignment) = true
	}

	case object FeedbackNotReleased extends ParameterlessCourseworkFilter {
		def getDescription = "students with unpublished feedback"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder).exists(!_.feedback.released)
		def applies(assignment: Assignment) = true
	}

	case object FeedbackNotDownloaded extends ParameterlessCourseworkFilter {
		def getDescription = "students who haven't downloaded their feedback"
		def predicate(item: Student): Boolean =
			item.coursework.enhancedFeedback.filterNot(_.feedback.isPlaceholder).exists(!_.downloaded)
		def applies(assignment: Assignment) = true
	}


}