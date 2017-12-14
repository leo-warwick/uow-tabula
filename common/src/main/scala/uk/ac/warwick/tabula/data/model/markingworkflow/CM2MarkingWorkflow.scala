package uk.ac.warwick.tabula.data.model.markingworkflow

import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import javax.persistence.{Column, DiscriminatorType, OneToMany, _}

import org.hibernate.annotations.{BatchSize, Type}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.data.PostLoadBehaviour
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.UserOrdering._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.services.{CM2MarkingWorkflowService, UserGroupCacheManager}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._
import scala.collection.immutable.{ListMap, SortedSet, TreeMap}


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
abstract class CM2MarkingWorkflow extends GeneratedId with PermissionsTarget with Serializable with HasSettings with PostLoadBehaviour {

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
		stageMarkers.asScala.map(sm => sm.stage -> sm.markers.users.sorted).toMap

	def allMarkers: SortedSet[Marker] = SortedSet(markers.values.flatten.toSeq.distinct:_ *)

	// If two stages have the same roleName only keep the earliest stage.
	def markersByRole: Map[String, Seq[Marker]] =  {
		// filter out stages that share markers - we only want each set of markers once
		// using foldLeft is important here. we want to keep the earliest of the stages that shares a marker
		val unsorted = markers.foldLeft(Map.empty[MarkingWorkflowStage, Seq[Marker]]){ case (acc, (s, m)) =>
			if (acc.keys.exists(stage => stage.roleName == s.roleName)) acc else acc + (s -> m)
		}
		val sortedByStage = TreeMap(unsorted.toSeq:_*)
		// now that we have sorted by stage insert into a list map to preserve the order
		ListMap(sortedByStage.toSeq:_*).map{case (k,v) => k.roleName -> v}
	}

	@Column(name="is_reusable", nullable = false)
	var isReusable: JBoolean = false

	@Basic
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = AcademicYear.now()

	// should be hardcoded to the MarkingWorkflowType with the same value as the implementations DiscriminatorValue :(
	def workflowType: MarkingWorkflowType

	// sometimes we show roles to users when assigning markers and sometimes we show stages
	// this is a sorted list of either role names or stage allocationNames
	def allocationOrder: List[String] = {
		val stagesByRole = markerStages.groupBy(_.roleName)
		if(workflowType.rolesShareAllocations) {
			stagesByRole.keys.toList.sortBy(r => stagesByRole(r).map(_.order).min) // sort roles by their earliest stages
		} else {
			allStages.sortBy(_.order).map(_.allocationName).toList
		}
	}

	def allStages: Seq[MarkingWorkflowStage] = workflowType.allStages
	def markerStages: Seq[MarkingWorkflowStage] = workflowType.allStages.filter(_.hasMarkers)
	def initialStages: Seq[MarkingWorkflowStage] = workflowType.initialStages

	def permissionsParents: Stream[Department] = Option(department).toStream

	// replace the markers for a specified stage in the workflow
	protected def replaceStageMarkers(stage: MarkingWorkflowStage, markers: Seq[Marker]): Unit = {
		stageMarkers.asScala.find(_.stage == stage).foreach(sm => {
			// Do this in a roundabout way for the cache manager's benefit
			sm.markers.users.foreach(sm.markers.remove)
			markers.foreach(sm.markers.add)
		})
	}
	// replace the markers in this workflow
	def replaceMarkers(markers: Seq[Marker]*): Unit

	def studentsChooseMarkers: Boolean = false

	def canDeleteMarkers: Boolean = {
		def hasSubmissions = assignments.asScala.exists(_.submissions.asScala.nonEmpty)
		def markersAssigned = assignments.asScala.exists(_.markersAssigned)
		!((studentsChooseMarkers && hasSubmissions) || markersAssigned)
	}

	override def postLoad() {
		ensureSettings
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