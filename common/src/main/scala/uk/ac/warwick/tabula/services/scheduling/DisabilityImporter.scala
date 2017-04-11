package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportDisabilitiesCommand, ImportAcademicInformationCommand}
import uk.ac.warwick.tabula.data.DisabilityDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Disability
import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.JavaConversions.asScalaBuffer

trait DisabilityImporter extends Logging {
	var disabilityDao: DisabilityDao = Wire[DisabilityDao]

	private var disabilityMap: Map[String, Disability] = _

	def getDisabilityForCode(code: String): Disability = {
		if (disabilityMap == null) updateDisabilityMap()
		disabilityMap(code)
	}

	protected def updateDisabilityMap() {
		disabilityMap = slurpDisabilities()
	}

	private def slurpDisabilities(): Map[String, Disability] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS disability map")

			(for {
				disabilityCode <- disabilityDao.getAllDisabilityCodes
				disability <- disabilityDao.getByCode(disabilityCode)
			} yield (disabilityCode -> disability)).toMap

		}
	}

	def importDisabilities(): ImportAcademicInformationCommand.ImportResult = {
		val results = getImportCommands().map { _.apply()._2 }

		updateDisabilityMap()

		ImportAcademicInformationCommand.combineResults(results)
	}

	def getImportCommands(): Seq[ImportDisabilitiesCommand]
}

@Profile(Array("dev", "test", "production")) @Service
class SitsDisabilityImporter extends DisabilityImporter {
	import SitsDisabilityImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val disabilitysQuery = new DisabilitysQuery(sits)

	def getImportCommands: Seq[ImportDisabilitiesCommand] = {
		disabilitysQuery.execute.toSeq
	}
}

object SitsDisabilityImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// DSB in SITS is Disability Table
	def GetDisability = f"select dsb_code, dsb_snam, dsb_name from $sitsSchema.srs_dsb"

	class DisabilitysQuery(ds: DataSource) extends MappingSqlQuery[ImportDisabilitiesCommand](ds, GetDisability) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) =
			new ImportDisabilitiesCommand(
				DisabilityInfo(
					code = resultSet.getString("dsb_code"),
					shortName = resultSet.getString("dsb_snam"),
					definition = resultSet.getString("dsb_name")
				)
			)
	}

}

case class DisabilityInfo(code: String, shortName: String, definition: String)

@Profile(Array("sandbox")) @Service
class SandboxDisabilityImporter extends DisabilityImporter {

	def getImportCommands(): Seq[ImportDisabilitiesCommand] =
		Seq(
			// a sample range of disabilities which might affect administrative decisions in Tabula:
			new ImportDisabilitiesCommand(DisabilityInfo("04", "WHEELCHAIR", "Wheelchair user/mobility difficulties")),
			new ImportDisabilitiesCommand(DisabilityInfo("07", "UNSEEN DISAB.", "An unseen disability, e.g. diabetes, epilepsy, asthma")),
			new ImportDisabilitiesCommand(DisabilityInfo("10", "AS/DIS/ASP SYN", "Autistic Spectrum Disorder or Aspergers Syndrome"))
		)
}

trait DisabilityImporterComponent {
	def disabilityImporter: DisabilityImporter
}

trait AutowiringDisabilityImporterComponent extends DisabilityImporterComponent {
	var disabilityImporter: DisabilityImporter = Wire[DisabilityImporter]
}
