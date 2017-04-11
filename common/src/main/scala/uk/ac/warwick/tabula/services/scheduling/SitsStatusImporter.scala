package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportSitsStatusCommand
import uk.ac.warwick.tabula.data.SitsStatusDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.JavaConverters._

trait SitsStatusImporter extends Logging {
	var sitsStatusDao: SitsStatusDao = Wire.auto[SitsStatusDao]

	var sitsStatusMap:Map[String,SitsStatus] = null

	def getSitsStatusForCode(code:String): Option[SitsStatus] = {
			if (sitsStatusMap == null){
				sitsStatusMap=slurpSitsStatuses()
			}
		sitsStatusMap.get(code)
	}


	def getSitsStatuses(): Seq[ImportSitsStatusCommand]

	def slurpSitsStatuses(): Map[String, SitsStatus] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS status map")

			(for (statusCode <- sitsStatusDao.getAllStatusCodes; status <- sitsStatusDao.getByCode(statusCode)) yield {
				(statusCode, status)
			}).toMap
		}
	}
}

@Profile(Array("dev", "test", "production")) @Service
class SitsStatusImporterImpl extends SitsStatusImporter {
	import SitsStatusImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val sitsStatusesQuery = new SitsStatusesQuery(sits)

	def getSitsStatuses(): Seq[ImportSitsStatusCommand] = {
		val statuses = sitsStatusesQuery.execute.asScala.toSeq
		sitsStatusMap = slurpSitsStatuses()
		statuses
	}
}

@Profile(Array("sandbox")) @Service
class SandboxSitsStatusImporter extends SitsStatusImporter {
	def getSitsStatuses(): Seq[ImportSitsStatusCommand] =
		Seq(
			new ImportSitsStatusCommand(SitsStatusInfo("C", "CURRENT STUDENT", "Current Student")),
			new ImportSitsStatusCommand(SitsStatusInfo("P", "PERMANENTLY W/D", "Permanently Withdrawn"))
		)
}

case class SitsStatusInfo(code: String, shortName: String, fullName: String)

object SitsStatusImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

	def GetSitsStatus = f"""
		select sta_code, sta_snam, sta_name from $sitsSchema.srs_sta
		"""

	class SitsStatusesQuery(ds: DataSource) extends MappingSqlQuery[ImportSitsStatusCommand](ds, GetSitsStatus) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) =
			new ImportSitsStatusCommand(
				SitsStatusInfo(resultSet.getString("sta_code"), resultSet.getString("sta_snam"), resultSet.getString("sta_name"))
			)
	}

}

trait SitsStatusImporterComponent {
	def sitsStatusImporter: SitsStatusImporter
}

trait AutowiringSitsStatusImporterComponent extends SitsStatusImporterComponent {
	var sitsStatusImporter: SitsStatusImporter = Wire[SitsStatusImporter]
}