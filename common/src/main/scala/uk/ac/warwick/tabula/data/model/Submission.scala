package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence._
import javax.validation.constraints.NotNull

import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.PlagiarismInvestigation.{InvestigationCompleted, SuspectPlagiarised}
import uk.ac.warwick.tabula.data.model.forms.{FormField, SavedFormValue}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConversions._
import scala.collection.mutable

object Submission {
	val UseDisabilityFieldName = "use-disability"
}

@Entity @Access(AccessType.FIELD)
class Submission extends GeneratedId with PermissionsTarget with ToEntityReference with FeedbackReportGenerator {

	type Entity = Submission

	def isLate: Boolean = submittedDate != null && assignment.isLate(this)
	def isAuthorisedLate: Boolean = submittedDate != null && assignment.isAuthorisedLate(this)
	def workingDaysLate: Int = assignment.workingDaysLate(this)
	def deadline: DateTime = assignment.submissionDeadline(this)

	def feedbackDeadline: Option[LocalDate] = assignment.feedbackDeadlineForSubmission(this)

	@ManyToOne(optional = false, cascade = Array(PERSIST, MERGE), fetch = LAZY)
	@JoinColumn(name = "assignment_id")
	var assignment: Assignment = _

	def permissionsParents: Stream[Assignment] = Option(assignment).toStream

	var submitted: Boolean = false

	@Column(name = "submitted_date")
	var submittedDate: DateTime = _

	@NotNull
	@Column(name = "userId")
	var usercode: String = _

	@Type(`type` = "uk.ac.warwick.tabula.data.model.PlagiarismInvestigationUserType")
	var plagiarismInvestigation: PlagiarismInvestigation = PlagiarismInvestigation.Default

	def suspectPlagiarised: Boolean = plagiarismInvestigation == SuspectPlagiarised
	def investigationCompleted: Boolean = plagiarismInvestigation == InvestigationCompleted

	/**
	 * It isn't essential to record University ID as their user ID
	 * will identify them, but a lot of processes require the university
	 * number as the way of identifying a person and it'd be expensive
	 * to go and fetch the value every time. Also if we're keeping
	 * records for a while, the ITS account can be expunged so we'd lose
	 * it entirely.
	 */
	@Column(name = "universityId")
	var _universityId: String = _

	def universityId = Option(_universityId)

	def studentIdentifier = universityId.getOrElse(usercode)


	@OneToMany(mappedBy = "submission", cascade = Array(ALL))
	@BatchSize(size=200)
	var values: JSet[SavedFormValue] = new java.util.HashSet

	def getValue(field: FormField): Option[SavedFormValue] = {
		values.find( _.name == field.name )
	}

	def isForUser(user: User): Boolean = usercode == user.getUserId

	def firstMarker:Option[User] = assignment.getStudentsFirstMarker(usercode)

	def secondMarker:Option[User] = assignment.getStudentsSecondMarker(usercode)

	def valuesByFieldName: Map[String, String] = (values map { v => (v.name, v.value) }).toMap

	def valuesWithAttachments: mutable.Set[SavedFormValue] = values.filter(_.hasAttachments)

	def allAttachments: Seq[FileAttachment] = valuesWithAttachments.toSeq flatMap { _.attachments }

	def hasOriginalityReport: JBoolean = allAttachments.exists( _.originalityReportReceived )

	def isNoteworthy: Boolean = suspectPlagiarised || isAuthorisedLate || isLate

	/** Filename as we would expect to find this attachment in a downloaded zip of submissions. */
	def zipFileName(attachment: FileAttachment): String = {
		assignment.module.code + " - " + studentIdentifier + " - " + attachment.name
	}

	def zipFilename(attachment: FileAttachment, name: String): String = {
		assignment.module.code + " - " + name + " - " + attachment.name
	}

	def useDisability: Boolean = values.find(_.name == Submission.UseDisabilityFieldName).exists(_.value.toBoolean)

	def toEntityReference: SubmissionEntityReference = new SubmissionEntityReference().put(this)
}

trait FeedbackReportGenerator {
	def usercode: String
	def feedbackDeadline: Option[LocalDate]
}