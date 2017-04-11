package uk.ac.warwick.tabula.data
import scala.collection.JavaConverters._

import org.springframework.stereotype.Repository

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.ModeOfAttendance

trait DecodeDao {
	def saveOrUpdate(modeOfAttendance: ModeOfAttendance)
	def getByCode(code: String): Option[ModeOfAttendance]
	def getAllStatusCodes: Seq[String]
	def getFullName(code: String): Option[String]
}

