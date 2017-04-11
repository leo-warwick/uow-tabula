package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import uk.ac.warwick.tabula.data.model.AttendanceNote

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("attendanceMonitoringPoint")
class AttendanceMonitoringNote extends AttendanceNote {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_id")
	var point: AttendanceMonitoringPoint = _

}