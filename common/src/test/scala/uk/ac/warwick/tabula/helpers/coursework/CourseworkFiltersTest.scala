package uk.ac.warwick.tabula.helpers.coursework

import org.joda.time.{DateTime, DateTimeConstants}
import org.mockito.Mockito._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.coursework.assignments.ListSubmissionsCommand._
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand._
import uk.ac.warwick.tabula.commands.coursework.feedback.ListFeedbackCommand._
import uk.ac.warwick.tabula.data.convert.JodaDateTimeConverter
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.{InvestigationCompleted, NotInvestigated, SuspectPlagiarised}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.{Extension, MarkerSelectField, SavedFormValue, WordCountField}
import uk.ac.warwick.tabula.services.SubmissionService
import uk.ac.warwick.tabula.{Fixtures, MockUserLookup, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

// scalastyle:off magic.number
class CourseworkFiltersTest extends TestBase with Mockito {

	val department: Department = Fixtures.department("in", "IT Services")
	val module: Module = Fixtures.module("in101", "Introduction to Web Development")
	val assignment: Assignment = Fixtures.assignment("Programming Test")
	assignment.module = module
	module.adminDepartment = department

	val mockUserLookup = new MockUserLookup
	mockUserLookup.registerUserObjects(
		new User("cuscav") { setWarwickId("0672089"); setFoundUser(true); setVerified(true); }
	)
	assignment.userLookup = mockUserLookup

	@Test def of() {
		CourseworkFilters.of("NotReleasedForMarking") match {
			case CourseworkFilters.NotReleasedForMarking =>
			case what:Any => fail("what is this?" + what)
		}
	}

	@Test(expected=classOf[IllegalArgumentException]) def invalidFilter() {
		CourseworkFilters.of("Spank")
	}

	@Test def name() {
		CourseworkFilters.AllStudents.getName should be ("AllStudents")
		CourseworkFilters.NotReleasedForMarking.getName should be ("NotReleasedForMarking")
		CourseworkFilters.of("NotReleasedForMarking").getName should be ("NotReleasedForMarking")
	}

	@Test def AllStudents() {
		val filter = CourseworkFilters.AllStudents

		// Should pass anything and any assignment, so just check with null
		filter.applies(null) should be {true}
		filter.predicate(null) should be {true}
	}

	private def workflowItems(
			submission: Option[Submission]=None,
			submissionDownloaded: Boolean=false,
			feedback: Option[Feedback]=None,
			feedbackDownloaded: Boolean=false,
			onlineFeedbackViewed: Boolean=false,
			extension: Option[Extension]=None,
			withinExtension: Boolean=false,
			student:User=null) =
		WorkflowItems(
			student=student,
			enhancedSubmission=submission map { s => SubmissionListItem(s, submissionDownloaded) },
			enhancedFeedback=feedback map { f => FeedbackListItem(f, feedbackDownloaded, onlineFeedbackViewed, new FeedbackForSits) },
			enhancedExtension=extension map { e => ExtensionListItem(e, withinExtension) }
		)

	private def student(
			submission: Option[Submission]=None,
			submissionDownloaded: Boolean=false,
			feedback: Option[Feedback]=None,
			feedbackDownloaded: Boolean=false,
			onlineFeedbackViewed: Boolean=false,
			extension: Option[Extension]=None,
			withinExtension: Boolean=false,
			assignment:Assignment=null,
			user:User=Fixtures.user()) =
		Student(
			user=user,
			progress=null,
			nextStage=None,
			stages=ListMap.empty,
			coursework=workflowItems(submission, submissionDownloaded, feedback, feedbackDownloaded, onlineFeedbackViewed, extension, withinExtension),
			assignment=assignment,
			disability = None
		)

	class SampleFilteringCommand(elems: (String, String)*) {
		var filter: JMap[String, String] = JHashMap()
		elems foreach { case (k, v) => filter.put(k, v) }
	}

	@Test def SubmittedBetweenDates() {
		val filter = CourseworkFilters.SubmittedBetweenDates

		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		val converter = new JodaDateTimeConverter

		// Validation - start and end date must be defined and end must be after start
		{
			val cmd = new SampleFilteringCommand()
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (2)
			errors.getFieldErrors.asScala.head.getField should be ("filter[startDate]")
			errors.getFieldErrors.asScala.head.getCodes should contain ("NotEmpty")
			errors.getFieldErrors.asScala(1).getField should be ("filter[endDate]")
			errors.getFieldErrors.asScala(1).getCodes should contain ("NotEmpty")
		}

		{
			val startDate = new DateTime(2013, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)
			val endDate = new DateTime(2013, DateTimeConstants.APRIL, 1, 0, 0, 0, 0)

			val cmd = new SampleFilteringCommand(
				"startDate" -> converter.convertLeft(endDate),
				"endDate" -> converter.convertLeft(startDate)
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (1)
			errors.getFieldErrors.asScala.head.getField should be ("filter[endDate]")
			errors.getFieldErrors.asScala.head.getCode should be ("filters.SubmittedBetweenDates.end.beforeStart")
		}

		{
			val startDate = new DateTime(2013, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)
			val endDate = new DateTime(2013, DateTimeConstants.APRIL, 1, 0, 0, 0, 0)

			val cmd = new SampleFilteringCommand(
				"startDate" -> converter.convertLeft(startDate),
				"endDate" -> converter.convertLeft(endDate)
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.hasErrors should be {false}
		}

		// Valid where there is a submission, and the submission is in March
		val startDate = new DateTime(2013, DateTimeConstants.MARCH, 1, 0, 0, 0, 0)
		val endDate = new DateTime(2013, DateTimeConstants.APRIL, 1, 0, 0, 0, 0)

		val params = Map(
			"startDate" -> converter.convertLeft(startDate),
			"endDate" -> converter.convertLeft(endDate)
		)

		filter.predicate(params)(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		// Submission in February
		submission.submittedDate = startDate.minusDays(1)
		filter.predicate(params)(student(submission=Option(submission))) should be {false}

		// Submission in April
		submission.submittedDate = endDate.plusDays(1)
		filter.predicate(params)(student(submission=Option(submission))) should be {false}

		// Submission exactly on start date
		submission.submittedDate = startDate
		filter.predicate(params)(student(submission=Option(submission))) should be {true}

		// Submission exactly on end date
		submission.submittedDate = endDate
		filter.predicate(params)(student(submission=Option(submission))) should be {true}

		// Submission inbetween
		submission.submittedDate = startDate.plusDays(1)
		filter.predicate(params)(student(submission=Option(submission))) should be {true}
	}

	@Test def OnTime() {
		val filter = CourseworkFilters.OnTime

		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid where there is a submission, that submission is not late, and that submission is not authorised late
		filter.predicate(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		submission.isLate should be {false}
		submission.isAuthorisedLate should be {false}

		filter.predicate(student(submission=Option(submission))) should be {true}

		// Where submission is late, they don't fit
		assignment.closeDate = DateTime.now.minusDays(1)
		submission.submittedDate = DateTime.now

		submission.isLate should be {true}
		submission.isAuthorisedLate should be {false}
		filter.predicate(student(submission=Option(submission))) should be {false}

		// Authorised late isn't allowed here either
		// TODO is this right?

		val extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = DateTime.now.plusDays(1)
		extension.assignment = assignment
		assignment.extensions.add(extension)

		submission.isLate should be {false}
		submission.isAuthorisedLate should be {true}
		filter.predicate(student(submission=Option(submission), extension=Option(extension), withinExtension=true)) should be {false}
	}

	@Test def Late() {
		val filter = CourseworkFilters.Late

		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid where there is a submission, that submission is late, and that submission is not authorised late
		filter.predicate(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		submission.isLate should be {false}
		submission.isAuthorisedLate should be {false}

		filter.predicate(student(submission=Option(submission))) should be {false}

		// Where submission is late, they don't fit
		assignment.closeDate = DateTime.now.minusDays(1)
		submission.submittedDate = DateTime.now

		submission.isLate should be {true}
		submission.isAuthorisedLate should be {false}
		filter.predicate(student(submission=Option(submission))) should be {true}

		// Authorised late isn't allowed here

		val extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = DateTime.now.plusDays(1)
		extension.assignment = assignment
		assignment.extensions.add(extension)

		submission.isLate should be {false}
		submission.isAuthorisedLate should be {true}
		filter.predicate(student(submission=Option(submission), extension=Option(extension), withinExtension=true)) should be {false}
	}

	@Test def WithExtension() {
		val filter = CourseworkFilters.WithExtension

		// Only applies to assignments that collect submissions and accept extensions
		assignment.allowExtensions = false

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		assignment.allowExtensions = true

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid when there is an extension
		filter.predicate(student(extension=None)) should be {false}
		filter.predicate(student(extension=Option(Fixtures.extension()))) should be {true}
	}

	@Test def WithinExtension() {
		val filter = CourseworkFilters.WithinExtension

		// Only applies to assignments that collect submissions and accept extensions
		assignment.allowExtensions = false

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		assignment.allowExtensions = true

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid only when the submission is authorised late (NOT when we are just within extension - else we wouldn't have submitted)
		filter.predicate(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		submission.isLate should be {false}
		submission.isAuthorisedLate should be {false}

		filter.predicate(student(submission=Option(submission))) should be {false}

		// Where submission is late, they don't fit
		assignment.closeDate = DateTime.now.minusDays(1)
		submission.submittedDate = DateTime.now

		submission.isLate should be {true}
		submission.isAuthorisedLate should be {false}
		filter.predicate(student(submission=Option(submission))) should be {false}

		// Authorised late fits

		val extension = Fixtures.extension("0672089", "cuscav")
		extension.approve()
		extension.expiryDate = DateTime.now.plusDays(1)
		extension.assignment = assignment
		assignment.extensions.add(extension)

		submission.isLate should be {false}
		submission.isAuthorisedLate should be {true}
		filter.predicate(student(submission=Option(submission), extension=Option(extension), withinExtension=true)) should be {true}
	}

	@Test def WithWordCount() {
		val filter = CourseworkFilters.WithWordCount

		// Only applies to assignments that collect submissions and have a word count field defined
		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		val wordCountField = new WordCountField
		wordCountField.name = Assignment.defaultWordCountName
		assignment.addField(wordCountField)

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Validation - min and max must be defined, in range, and max must be >= min
		{
			val cmd = new SampleFilteringCommand()
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (2)
			errors.getFieldErrors.asScala.head.getField should be ("filter[minWords]")
			errors.getFieldErrors.asScala.head.getCodes should contain ("NotEmpty")
			errors.getFieldErrors.asScala(1).getField should be ("filter[maxWords]")
			errors.getFieldErrors.asScala(1).getCodes should contain ("NotEmpty")
		}

		{
			val cmd = new SampleFilteringCommand(
				"minWords" -> 50.toString,
				"maxWords" -> "steve"
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (1)
			errors.getFieldErrors.asScala.head.getField should be ("filter[maxWords]")
			errors.getFieldErrors.asScala.head.getCodes should contain ("typeMismatch")
		}

		{
			val cmd = new SampleFilteringCommand(
				"minWords" -> (-15).toString,
				"maxWords" -> (-200).toString
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (3)
			errors.getFieldErrors.asScala.head.getField should be ("filter[maxWords]")
			errors.getFieldErrors.asScala.head.getCodes should contain ("filters.WithWordCount.max.lessThanMin")
			errors.getFieldErrors.asScala(1).getField should be ("filter[minWords]")
			errors.getFieldErrors.asScala(1).getCodes should contain ("filters.WithWordCount.min.lessThanZero")
			errors.getFieldErrors.asScala(2).getField should be ("filter[maxWords]")
			errors.getFieldErrors.asScala(2).getCodes should contain ("filters.WithWordCount.max.lessThanZero")
		}

		{
			val cmd = new SampleFilteringCommand(
				"minWords" -> "0",
				"maxWords" -> "100"
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.hasErrors should be {false}
		}

		// Valid where there is a submission, and the word count is between 40 and 60
		val params = Map(
			"minWords" -> "40",
			"maxWords" -> "60"
		)

		filter.predicate(params)(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		filter.predicate(params)(student(submission=Option(submission))) should be {false}

		val v = new SavedFormValue
		v.name = wordCountField.name
		v.value = "30"
		submission.values.add(v)

		filter.predicate(params)(student(submission=Option(submission))) should be {false}

		v.value = "40"
		filter.predicate(params)(student(submission=Option(submission))) should be {true}

		v.value = "50"
		filter.predicate(params)(student(submission=Option(submission))) should be {true}

		v.value = "60"
		filter.predicate(params)(student(submission=Option(submission))) should be {true}
	}

	@Test def Submitted() {
		val filter = CourseworkFilters.Submitted

		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid when there is a submission
		filter.predicate(student(submission=None)) should be {false}
		filter.predicate(student(submission=Option(Fixtures.submission()))) should be {true}
	}

	@Test def Unsubmitted() {
		val filter = CourseworkFilters.Unsubmitted

		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid when there is no submission
		filter.predicate(student(submission=None)) should be {true}
		filter.predicate(student(submission=Option(Fixtures.submission()))) should be {false}
	}

	@Test def NotReleasedForMarking() {
		val filter = CourseworkFilters.NotReleasedForMarking

		// Only applies to assignments that collect submissions and have a marking workflow
		assignment.markingWorkflow = null

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		assignment.markingWorkflow = Fixtures.seenSecondMarkingLegacyWorkflow("my marking workflow")

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		val s = Fixtures.user("0672089", "cuscav")
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		assignment.isReleasedForMarking(submission.usercode) should be {false}

		filter.predicate(student(user=s, submission=Option(submission), assignment=assignment)) should be {true}

		// Release for marking, no longer fits
		val feedback = Fixtures.assignmentFeedback("0672089", "cuscav")
		assignment.feedbacks.add(feedback)
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		assignment.isReleasedForMarking(submission.usercode) should be {true}

		filter.predicate(student(user=s, submission=Option(submission), assignment=assignment)) should be {false}
	}

	@Test def NotMarked() {
		val filter = CourseworkFilters.NotMarked

		// Only applies to assignments that collect submissions and have a marking workflow
		assignment.markingWorkflow = null

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		val workflow = Fixtures.studentsChooseMarkerWorkflow("my marking workflow")
		workflow.submissionService = mock[SubmissionService]
		assignment.markingWorkflow = workflow

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid only where a submission exists, has been released for marking, and has a first marker
		filter.predicate(student(submission=None, assignment=assignment)) should be {false}

		val s = Fixtures.user("0672089", "cuscav")
		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment
		when(workflow.submissionService.getSubmissionByUsercode(assignment, "cuscav")) thenReturn Option(submission)

		assignment.isReleasedForMarking(submission.usercode) should be {false}

		filter.predicate(student(user=s, submission=Option(submission), assignment=assignment)) should be {false}

		// Release for marking and make sure it has a first marker
		val feedback = Fixtures.assignmentFeedback("0672089", "cuscav")
		assignment.feedbacks.add(feedback)
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		assignment.isReleasedForMarking(submission.usercode) should be {true}

		val f = new MarkerSelectField
		f.name = Assignment.defaultMarkerSelectorName
		assignment.addField(f)

		val v = new SavedFormValue
		v.name = Assignment.defaultMarkerSelectorName
		v.value = "cusmab"
		submission.values.add(v)

		filter.predicate(student(user=s, submission=Option(submission), assignment=assignment)) should be {true}
	}

	@Test def MarkedByFirst() {
		val filter = CourseworkFilters.MarkedByFirst

		// Only applies to assignments that collect submissions and have a marking workflow
		assignment.markingWorkflow = null

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		assignment.markingWorkflow = Fixtures.seenSecondMarkingLegacyWorkflow("my marking workflow")

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid only if released to second marker OR marking is completed
		filter.predicate(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		val feedback = Fixtures.assignmentFeedback("0672089", "cuscav")
		assignment.feedbacks.add(feedback)
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		assignment.isReleasedToSecondMarker(submission.usercode) should be {false}

		filter.predicate(student(feedback=Option(feedback))) should be {false}

		feedback.firstMarkerFeedback.state = MarkingState.MarkingCompleted
		filter.predicate(student(feedback=Option(feedback))) should be {true}

		feedback.secondMarkerFeedback = Fixtures.markerFeedback(feedback)
		assignment.isReleasedToSecondMarker(submission.usercode) should be {true}

		filter.predicate(student(feedback=Option(feedback))) should be {true}
	}

	@Test def MarkedBySecond() {
		val filter = CourseworkFilters.MarkedBySecond

		// Only applies to assignments that collect submissions and have a marking workflow, and only if it's seen second marking
		assignment.markingWorkflow = null

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		assignment.markingWorkflow = Fixtures.seenSecondMarkingLegacyWorkflow("my marking workflow")

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid only if marking is completed
		filter.predicate(student(feedback=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		val feedback = Fixtures.assignmentFeedback("0672089", "cuscav")
		assignment.feedbacks.add(feedback)
		feedback.firstMarkerFeedback = Fixtures.markerFeedback(feedback)
		feedback.secondMarkerFeedback = Fixtures.markerFeedback(feedback)

		filter.predicate(student(feedback=Option(feedback))) should be {false}

		feedback.secondMarkerFeedback.state = MarkingState.MarkingCompleted

		filter.predicate(student(feedback=Option(feedback))) should be {true}
	}

	@Test def CheckedForPlagiarism() {
		val filter = CourseworkFilters.CheckedForPlagiarism

		// Only applies to assignments that collect submissions and the department has plagiarism detection enabled
		department.plagiarismDetectionEnabled = false

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		department.plagiarismDetectionEnabled = true

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid when the submission exists and it has at least one attachment with an originality report
		filter.predicate(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		submission.hasOriginalityReport.booleanValue() should be {false}

		filter.predicate(student(submission=Option(submission))) should be {false}

		val a = new FileAttachment
		val originalityReport =new OriginalityReport
		originalityReport.reportReceived = true
		a.originalityReport = originalityReport
		submission.values.add(SavedFormValue.withAttachments(submission, "Turnitin", Seq(a).toSet))

		submission.hasOriginalityReport.booleanValue() should be {true}

		filter.predicate(student(submission=Option(submission))) should be {true}
	}

	@Test def WithOverlapPercentage() {
		val filter = CourseworkFilters.WithOverlapPercentage

		// Only applies to assignments that collect submissions and the department has plagiarism detection enabled
		department.plagiarismDetectionEnabled = false

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		department.plagiarismDetectionEnabled = true

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Validation - min and max must be defined, in range, and max must be >= min
		{
			val cmd = new SampleFilteringCommand()
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (2)
			errors.getFieldErrors.asScala.head.getField should be ("filter[minOverlap]")
			errors.getFieldErrors.asScala.head.getCodes should contain ("NotEmpty")
			errors.getFieldErrors.asScala(1).getField should be ("filter[maxOverlap]")
			errors.getFieldErrors.asScala(1).getCodes should contain ("NotEmpty")
		}

		{
			val cmd = new SampleFilteringCommand(
				"minOverlap" -> 50.toString,
				"maxOverlap" -> "steve"
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (1)
			errors.getFieldErrors.asScala.head.getField should be ("filter[maxOverlap]")
			errors.getFieldErrors.asScala.head.getCodes should contain ("typeMismatch")
		}

		{
			val cmd = new SampleFilteringCommand(
				"minOverlap" -> 1500.toString,
				"maxOverlap" -> 200.toString
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.getErrorCount should be (3)
			errors.getFieldErrors.asScala.head.getField should be ("filter[maxOverlap]")
			errors.getFieldErrors.asScala.head.getCodes should contain ("filters.WithOverlapPercentage.max.lessThanMin")
			errors.getFieldErrors.asScala(1).getField should be ("filter[minOverlap]")
			errors.getFieldErrors.asScala(1).getCodes should contain ("filters.WithOverlapPercentage.min.notInRange")
			errors.getFieldErrors.asScala(2).getField should be ("filter[maxOverlap]")
			errors.getFieldErrors.asScala(2).getCodes should contain ("filters.WithOverlapPercentage.max.notInRange")
		}

		{
			val cmd = new SampleFilteringCommand(
				"minOverlap" -> "0",
				"maxOverlap" -> "100"
			)
			val errors = new BindException(cmd, "cmd")
			filter.validate(cmd.filter.asScala.toMap, "filter")(errors)

			errors.hasErrors should be {false}
		}

		// Valid where there is a submission, and the overlap percentage is between 40 and 60
		val params = Map(
			"minOverlap" -> "40",
			"maxOverlap" -> "60"
		)

		filter.predicate(params)(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		submission.hasOriginalityReport.booleanValue() should be {false}

		filter.predicate(params)(student(submission=Option(submission))) should be {false}

		val a = new FileAttachment

		a.originalityReport = new OriginalityReport
		a.originalityReport.reportReceived = true
		a.originalityReport.overlap = Option(30)
		submission.values.add(SavedFormValue.withAttachments(submission, "Turnitin", Seq(a).toSet))

		submission.hasOriginalityReport.booleanValue() should be {true}

		filter.predicate(params)(student(submission=Option(submission))) should be {false}

		a.originalityReport.overlap = Option(40)
		filter.predicate(params)(student(submission=Option(submission))) should be {true}

		a.originalityReport.overlap = Option(50)
		filter.predicate(params)(student(submission=Option(submission))) should be {true}

		a.originalityReport.overlap = Option(60)
		filter.predicate(params)(student(submission=Option(submission))) should be {true}
	}

	@Test def NotCheckedForPlagiarism() {
		val filter = CourseworkFilters.NotCheckedForPlagiarism

		// Only applies to assignments that collect submissions and the department has plagiarism detection enabled
		department.plagiarismDetectionEnabled = false

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {false}

		department.plagiarismDetectionEnabled = true

		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid when the submission exists and it doesn't have an originality report
		filter.predicate(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		submission.hasOriginalityReport.booleanValue() should be {false}

		filter.predicate(student(submission=Option(submission))) should be {true}

		// Checked for plagiarism, no longer fits
		val a = new FileAttachment
		val originalityReport =new OriginalityReport
		originalityReport.reportReceived = true
		a.originalityReport = originalityReport

		submission.values.add(SavedFormValue.withAttachments(submission, "Turnitin", Seq(a).toSet))

		submission.hasOriginalityReport.booleanValue() should be {true}

		filter.predicate(student(submission=Option(submission))) should be {false}
	}

	@Test def markedPlagiarised() {
		val filter = CourseworkFilters.MarkedPlagiarised

		// Only applies to assignments that collect submissions
		assignment.collectSubmissions = false
		filter.applies(assignment) should be {false}

		assignment.collectSubmissions = true
		filter.applies(assignment) should be {true}

		// Valid when the submission exists and it has been marked as plagiarised
		filter.predicate(student(submission=None)) should be {false}

		val submission = Fixtures.submission("0672089", "cuscav")
		submission.assignment = assignment

		submission.plagiarismInvestigation = NotInvestigated

		filter.predicate(student(submission=Option(submission))) should be {false}

		submission.plagiarismInvestigation = SuspectPlagiarised

		filter.predicate(student(submission=Option(submission))) should be {true}

		submission.plagiarismInvestigation = InvestigationCompleted

		filter.predicate(student(submission=Option(submission))) should be {false}
	}

	@Test def NoFeedback() {
		val filter = CourseworkFilters.NoFeedback

		// Should pass any assignment, so just check with null
		filter.applies(null) should be {true}

		// Valid where there's no feedback
		filter.predicate(student(feedback=None)) should be {true}

		val feedback = new AssignmentFeedback
		feedback.actualMark = Option(41)
		feedback.assignment = assignment
		filter.predicate(student(feedback=Option(feedback))) should be {false}
	}

	@Test def FeedbackNotReleased() {
		val filter = CourseworkFilters.FeedbackNotReleased

		// Should pass any assignment, so just check with null
		filter.applies(null) should be {true}

		// Valid where there's feedback, but it hasn't been released
		filter.predicate(student(feedback=None)) should be {false}

		val feedback = Fixtures.assignmentFeedback("0672089", "cuscav")
		feedback.actualMark = Option(41)
		feedback.released = false
		feedback.assignment = assignment

		filter.predicate(student(feedback=Option(feedback))) should be {true}

		feedback.released = true

		filter.predicate(student(feedback=Option(feedback))) should be {false}
	}

	@Test def FeedbackNotDownloaded() {
		val filter = CourseworkFilters.FeedbackNotDownloaded

		// Should pass any assignment, so just check with null
		filter.applies(null) should be {true}

		val testFeedback = Fixtures.assignmentFeedback()
		testFeedback.actualMark = Option(41)
		testFeedback.assignment = assignment

		// Valid where there's feedback, but it hasn't been downloaded
		filter.predicate(student(feedback=None)) should be {false}
		filter.predicate(student(feedback=Option(testFeedback), feedbackDownloaded=false)) should be {true}
		filter.predicate(student(feedback=Option(testFeedback), feedbackDownloaded=true)) should be {false}
	}


}