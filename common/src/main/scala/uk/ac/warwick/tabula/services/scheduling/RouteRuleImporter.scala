package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet
import javax.sql.DataSource

import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportMemberHelpers
import uk.ac.warwick.tabula.data.model.{UpstreamRouteRule, UpstreamRouteRuleEntry}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.scheduling.RouteRuleImporter.{UpstreamRouteRuleQuery, UpstreamRouteRuleRow}
import uk.ac.warwick.tabula.services.AutowiringCourseAndRouteServiceComponent
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.services.exams.grids.AutowiringUpstreamModuleListServiceComponent

import scala.collection.JavaConverters._
import scala.util.matching.Regex


trait RouteRuleImporter {

	def getRouteRules: Seq[UpstreamRouteRule]

}

@Profile(Array("dev", "test", "production"))
@Service
class RouteRuleImporterImpl extends RouteRuleImporter with InitializingBean
	with TaskBenchmarking with AutowiringCourseAndRouteServiceComponent with AutowiringUpstreamModuleListServiceComponent {

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	var routeRuleQuery: UpstreamRouteRuleQuery = _

	override def afterPropertiesSet() {
		routeRuleQuery = new UpstreamRouteRuleQuery(sits)
	}

	override def getRouteRules: Seq[UpstreamRouteRule] = {
		val rows = benchmarkTask("Fetch route rules") { routeRuleQuery.execute }
		// Remove rows that have null entires that aren't allowed
		val nonEmptyRows = rows.asScala.filter(r => r.routeCode.hasText && r.yearOfStudy.nonEmpty && r.moduleListCode != null &&  r.moduleListCode.nonEmpty)
		// Batch fetch the routes and module lists
		val routeCodes = nonEmptyRows.map(_.routeCode).distinct
		val moduleListCodes = nonEmptyRows.map(_.moduleListCode).distinct
		val routes = transactional(readOnly = true) { courseAndRouteService.getRoutesByCodes(routeCodes) }
		val moduleLists = transactional(readOnly = true) { upstreamModuleListService.findByCodes(moduleListCodes) }
		// Remove rows that have invalid routes and module lists
		val validRows: Seq[UpstreamRouteRuleRow] = nonEmptyRows.groupBy(r => (r.routeCode, r.moduleListCode))
			.filter { case((routeCode, moduleListCode), groupedRows) =>
				routes.exists(_.code == routeCode) && moduleLists.exists(_.code == moduleListCode)
			}.values.flatten.toSeq

		validRows.groupBy(r => (r.routeCode, r.yearOfStudy, r.academicYear)).map { case((routeCode, yearOfStudy, academicYearOption), groupedRows) =>
			val route = routes.find(_.code == routeCode).get
			val rule = new UpstreamRouteRule(academicYearOption, route, yearOfStudy.get)
			rule.entries.addAll(groupedRows.map(row => new UpstreamRouteRuleEntry(
				rule,
				moduleLists.find(_.code == row.moduleListCode).get,
				row.minCats,
				row.maxCats,
				row.minModules,
				row.maxModules
			)).asJava)
			rule
		}.toSeq
	}
}

@Profile(Array("sandbox"))
@Service
class SandboxRouteRuleImporter extends RouteRuleImporter {

	override def getRouteRules: Seq[UpstreamRouteRule] = Seq()

}

object RouteRuleImporter {

	var sitsSchema: String = Wire.property("${schema.sits}")

	val academicYearPattern: Regex = ".*(\\d\\d/\\d\\d).*".r

	def GetRouteRules: String = """
		select
			pmr.pwy_code as route_code,
			pmr.lev_code as year_of_study,
			pmr.pmr_desc as description,
	 		pmb.fmc_code as module_list,
			pmb.pmb_min as min_cats,
			pmb.pmb_max as max_cats,
			pmb.pmb_minm as min_modules,
			pmb.pmb_maxm as max_modules
		from %s.cam_pmr pmr
			join %s.cam_pmb pmb on pmb.pwy_code = pmr.pwy_code and pmb.pmr_code = pmr.pmr_code
	""".format(sitsSchema, sitsSchema)

	case class UpstreamRouteRuleRow(
		routeCode: String,
		yearOfStudy: Option[Int],
		academicYear: Option[AcademicYear],
		moduleListCode: String,
		minCats: Option[BigDecimal],
		maxCats: Option[BigDecimal],
		minModules: Option[Int],
		maxModules: Option[Int]
	)

	class UpstreamRouteRuleQuery(ds: DataSource) extends MappingSqlQuery[UpstreamRouteRuleRow](ds, GetRouteRules) {
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): UpstreamRouteRuleRow = {
			val academicYear = rs.getString("description").maybeText.flatMap {
				case academicYearPattern(academicYearString) => Option(academicYearString)
				case _ => None
			}.map(AcademicYear.parse)
			UpstreamRouteRuleRow(
				rs.getString("route_code").maybeText.map(_.toLowerCase).orNull,
				ImportMemberHelpers.getInteger(rs, "year_of_study"),
				academicYear,
				rs.getString("module_list"),
				Option(rs.getBigDecimal("min_cats")).map(BigDecimal.apply),
				Option(rs.getBigDecimal("max_cats")).map(BigDecimal.apply),
				ImportMemberHelpers.getInteger(rs, "min_modules"),
				ImportMemberHelpers.getInteger(rs, "max_modules")
			)
		}
	}

}