package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConversions._

trait Tier4VisaImporter {

	def hasTier4Visa(universityId: String): Boolean // true if the student has a VIS record with a tier 4 type
}

@Profile(Array("dev", "test", "production")) @Service
class Tier4VisaImporterImpl extends Tier4VisaImporter {
	import Tier4VisaImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val tier4VisaMappingQuery = new Tier4VisaMappingQuery(sits)

	def hasTier4Visa(universityId: String): Boolean = {
		val rowCount = tier4VisaMappingQuery.executeByNamedParam(Map("universityId" -> universityId)).head
		(rowCount.intValue() > 0)
	}
}

@Profile(Array("sandbox")) @Service
class SandboxTier4VisaImporter extends Tier4VisaImporter {
	def hasTier4Visa(universityId: String): Boolean = false
}

object Tier4VisaImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")
	var nonTier4VisaTypes: String = Wire.property("${nonTier4VisaTypes}")

	def Tier4VisaSql = f"""
			select count(*) as count from $sitsSchema.srs_vis -- visa details
			where vis_stuc = :universityId
			and vis_iuse = 'Y'
			and vis_udf1 not in ($nonTier4VisaTypes)
		"""


	class Tier4VisaMappingQuery(ds: DataSource)
		extends MappingSqlQueryWithParameters[(Number)](ds, Tier4VisaSql) {
		this.declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): JLong = {
			(rs.getLong("count"))
		}
	}
}

trait Tier4VisaImporterComponent {
	def tier4VisaImporter: Tier4VisaImporter
}

trait AutowiringTier4VisaImporterComponent extends Tier4VisaImporterComponent{
	var tier4VisaImporter: Tier4VisaImporter = Wire[Tier4VisaImporter]
}
