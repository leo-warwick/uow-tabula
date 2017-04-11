package uk.ac.warwick.tabula.data.model

import javax.persistence._

import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpointTotal, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.groups._

/**
 * Stores a reference to an entity that is being pointed at in a
 * Notification. There is a subclass of this for every different
 * entity that we would need to point to. Hibernate's subclass magic
 * will work out which one to make and thus which table to get the
 * object from.
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entity_type")
abstract class EntityReference[A >: Null <: AnyRef] extends GeneratedId {
	// Maps to Notification.items
	@ManyToOne
	@JoinColumn(name = "notification_id")
	var notification: Notification[_, _] = _

	var entity: A

	def put(e: A): this.type = {
		this.entity = e
		this
	}

	type Entity = A
}

/*
 A subtype for each type of object.

 The entity property is eager-loaded - some of these types (mainly the abstract
 ones) may cause errors if lazy loading is turned on, as it will fail casting
 the proxy abstract class to the specific subtype.
 */

@Entity @DiscriminatorValue(value="assignment")
class AssignmentEntityReference extends EntityReference[Assignment] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="submission")
class SubmissionEntityReference extends EntityReference[Submission] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="feedback")
class AssignmentFeedbackEntityReference extends EntityReference[AssignmentFeedback] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="examFeedback")
class ExamFeedbackEntityReference extends EntityReference[ExamFeedback] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="markerFeedback")
class MarkerFeedbackEntityReference extends EntityReference[MarkerFeedback] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="module")
class ModuleEntityReference extends EntityReference[Module] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="extension")
class ExtensionEntityReference extends EntityReference[Extension] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="studentRelationship")
class StudentRelationshipEntityReference extends EntityReference[StudentRelationship] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="meetingRecord")
class MeetingRecordEntityReference extends EntityReference[AbstractMeetingRecord] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="meetingRecordApprovel")
class MeetingRecordApprovalEntityReference extends EntityReference[MeetingRecordApproval] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="smallGroup")
class SmallGroupEntityReference extends EntityReference[SmallGroup] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="smallGroupSet")
class SmallGroupSetEntityReference extends EntityReference[SmallGroupSet] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="smallGroupEvent")
class SmallGroupEventEntityReference extends EntityReference[SmallGroupEvent] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="smallGroupEventOccurrence")
class SmallGroupEventOcurrenceEntityReference extends EntityReference[SmallGroupEventOccurrence] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="departmentSmallGroupSet")
class DepartmentSmallGroupSetEntityReference extends EntityReference[DepartmentSmallGroupSet] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="departmentSmallGroup")
class DepartmentSmallGroupEntityReference extends EntityReference[DepartmentSmallGroup] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="originalityReport")
class OriginalityReportEntityReference extends EntityReference[OriginalityReport] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="department")
class DepartmentEntityReference extends EntityReference[Department] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="exam")
class ExamEntityReference extends EntityReference[Exam] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="attendanceMonitoringScheme")
class AttendanceMonitoringSchemeEntityReference extends EntityReference[AttendanceMonitoringScheme] {
	@ManyToOne
	var entity: Entity = _
}

@Entity @DiscriminatorValue(value="attendanceMonitoringCheckpointTotal")
class AttendanceMonitoringCheckpointTotalEntityReference extends EntityReference[AttendanceMonitoringCheckpointTotal] {
	@ManyToOne
	var entity: Entity = _
}