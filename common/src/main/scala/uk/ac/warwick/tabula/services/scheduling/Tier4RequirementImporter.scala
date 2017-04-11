package uk.ac.warwick.tabula.services.scheduling

import java.lang.Long
import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JMap

import scala.collection.JavaConversions.{asScalaBuffer, mapAsJavaMap}

trait Tier4RequirementImporter {
	def hasTier4Requirement(universityId: String): Boolean
}

@Profile(Array("dev", "test", "production")) @Service
class Tier4RequirementImporterImpl extends Tier4RequirementImporter {
	import Tier4RequirementImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val tier4RequirementMappingQuery = new Tier4RequirementMappingQuery(sits)

	def hasTier4Requirement(universityId: String): Boolean = {
		val numNationalitiesNotNeedingVisa =
			tier4RequirementMappingQuery.executeByNamedParam(Map("universityId" -> universityId)).head
		(numNationalitiesNotNeedingVisa.intValue() == 0)
	}
}

@Profile(Array("sandbox")) @Service
class SandboxTier4RequirementImporter extends Tier4RequirementImporter {
	def hasTier4Requirement(universityId: String): Boolean = false
}

object Tier4RequirementImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

	// Originally this query also had a condition nat_iuse = 'Y' but we found that
	// there are current students with nationality codes which are not flagged as in
	// use.
	// Returns the number of nationalities for the student (1 or 2, which is the most SITS stores) which don't need a visa
	def GetTier4RequirementSql = f"""
			select count(nat_edid) as count from $sitsSchema.srs_nat -- nationality
			where nat_code in (
				(select stu_natc from $sitsSchema.ins_stu where stu_code = :universityId),
				(select stu_nat1 from $sitsSchema.ins_stu where stu_code = :universityId))
			and nat_edid = 0 -- indicates that the nationality does not require a visa
		"""

	class Tier4RequirementMappingQuery(ds: DataSource)
		extends MappingSqlQueryWithParameters[(Number)](ds, GetTier4RequirementSql) {
		this.declareParameter(new SqlParameter("universityId", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): Long = {
			(rs.getLong("count"))
		}
	}
}

trait Tier4RequirementImporterComponent {
	def tier4RequirementImporter: Tier4RequirementImporter
}

trait AutowiringTier4RequirementImporterComponent extends Tier4RequirementImporterComponent{
	var tier4RequirementImporter: Tier4RequirementImporter = Wire[Tier4RequirementImporter]
}
