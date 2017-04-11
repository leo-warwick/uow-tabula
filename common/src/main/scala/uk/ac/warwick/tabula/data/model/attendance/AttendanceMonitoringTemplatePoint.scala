package uk.ac.warwick.tabula.data.model.attendance

import javax.persistence.{Table, Entity, JoinColumn, FetchType, ManyToOne, Column}
import uk.ac.warwick.tabula.data.model.GeneratedId
import javax.validation.constraints.NotNull
import org.joda.time.{DateTime, LocalDate}
import org.hibernate.annotations.Type

@Entity
@Table(name = "attendancetemplatepoint")
class AttendanceMonitoringTemplatePoint extends GeneratedId {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_template_id")
	var scheme: AttendanceMonitoringTemplate = _

	@NotNull
	var name: String = _

	@Column(name = "start_week")
	var startWeek: Int = _

	@Column(name = "end_week")
	var endWeek: Int = _

	@Column(name = "start_date")
	var startDate: LocalDate = _

	@Column(name = "end_date")
	var endDate: LocalDate = _

	@NotNull
	@Column(name = "created_date")
	var createdDate: DateTime = _

	@NotNull
	@Column(name = "updated_date")
	var updatedDate: DateTime = _

	def toPoint: AttendanceMonitoringPoint = {
		val point = new AttendanceMonitoringPoint
		point.name = name
		point.startWeek = startWeek
		point.endWeek = endWeek
		point.createdDate = new DateTime()
		point.updatedDate = new DateTime()
		point
	}

}
