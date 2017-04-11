package uk.ac.warwick.tabula.data.model

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence._

import org.hibernate.annotations.{BatchSize, Filter, FilterDef, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model.forms._
import uk.ac.warwick.tabula.services.UserGroupCacheManager

import scala.collection.JavaConverters._
import scala.collection.mutable

object Exam {
	final val NotDeletedFilter = "notDeleted"
	final val defaultFeedbackTextFieldName = "feedbackText"
}

@FilterDef(name = Exam.NotDeletedFilter, defaultCondition = "deleted = 0")
@Filter(name = Exam.NotDeletedFilter)
@Entity
@Access(AccessType.FIELD)
class Exam
	extends Assessment
	with ToEntityReference
	with PostLoadBehaviour
	with Serializable {

	type Entity = Exam

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "module_id")
	override var module: Module = _

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	override var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(new DateTime())

	override var name: String = _

	@OneToMany(mappedBy = "exam", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	override var assessmentGroups: JList[AssessmentGroup] = JArrayList()

	@OneToMany(mappedBy = "exam", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var feedbacks: JList[ExamFeedback] = JArrayList()
	override def allFeedback: mutable.Buffer[ExamFeedback] = feedbacks.asScala

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "workflow_id")
	var markingWorkflow: MarkingWorkflow = _

	@OneToMany(mappedBy = "exam", fetch = LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var firstMarkers: JList[FirstMarkersMap] = JArrayList()

	@OneToMany(mappedBy = "exam", fetch = LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var secondMarkers: JList[SecondMarkersMap] = JArrayList()

	var released: JBoolean = _

	// sort order is unpredictable on retrieval from Hibernate; use indexed defs below for access
	@OneToMany(mappedBy = "exam", fetch = LAZY, cascade = Array(ALL))
	@BatchSize(size = 200)
	var fields: JList[ExamFormField] = JArrayList()

	def feedbackFields: Seq[ExamFormField] = fields.asScala.filter(_.context == FormFieldContext.Feedback).sortBy(_.position)

	def addField(field: ExamFormField) {
		if (field.context == null) throw new IllegalArgumentException("Field with name " + field.name + " has no context specified")
		if (fields.asScala.exists(_.name == field.name)) throw new IllegalArgumentException("Field with name " + field.name + " already exists")
		field.exam = this
		field.position = fields.asScala.count(_.context == field.context)
		fields.add(field)
	}

	override def addDefaultFeedbackFields() {
		val feedback = new ExamTextField
		feedback.name = Exam.defaultFeedbackTextFieldName
		feedback.value = ""
		feedback.context = FormFieldContext.Feedback

		addField(feedback)
	}

	override def addDefaultFields() {
		addDefaultFeedbackFields()
	}

	// Used for ad-hoc students
	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "membersgroup_id")
	private var _members: UserGroup = UserGroup.ofUsercodes

	def members: UnspecifiedTypeUserGroup = {
		Option(_members).map {
			new UserGroupCacheManager(_, assessmentMembershipService.examManualMembershipHelper)
		}.orNull
	}

	def members_=(group: UserGroup) {
		_members = group
	}

	// TAB-1446 If hibernate sets members to null, make a new empty usergroup
	override def postLoad() {
		ensureMembersGroup
	}

	def ensureMembersGroup: UserGroup = {
		if (_members == null) _members = UserGroup.ofUsercodes
		_members
	}

	def requiresMarks: Int = {
		membershipInfo.items.count(info => {
			val feedback = allFeedback.find(_.usercode == info.userId.getOrElse(""))
			feedback.isEmpty || feedback.get.latestMark.isEmpty
		})
	}

	@transient
	override val collectMarks: JBoolean = true

	override def permissionsParents: Stream[Module] = Option(module).toStream

	override def toEntityReference: ExamEntityReference = new ExamEntityReference().put(this)

}
