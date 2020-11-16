package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.apache.http.HttpEntity
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.AbstractResponseHandler
import org.apache.http.util.EntityUtils
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.{MappingSqlQuery, MappingSqlQueryWithParameters}
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.DegreeType
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.AutowiringApacheHttpClientComponent

import scala.jdk.CollectionConverters._
import scala.util.parsing.json.JSON

case class DepartmentInfo(fullName: String, shortName: String, code: String, faculty: String, parentCode: Option[String] = None, filterName: Option[String] = None)

case class ModuleInfo(name: String, shortName: String, code: String, group: String, degreeType: DegreeType, status: Option[String] = None)

case class ModuleTeachingDepartmentInfo(code: String, departmentCode: String, percentage: JBigDecimal)

case class RouteInfo(name: String, code: String, degreeType: DegreeType, active: Boolean)

case class RouteTeachingDepartmentInfo(code: String, departmentCode: String, percentage: JBigDecimal)

/**
  * Retrieves department and module information from an external location.
  */
trait ModuleImporter {
  def getModules(deptCode: String): Seq[ModuleInfo]

  def getModuleTeachingDepartments(moduleCode: String): Seq[ModuleTeachingDepartmentInfo]

  def getRoutes(deptCode: String): Seq[RouteInfo]

  def getRouteTeachingDepartments(routeCode: String): Seq[RouteTeachingDepartmentInfo]

  def getDepartments(): Seq[DepartmentInfo]
}

/**
  * Retrieves department and module information from Webgroups and SITS.
  */
@Profile(Array("dev", "test", "production"))
@Service
class ModuleImporterImpl extends ModuleImporter with Logging with AutowiringApacheHttpClientComponent {

  import ModuleImporter._

  var sits: DataSource = Wire[DataSource]("sitsDataSource")

  var departmentsApiUrl: String = Wire.property("${departments.api}")

  lazy val moduleInfoMappingQuery = new ModuleInfoMappingQuery(sits)
  lazy val moduleTeachingDepartmentMappingQuery = new ModuleTeachingDepartmentInfoMappingQuery(sits)
  lazy val routeInfoMappingQuery = new RouteInfoMappingQuery(sits)
  lazy val routeTeachingDepartmentMappingQuery = new RouteTeachingDepartmentInfoMappingQuery(sits)

  def getDepartments(): Seq[DepartmentInfo] = {
    httpClient.execute(RequestBuilder.get(departmentsApiUrl).build(), new AbstractResponseHandler[Seq[DepartmentInfo]] {
      override def handleEntity(entity: HttpEntity): Seq[DepartmentInfo] = {
        val json = EntityUtils.toString(entity)
        JSON.parseFull(json) match {
          case Some(departments: Seq[Map[String, Any]]@unchecked) =>
            departments
              .filter(_.get("code").collect { case s: String => s }.orNull.hasText)
              .map { properties =>
                DepartmentInfo(
                  fullName = properties.get("name").collect { case s: String => s }.orNull.safeTrim,
                  shortName = properties.get("shortName").collect { case s: String => s }.orNull.safeTrim,
                  code = properties.get("code").collect { case s: String => s }.orNull.toLowerCase,
                  faculty = properties.get("faculty").collect { case s: String => s }.orNull.safeTrim
                )
              }
          case _ => throw new IllegalArgumentException(s"Couldn't parse JSON $json")
        }
      }
    })
  }

  def getModules(deptCode: String): Seq[ModuleInfo] = moduleInfoMappingQuery.executeByNamedParam(JMap(
    "department_code" -> deptCode.toUpperCase
  )).asScala.toSeq

  def getModuleTeachingDepartments(moduleCode: String): Seq[ModuleTeachingDepartmentInfo] = moduleTeachingDepartmentMappingQuery.executeByNamedParam(JMap(
    "module_code" -> moduleCode.toUpperCase
  )).asScala.toSeq

  def getRoutes(deptCode: String): Seq[RouteInfo] = routeInfoMappingQuery.execute(deptCode.toUpperCase).asScala.toSeq

  def getRouteTeachingDepartments(routeCode: String): Seq[RouteTeachingDepartmentInfo] = routeTeachingDepartmentMappingQuery.executeByNamedParam(JMap(
    "route_code" -> routeCode.toUpperCase
  )).asScala.toSeq
}

@Profile(Array("sandbox"))
@Service
class SandboxModuleImporter extends ModuleImporter {

  def getDepartments(): Seq[DepartmentInfo] =
    SandboxData.Departments.toSeq map { case (code, d) => DepartmentInfo(d.name, d.name, d.code, d.facultyCode) }

  def getModules(deptCode: String): Seq[ModuleInfo] =
    SandboxData.Departments.get(deptCode).map(_.modules.toSeq.map { case (code, m) => ModuleInfo(m.name, m.shortName, m.code, deptCode + "-" + m.code, DegreeType.fromCode("UG")) }).getOrElse(Seq())

  def getModuleTeachingDepartments(moduleCode: String): Seq[ModuleTeachingDepartmentInfo] =
    SandboxData.Departments.values
      .find { department =>
        department.modules.keys.toSeq.contains(moduleCode)
      }
      .toSeq
      .flatMap { department =>
        department.modules
          .find { case (code, _) => code == moduleCode }
          .toSeq
          .map { case (code, m) => ModuleTeachingDepartmentInfo(code, department.code, JBigDecimal(Some(100))) }
      }

  def getRoutes(deptCode: String): Seq[RouteInfo] =
    SandboxData.Departments.get(deptCode).map(_.routes.toSeq map { case (code, r) => RouteInfo(r.name, r.code, r.degreeType, active = true) }).getOrElse(Seq())

  def getRouteTeachingDepartments(routeCode: String): Seq[RouteTeachingDepartmentInfo] =
    SandboxData.Departments.values
      .find { department =>
        department.routes.keys.toSeq.contains(routeCode)
      }
      .toSeq
      .flatMap { department =>
        department.routes
          .find { case (code, _) => code == routeCode }
          .toSeq
          .map { case (code, r) => RouteTeachingDepartmentInfo(code, department.code, JBigDecimal(Some(100))) }
      }

}

object ModuleImporter {
  var sitsSchema: String = Wire.property("${schema.sits}")

  final val GetModulesSql =
    s"""
		select substr(mod.mod_code,0,5) as code, max(mod.mod_name) as name, max(mod.mod_snam) as short_name, max(mod.sch_code) as scheme_code, max(mod.mot_code) as status
		  from $sitsSchema.ins_mod mod
		  where
		    mod.mod_code like '_____-%' and
		    mod.dpt_code = :department_code and
		    mod.mod_iuse = 'Y'
		  group by substr(mod.mod_code,0,5)
		"""

  final def GetModuleTeachingDepartmentsSql =
    f"""
		select substr(top.top_code, 0, 5) as code, top.dpt_code as department_code, min(top.top_perc) as percentage
			from $sitsSchema.cam_top top
				join $sitsSchema.ins_mod mod
					on mod.mod_code = top.mod_code and
						 mod.mod_iuse = 'Y' and
						 mod.mot_code not in ('S-', 'D')

			where
				substr(top.top_code, 0, 5) = :module_code and
				top.top_iuse = 'Y' and
				top.dpt_code is not null
			group by
				substr(top.top_code, 0, 5), top.dpt_code
		"""

  final def GetRoutesSql =
    s"""
		select
		  pwy.pwy_code as code,
		  pwy.pwy_name as name,
		  pwy.pwy_pwtc as degree_type,
		  pwy.pwy_iuse as in_use
		from $sitsSchema.ins_pwy pwy
		where
		  pwy.pwy_pwgc = :department_code and
		  pwy.pwy_pwtc in ('UG', 'PG', 'PGCE', 'IS')
		"""

  final val GetRouteTeachingDepartmentsSql =
    s"""
		select
			psd.psd_pwyc as code,
			psd.psd_dptc as department_code,
			psd.psd_perc as percentage
		from $sitsSchema.ins_psd psd
		where
			psd.psd_pwyc = :route_code
		"""

  class ModuleInfoMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[ModuleInfo](ds, GetModulesSql) {
    declareParameter(new SqlParameter("department_code", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): ModuleInfo = {
      val moduleCode = rs.getString("code").toLowerCase.safeTrim
      val deptCode = params(0).toString.toLowerCase
      val degreeType: DegreeType = DegreeType.getFromSchemeCode(rs.getString("scheme_code"))
      ModuleInfo(
        name = rs.getString("name").safeTrim,
        shortName = rs.getString("short_name"),
        code = moduleCode,
        group = deptCode + "-" + moduleCode,
        degreeType = degreeType,
        status = Option(rs.getString("status"))
      )
    }
  }

  class ModuleTeachingDepartmentInfoMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[ModuleTeachingDepartmentInfo](ds, GetModuleTeachingDepartmentsSql) {
    declareParameter(new SqlParameter("module_code", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): ModuleTeachingDepartmentInfo = {
      ModuleTeachingDepartmentInfo(
        rs.getString("code").toLowerCase.safeTrim,
        rs.getString("department_code").toLowerCase.safeTrim,
        rs.getBigDecimal("percentage")
      )
    }
  }

  class RouteInfoMappingQuery(ds: DataSource) extends MappingSqlQuery[RouteInfo](ds, GetRoutesSql) {
    declareParameter(new SqlParameter("department_code", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): RouteInfo = {
      val routeCode = rs.getString("code").toLowerCase.safeTrim
      RouteInfo(
        rs.getString("name").safeTrim,
        routeCode,
        DegreeType.fromCode(rs.getString("degree_type").safeTrim),
        rs.getString("in_use").safeTrim.equals("Y")
      )
    }
  }

  class RouteTeachingDepartmentInfoMappingQuery(ds: DataSource) extends MappingSqlQueryWithParameters[RouteTeachingDepartmentInfo](ds, GetRouteTeachingDepartmentsSql) {
    declareParameter(new SqlParameter("route_code", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int, params: Array[java.lang.Object], context: JMap[_, _]): RouteTeachingDepartmentInfo = {
      RouteTeachingDepartmentInfo(
        rs.getString("code").toLowerCase.safeTrim,
        rs.getString("department_code").toLowerCase.safeTrim,
        rs.getBigDecimal("percentage")
      )
    }
  }

}

trait ModuleImporterComponent {
  def moduleImporter: ModuleImporter
}

trait AutowiringModuleImporterComponent extends ModuleImporterComponent {
  var moduleImporter: ModuleImporter = Wire[ModuleImporter]
}
