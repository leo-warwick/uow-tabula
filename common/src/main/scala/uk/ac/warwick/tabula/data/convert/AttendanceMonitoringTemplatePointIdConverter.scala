package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplatePoint
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.system.TwoWayConverter


class AttendanceMonitoringTemplatePointIdConverter extends TwoWayConverter[String, AttendanceMonitoringTemplatePoint] {

	@Autowired var service: AttendanceMonitoringService = _

	override def convertRight(id: String): AttendanceMonitoringTemplatePoint = (Option(id) flatMap { service.getTemplatePointById }).orNull
	override def convertLeft(scheme: AttendanceMonitoringTemplatePoint): String = (Option(scheme) map {_.id}).orNull

}
