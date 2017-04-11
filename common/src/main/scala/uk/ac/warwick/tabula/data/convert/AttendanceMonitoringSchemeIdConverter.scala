package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import org.springframework.beans.factory.annotation.Autowired

class AttendanceMonitoringSchemeIdConverter extends TwoWayConverter[String, AttendanceMonitoringScheme] {

	@Autowired var service: AttendanceMonitoringService = _

	override def convertRight(id: String): AttendanceMonitoringScheme = (Option(id) flatMap { service.getSchemeById }).orNull
	override def convertLeft(scheme: AttendanceMonitoringScheme): String = (Option(scheme) map {_.id}).orNull

}
