package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence.{Column, DiscriminatorType, OneToMany, _}

import org.hibernate.annotations.{BatchSize, Type}
import org.joda.time.DateTime

import scala.collection.immutable.{SortedMap, TreeMap}
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, UserGroupCacheManager}


object CM2MarkingWorkflow {
	implicit val defaultOrdering: Ordering[CM2MarkingWorkflow] = Ordering.by {
		workflow: CM2MarkingWorkflow => workflow.name.toLowerCase
	}
}

@Entity
@Table(name="MarkingWorkflow")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="WorkflowType", discriminatorType = DiscriminatorType.STRING, length=255)
@Access(AccessType.FIELD)
abstract class CM2MarkingWorkflow extends GeneratedId with PermissionsTarget with Serializable {

	type Usercode = String
	type UniversityId = String
	type Marker = User

	/** A descriptive name for the users' reference. */
	@Basic(optional = false)
	var name: String = _

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "department_id")
	var department: Department = _

	@OneToMany(mappedBy = "cm2MarkingWorkflow", fetch = LAZY)
	@BatchSize(size = 200)
	var assignments: JList[Assignment] = JArrayList()

	@OneToMany(mappedBy = "workflow", fetch = LAZY, cascade = Array(ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var stageMarkers: JList[StageMarkers] = JArrayList()

	def markers: Map[MarkingWorkflowStage, Seq[Marker]] =
		stageMarkers.asScala.map(sm => sm.stage -> sm.markers.knownType.users.sortBy(u => (u.getLastName, u.getFirstName))).toMap

	// If two stages have the same roleName only keep the earliest stage.
	def markersByRole: SortedMap[MarkingWorkflowStage, Seq[Marker]] =  {
		val unsorted = markers.foldRight(Map.empty[MarkingWorkflowStage, Seq[Marker]]){ case ((s, m), acc) =>
			if (acc.keys.exists(_.roleName == s.roleName)) acc else acc + (s -> m)
		}
		TreeMap(unsorted.toSeq:_*)
	}

	@Column(name="is_reusable", nullable = false)
	var isReusable: JBoolean = false

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)

	// should be hardcoded to the MarkingWorkflowType with the same value as the implementations DiscriminatorValue :(
	def workflowType: MarkingWorkflowType

	def allStages: Seq[MarkingWorkflowStage] = workflowType.allStages
	def initialStages: Seq[MarkingWorkflowStage] = workflowType.initialStages

	def permissionsParents: Stream[Department] = Option(department).toStream

	// replace the markers for a specified stage in the workflow
	protected def replaceStageMarkers(stage:MarkingWorkflowStage, markers:Seq[Marker]): Unit = {
		stageMarkers.asScala.find(_.stage == stage).foreach(sm => {
			sm.markers.knownType.includedUserIds = markers.map(_.getUserId)
		})
	}
	// replace the markers in this workflow
	def replaceMarkers(markers: Seq[Marker]*): Unit

	def studentsChooseMarkers: Boolean = false

	def canDeleteMarkers: Boolean = {
		def oneHasSubmissions = assignments.asScala.exists(_.submissions.asScala.nonEmpty)
		def oneIsReleased = assignments.asScala.exists(_.allFeedback.nonEmpty)
		!((studentsChooseMarkers && oneHasSubmissions) || oneIsReleased)
	}
}

@Entity
@Table(name="StageMarkers")
@Access(AccessType.FIELD)
class StageMarkers extends GeneratedId with Serializable {

	def this(stage: MarkingWorkflowStage, workflow: CM2MarkingWorkflow){
		this()
		this.workflow = workflow
		this.stage = stage
	}

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStageUserType")
	var stage: MarkingWorkflowStage = _

	@ManyToOne(optional = false, cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id")
	var workflow: CM2MarkingWorkflow = _

	@transient
	var cm2MarkingWorkflowService: Option[CM2MarkingWorkflowService] = Wire.option[CM2MarkingWorkflowService]

	/** The group of markers for this stage */
	@OneToOne(cascade = Array(CascadeType.ALL), fetch = FetchType.LAZY)
	@JoinColumn(name = "markers")
	private var _markers = UserGroup.ofUsercodes
	def markers: UnspecifiedTypeUserGroup = {
		cm2MarkingWorkflowService match {
			case Some(service) =>
				new UserGroupCacheManager(_markers, service.markerHelper)
			case _ => _markers
		}
	}
	def markers_=(group: UserGroup) { _markers = group }

}