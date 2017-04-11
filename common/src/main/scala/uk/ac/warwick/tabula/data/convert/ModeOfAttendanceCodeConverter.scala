package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.data.model.ModeOfAttendance
import uk.ac.warwick.tabula.data.ModeOfAttendanceDao

class ModeOfAttendanceCodeConverter extends TwoWayConverter[String, ModeOfAttendance] {

	@Autowired var dao: ModeOfAttendanceDao = _

	override def convertRight(code: String): ModeOfAttendance = (Option(code) flatMap { dao.getByCode }).orNull
	override def convertLeft(moa: ModeOfAttendance): String = (Option(moa) map {_.code}).orNull

}