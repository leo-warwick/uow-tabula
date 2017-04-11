package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.tabula.system.TwoWayConverter

import scala.collection.JavaConversions._
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{AssessmentServiceUserGroupHelpers, AssessmentService, UserGroupCacheManager, UserLookupService}
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.helpers.StringUtils._

/** A MarkingWorkflow defines how an assessment will be marked, including who
  * will be the markers and what rules should be used to decide how submissions
  * are distributed.
  *
  * A MarkingWorkflow is created against a Department and can then be reused by
  * many Assessments within that Department.
  */
@Entity
@Table(name="MarkScheme")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="MarkingMethod", discriminatorType = DiscriminatorType.STRING, length=255)
@Access(AccessType.FIELD)
@Deprecated
abstract class MarkingWorkflow extends GeneratedId with PermissionsTarget with Serializable {

	type Usercode = String
	type UniversityId = String

	@transient
	var userLookup: UserLookupService = Wire[UserLookupService]("userLookup")

	/** A descriptive name for the users' reference. */
	@Basic(optional = false)
	var name: String = null

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var department: Department = null

	// Not all marking workflows are suitable for exams
	def validForExams: Boolean = false

	def permissionsParents: Stream[Department] = Option(department).toStream

	def courseworkMarkingUrl(assignment: Assignment, marker: User, studentId: String): String =
		Routes.coursework.admin.assignment.markerFeedback.onlineFeedback(assignment, marker)

	def examMarkingUrl(exam: Exam, marker: User, studentId: String): String =
		Routes.exams.Exams.admin.markerFeedback.onlineFeedback(exam, marker)

	// FIXME this isn't really optional, but testing is a pain unless it's made so
	@transient var assessmentService: Option[AssessmentService with AssessmentServiceUserGroupHelpers] = Wire.option[AssessmentService with AssessmentServiceUserGroupHelpers]

	/** The group of first markers. */
	@OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "firstmarkers_id")
	private var _firstMarkers = UserGroup.ofUsercodes

	/** The second group of markers. May be unused if the marking workflow
	  * only has one marking stage.
	  */
	@OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "secondmarkers_id")
	private var _secondMarkers = UserGroup.ofUsercodes

	def thirdMarkers: UnspecifiedTypeUserGroup

	def firstMarkers: UnspecifiedTypeUserGroup = {
		assessmentService match {
			case Some(service) =>
				new UserGroupCacheManager(_firstMarkers, service.firstMarkerHelper)
			case _ => _firstMarkers
		}
	}
	def firstMarkers_=(group: UserGroup) { _firstMarkers = group }

	def secondMarkers: UnspecifiedTypeUserGroup = {
		assessmentService match {
			case Some(service) =>
				new UserGroupCacheManager(_secondMarkers, service.secondMarkerHelper)
			case _ => _secondMarkers
		}
	}
	def secondMarkers_=(group: UserGroup) { _secondMarkers = group }

	def markingMethod: MarkingMethod

	/** If true, the submitter chooses their first marker from a dropdown */
	def studentsChooseMarker = false

	def firstMarkerRoleName: String = "Marker"
	def firstMarkerVerb: String = "mark"

	// True if this marking workflow uses a second marker
	def hasSecondMarker: Boolean
	def secondMarkerRoleName: Option[String]
	def secondMarkerVerb: Option[String]

	def hasThirdMarker: Boolean
	def thirdMarkerRoleName: Option[String]
	def thirdMarkerVerb: Option[String]

	def studentHasMarker(assessment:Assessment, usercode: Usercode): Boolean =
		getStudentsFirstMarker(assessment, usercode).isDefined ||
		getStudentsSecondMarker(assessment, usercode).isDefined ||
		getStudentsThirdMarker(assessment, usercode).isDefined

	def getStudentsFirstMarker(assessment:Assessment, usercode: Usercode): Option[Usercode]

	def getStudentsSecondMarker(assessment:Assessment, usercode: Usercode): Option[Usercode]

	def getStudentsThirdMarker(assessment:Assessment, usercode: Usercode): Option[Usercode]

	// Get's the marker that is primarally responsible for the specified students feedback. This user is notified of
	// adjustments. The default is the first marker but this can be overriden when that isn't the case
	def getStudentsPrimaryMarker(assessment:Assessment, usercode: Usercode): Option[Usercode] =
		getStudentsFirstMarker(assessment, usercode)

	// get's the submissions for the given marker this must be an assignment as exams have no submission
	def getSubmissions(assignment: Assignment, user: User): Seq[Submission]

	def getMarkersStudents(assessment:Assessment, user: User): Seq[User]

	// get's this workflows role name for the specified position in the workflow
	def getRoleNameForPosition(position: FeedbackPosition): String = {
		position match {
			case FirstFeedback => firstMarkerRoleName
			case SecondFeedback => secondMarkerRoleName.getOrElse(MarkingWorkflow.adminRole)
			case ThirdFeedback => thirdMarkerRoleName.getOrElse(MarkingWorkflow.adminRole)
		}
	}

	// get's this workflows role name for the specified position in the workflow
	def getRoleNameForNextPosition(position: FeedbackPosition): String = {
		(position match {
			case FirstFeedback => secondMarkerRoleName
			case SecondFeedback => thirdMarkerRoleName
			case ThirdFeedback => None
		}).getOrElse(MarkingWorkflow.adminRole)
	}

	// get's this workflows role name for the specified position in the workflow
	def getRoleNameForPreviousPosition(position: FeedbackPosition): Option[String] = {
		position match {
			case FirstFeedback => None
			case SecondFeedback => Some(firstMarkerRoleName)
			case ThirdFeedback => secondMarkerRoleName
		}
	}

	// get's the next marker in the workflow if one exists
	def getNextMarker(position: Option[FeedbackPosition], assessment:Assessment, usercode: Usercode): Option[User] = {
		val markerId = position match {
			case Some(FirstFeedback) => getStudentsSecondMarker(assessment, usercode)
			case Some(SecondFeedback) => getStudentsThirdMarker(assessment, usercode)
			case _ => None
		}
		markerId.map(userLookup.getUserByUserId)
	}

	override def toString: UniversityId = "MarkingWorkflow(" + id + ")"

}

case class MarkerAndRole(role: String, marker: Option[User])

object MarkingWorkflow {

	val adminRole = "Administrator"

	def getMarkerFromAssessmentMap(userLookup: UserLookupService, usercode: String, markerMap: Map[String, UserGroup]): Option[String] = {
		val student = userLookup.getUserByUserId(usercode)
		val studentsGroup = markerMap.find{case(markerUserId, group) => group.includesUser(student)}
		studentsGroup.map{ case (markerUserId, _) => markerUserId }
	}

	implicit val defaultOrdering: Ordering[MarkingWorkflow] = Ordering.by { workflow: MarkingWorkflow => workflow.name.toLowerCase }

}

trait AssessmentMarkerMap {

	this : MarkingWorkflow =>

	def getStudentsFirstMarker(assessment: Assessment, usercode: Usercode): Option[Usercode] =
		MarkingWorkflow.getMarkerFromAssessmentMap(userLookup, usercode, assessment.firstMarkerMap)

	def getStudentsSecondMarker(assessment: Assessment, usercode: Usercode): Option[Usercode] =
		MarkingWorkflow.getMarkerFromAssessmentMap(userLookup, usercode, assessment.secondMarkerMap)

	// for assignemnts only. cannot get submissions for exams as they don't exist
	def getSubmissions(assignment: Assignment, marker: User): Seq[Submission] = {

		def getSubmissionsFromMap(assignment: Assignment, marker: User): Seq[Submission] = {
			val studentIds =
				assignment.firstMarkerMap.get(marker.getUserId).map{_.knownType.allIncludedIds}.getOrElse(Seq()) ++
					assignment.secondMarkerMap.get(marker.getUserId).map{_.knownType.allIncludedIds}.getOrElse(Seq())

			assignment.submissions.filter(s => studentIds.contains(s.usercode))
		}

		val allSubmissionsForMarker = getSubmissionsFromMap(assignment, marker)

		allSubmissionsForMarker.filter(submission => {

			val id = submission.usercode
			(
				assignment.markingWorkflow.hasThirdMarker &&
					assignment.isReleasedToThirdMarker(id) &&
					getStudentsThirdMarker(assignment, id).contains(marker.getUserId)
				) || (
				assignment.markingWorkflow.hasSecondMarker &&
					assignment.isReleasedToSecondMarker(id) &&
					getStudentsSecondMarker(assignment, id).contains(marker.getUserId)
				) || (
				assignment.isReleasedForMarking(id) &&
					getStudentsFirstMarker(assignment, id).contains(marker.getUserId)
				)
		})
	}

	def getMarkersStudents(assessment: Assessment, marker: User): Seq[User] = {
		assessment.firstMarkerMap.get(marker.getUserId).map{_.knownType.users}.getOrElse(Seq()) ++
			assessment.secondMarkerMap.get(marker.getUserId).map{_.knownType.users}.getOrElse(Seq())
	}

}

trait NoThirdMarker {

	this : MarkingWorkflow =>

	def hasThirdMarker = false
	def thirdMarkerRoleName = None
	def thirdMarkerVerb = None
	def thirdMarkers: UserGroup = UserGroup.ofUsercodes
	def getStudentsThirdMarker(assessment:Assessment, universityId: UniversityId): Option[String] = None
}

trait NoSecondMarker extends NoThirdMarker {

	this : MarkingWorkflow =>

	def hasSecondMarker = false
	def secondMarkerRoleName = None
	def secondMarkerVerb = None
	def getStudentsSecondMarker(assessment:Assessment, universityId: UniversityId): Option[String] = None
}


/**
 * Available marking methods and code to persist them
 */
sealed abstract class MarkingMethod(val name: String, val description: String){
	override def toString: String = name
}

object MarkingMethod {
	case object StudentsChooseMarker extends MarkingMethod("StudentsChooseMarker", "Students choose marker")
	case object SeenSecondMarkingLegacy extends MarkingMethod("SeenSecondMarkingLegacy", "Seen second marking - legacy")
	case object SeenSecondMarking extends MarkingMethod("SeenSecondMarking", "Seen second marking")
	case object ModeratedMarking extends MarkingMethod("ModeratedMarking", "Moderated marking")
	case object FirstMarkerOnly extends MarkingMethod("FirstMarkerOnly", "First marker only")

	val values: Set[MarkingMethod] = Set(
		StudentsChooseMarker,
		SeenSecondMarkingLegacy,
		SeenSecondMarking,
		ModeratedMarking,
		FirstMarkerOnly
	)

	def fromCode(code: String): MarkingMethod =
		if (code == null) null
		else values.find{_.name == code} match {
			case Some(method) => method
			case None => throw new IllegalArgumentException()
		}
}

class MarkingMethodUserType extends AbstractStringUserType[MarkingMethod]{
	override def convertToObject(string: String): MarkingMethod = MarkingMethod.fromCode(string)
	override def convertToValue(state: MarkingMethod): String = state.name
}

class StringToMarkingMethod extends TwoWayConverter[String, MarkingMethod] {
	override def convertRight(source: String): MarkingMethod = source.maybeText.map(MarkingMethod.fromCode).orNull
	override def convertLeft(source: MarkingMethod): String = Option(source).map { _.name }.orNull
}