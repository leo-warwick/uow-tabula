package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet
import javax.sql.DataSource

import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.`object`.MappingSqlQueryWithParameters
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.{UpstreamModuleList, UpstreamModuleListEntry}
import uk.ac.warwick.tabula.services.AutowiringCourseAndRouteServiceComponent
import uk.ac.warwick.tabula.services.scheduling.ModuleListImporter.{UpstreamModuleListEntityQuery, UpstreamModuleListQuery}
import collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import util.Try
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.jdbc.core.SqlParameter
import java.sql.Types


trait ModuleListImporter {

	def getModuleLists: Seq[UpstreamModuleList]

	def getModuleListEntries(lists: Seq[UpstreamModuleList]): Seq[UpstreamModuleListEntry]

}

@Profile(Array("dev", "test", "production"))
@Service
class ModuleListImporterImpl extends ModuleListImporter with InitializingBean
	with TaskBenchmarking with AutowiringCourseAndRouteServiceComponent {

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	var moduleListQuery: UpstreamModuleListQuery = _
	var moduleListEntityQuery: UpstreamModuleListEntityQuery = _

	override def afterPropertiesSet() {
		moduleListQuery = new UpstreamModuleListQuery(sits)
		moduleListEntityQuery = new UpstreamModuleListEntityQuery(sits)
	}

	override def getModuleLists: Seq[UpstreamModuleList] = {
		val rows = benchmarkTask("Fetch module lists") { moduleListQuery.execute }
		val validRows = rows.asScala.filter(r => r.routeCode.hasText && r.yearOfStudy.nonEmpty && r.academicYear.nonEmpty)
		val routeCodes = validRows.map(_.routeCode).distinct
		val routes = courseAndRouteService.getRoutesByCodes(routeCodes)
		validRows.groupBy(_.routeCode).flatMap { case(routeCode, groupedRows) => routes.find(_.code == routeCode) match {
			case Some(route) => groupedRows.map(row => new UpstreamModuleList(
				row.code,
				row.academicYear.get,
				route,
				row.yearOfStudy.get
			))
			case _ => Seq()
		}}.toSeq
	}

	override def getModuleListEntries(lists: Seq[UpstreamModuleList]): Seq[UpstreamModuleListEntry] = {
		if (lists.isEmpty) {
			Seq()
		} else {
			val rows = benchmarkTask("Fetch module list entities") {
				moduleListEntityQuery.executeByNamedParam(JMap(
					"lists" -> lists.map(_.code).asJava
				))
			}
			rows.asScala.groupBy(_.list).flatMap { case (listCode, groupedRows) => lists.find(_.code == listCode) match {
				case Some(list) => groupedRows.map(row => new UpstreamModuleListEntry(list, row.glob))
				case _ => Seq()
			}
			}.toSeq
		}
	}
}

@Profile(Array("sandbox"))
@Service
class SandboxModuleListImporter extends ModuleListImporter {

	override def getModuleLists: Seq[UpstreamModuleList] = Seq()

	override def getModuleListEntries(lists: Seq[UpstreamModuleList]): Seq[UpstreamModuleListEntry] = Seq()
}

object ModuleListImporter {

	var sitsSchema: String = Wire.property("${schema.sits}")
	var dialectRegexpLike = "regexp_like"

	def GetModuleLists: String = """
		select
			fmc.fmc_code as code
		from %s.cam_fmc fmc
		where
			fmc.fmc_iuse = 'Y'
			and %s(fmc.fmc_code, '\w{4}-\d-\d{2}-\w\w\w')
	""".format(sitsSchema, dialectRegexpLike)

	case class UpstreamModuleListRow(
		code: String,
		routeCode: String,
		yearOfStudy: Option[Int],
		academicYear: Option[AcademicYear]
	)

	class UpstreamModuleListQuery(ds: DataSource) extends MappingSqlQuery[UpstreamModuleListRow](ds, GetModuleLists) {
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int): UpstreamModuleListRow = {
			val codeParts = rs.getString("code").split("-")
			if (codeParts.length == 4) {
				UpstreamModuleListRow(
					rs.getString("code"),
					codeParts(0).toLowerCase,
					Try(codeParts(1).toInt).toOption,
					Try(codeParts(2).toInt).toOption.map(yy => AcademicYear(2000 + yy))
				)
			} else {
				UpstreamModuleListRow(rs.getString("code"),	"",	None,	None)
			}

		}
	}

	def GetModuleListEntities: String = """
		select
			fme.fmc_code as list,
	 		fme.fme_modp as glob
		from %s.cam_fme fme
		where fme.fmc_code in (:lists)
			and %s(fme.fmc_code, '\w{4}-\d-\d{2}-\w\w\w')
	""".format(sitsSchema, dialectRegexpLike)

	case class UpstreamModuleListEntityRow(
		list: String,
		glob: String
	)

	class UpstreamModuleListEntityQuery(ds: DataSource) extends MappingSqlQueryWithParameters[UpstreamModuleListEntityRow](ds, GetModuleListEntities) {
		declareParameter(new SqlParameter("lists", Types.VARCHAR))
		this.compile()
		override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): UpstreamModuleListEntityRow = {
			UpstreamModuleListEntityRow(
				rs.getString("list"),
				rs.getString("glob")
			)
		}
	}

}