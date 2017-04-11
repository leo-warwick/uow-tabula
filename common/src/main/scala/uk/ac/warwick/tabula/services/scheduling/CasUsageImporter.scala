package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._

import scala.collection.JavaConversions._

trait CasUsageImporter {
	// cas is a confirmation of acceptance to study - this determines whether such was used to apply for a visa
	def isCasUsed(universityId: String): Boolean
}

@Profile(Array("dev", "test", "production")) @Service
class CasUsageImporterImpl extends CasUsageImporter {
	import CasUsageImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val casUsedMappingQuery = new CasUsedMappingQuery(sits)

	def isCasUsed(universityId: String): Boolean = {
		val earliestEndDate = DateTime.now.minusMonths(4).toDate
		val rowCount = casUsedMappingQuery.executeByNamedParam(Map("universityId" -> universityId, "earliestEndDate" -> earliestEndDate)).head
		rowCount.intValue > 0
	}
}

@Profile(Array("sandbox")) @Service
class SandboxCasUsageImporter extends CasUsageImporter {
	def isCasUsed(universityId: String) = false
}

object CasUsageImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

		// check to see if CAS is current or expired less than 3 months ago
		// VCR table in SITS is Visa CAS Request
		// CAS is Confirmation of Acceptance for Studies
    def CasUsedSql = f"""
			select count(*) as count from $sitsSchema.srs_vcr
			where vcr_stuc = :universityId
			and vcr_iuse = 'Y'
			and vcr_ukst in ('USED','ASSIGNED') -- CAS status
			and vcr_endd > :earliestEndDate
		"""

	class CasUsedMappingQuery(ds: DataSource)
		extends MappingSqlQueryWithParameters[(Number)](ds, CasUsedSql) {
		this.declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		this.declareParameter(new SqlParameter("earliestEndDate", Types.DATE))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): JLong = rs.getLong("count")
	}
}

trait CasUsageImporterComponent {
	def casUsageImporter: CasUsageImporter
}

trait AutowiringCasUsageImporterComponent extends CasUsageImporterComponent{
	var casUsageImporter: CasUsageImporter = Wire[CasUsageImporter]
}
