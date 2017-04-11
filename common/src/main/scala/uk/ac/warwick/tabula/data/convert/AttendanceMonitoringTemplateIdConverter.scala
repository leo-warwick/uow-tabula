package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringTemplate
import org.springframework.beans.factory.annotation.Autowired


class AttendanceMonitoringTemplateIdConverter extends TwoWayConverter[String, AttendanceMonitoringTemplate] {

	@Autowired var service: AttendanceMonitoringService = _

	override def convertRight(id: String): AttendanceMonitoringTemplate = (Option(id) flatMap { service.getTemplateSchemeById }).orNull
	override def convertLeft(scheme: AttendanceMonitoringTemplate): String = (Option(scheme) map {_.id}).orNull

}
