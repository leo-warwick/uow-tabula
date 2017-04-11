package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import javax.sql.DataSource

import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

import scala.collection.JavaConversions._

trait SupervisorImporter {
	/**
	 * Returns a sequence of pairs of PRS codes and the percentage load
	 */

	def getSupervisorUniversityIds(scjCode: String, relationshipType: StudentRelationshipType): Seq[(String, JBigDecimal)]
}

@Profile(Array("dev", "test", "production")) @Service
class SupervisorImporterImpl extends SupervisorImporter {
	import SupervisorImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val supervisorMappingQuery = new SupervisorMappingQuery(sits)

	def getSupervisorUniversityIds(scjCode: String, relationshipType: StudentRelationshipType): Seq[(String, JBigDecimal)] = {
		supervisorMappingQuery.executeByNamedParam(Map("scj_code" -> scjCode, "sits_examiner_type" -> relationshipType.defaultRdxType))
	}
}

@Profile(Array("sandbox")) @Service
class SandboxSupervisorImporter extends SupervisorImporter {
	def getSupervisorUniversityIds(scjCode: String, relationshipType: StudentRelationshipType): Seq[(String, JBigDecimal)] = Seq.empty // TODO
}

object SupervisorImporter {
	var sitsSchema: String = Wire.property("${schema.sits}")

	def GetSupervisorsSql = f"""
		select
			prs_udf1,
			rdx_perc -- percentage
		from $sitsSchema.srs_rdx rdx, -- Research Degree Examiner
		$sitsSchema.ins_prs prs -- Personnel
		where rdx_scjc = :scj_code
		and rdx_extc = :sits_examiner_type
		and rdx.rdx_prsc = prs.prs_code
		"""

	class SupervisorMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[(String, JBigDecimal)](ds, GetSupervisorsSql) {
		this.declareParameter(new SqlParameter("scj_code", Types.VARCHAR))
		this.declareParameter(new SqlParameter("sits_examiner_type", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): (String, JBigDecimal) = {
			(rs.getString("prs_udf1"), rs.getBigDecimal("rdx_perc"))
		}
	}
}
