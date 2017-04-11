package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Type, BatchSize}
import uk.ac.warwick.tabula.JavaImports._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.GeneratedId


@Entity
@Table(name = "attendancetemplate")
class AttendanceMonitoringTemplate extends GeneratedId {

	@NotNull
	var templateName: String = _

	@NotNull
	var position: Int = _

	@OneToMany(mappedBy = "scheme", fetch = FetchType.LAZY, cascade=Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size=100)
	var points: JList[AttendanceMonitoringTemplatePoint] = JArrayList()

	@NotNull
	@Type(`type` = "uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPointStyleUserType")
	@Column(name = "point_style")
	var pointStyle: AttendanceMonitoringPointStyle = _

	var createdDate: DateTime = _

	var updatedDate: DateTime = _

}
