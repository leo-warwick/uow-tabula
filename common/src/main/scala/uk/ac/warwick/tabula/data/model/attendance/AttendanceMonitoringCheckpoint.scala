package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence.{Column, JoinColumn, FetchType, ManyToOne, Entity, ForeignKey}
import uk.ac.warwick.tabula.data.model.{StudentMember, GeneratedId}
import uk.ac.warwick.spring.Wire
import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.validation.constraints.NotNull

import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService

@Entity
class AttendanceMonitoringCheckpoint extends GeneratedId {

	@transient var attendanceMonitoringService: AttendanceMonitoringService = Wire[AttendanceMonitoringService]

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "point_id")
	@ForeignKey(name="none")
	var point: AttendanceMonitoringPoint = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id")
	@ForeignKey(name="none")
	var student: StudentMember = _

	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.AttendanceStateUserType")
	@Column(name = "state")
	private var _state: AttendanceState = _

	def state: AttendanceState = _state
	def state_=(state: AttendanceState) {
		if (attendanceMonitoringService.studentAlreadyReportedThisTerm(student, point)){
			throw new IllegalArgumentException
		}
		_state = state
	}

	def setStateDangerously(state: AttendanceState): Unit = {
		_state = state
	}

	@NotNull
	@Column(name = "updated_date")
	var updatedDate: DateTime = _

	@NotNull
	@Column(name = "updated_by")
	var updatedBy: String = _

	var autoCreated: Boolean = false

	@transient
	var activePoint = true

}