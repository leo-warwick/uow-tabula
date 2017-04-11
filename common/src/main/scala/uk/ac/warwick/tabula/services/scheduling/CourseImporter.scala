package uk.ac.warwick.tabula.services.scheduling

import java.sql.ResultSet
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportAcademicInformationCommand, ImportCourseCommand}
import uk.ac.warwick.tabula.data.CourseDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Course
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.util.core.StringUtils

import scala.collection.JavaConversions._

trait CourseImporter extends Logging {
	var courseDao: CourseDao = Wire[CourseDao]

	private var courseMap: Map[String, Course] = _

	def getCourseByCodeCached(code: String): Option[Course] = {
		if (courseMap == null) updateCourseMap()

		code.maybeText.flatMap {
			courseCode => courseMap.get(courseCode)
		}
	}

	protected def updateCourseMap() {
		courseMap = slurpCourses()
	}

	private def slurpCourses(): Map[String, Course] = {
		transactional(readOnly = true) {
			logger.debug("refreshing SITS course map")

			(for {
				courseCode <- courseDao.getAllCourseCodes
				course <- courseDao.getByCode(courseCode)
			} yield courseCode -> course).toMap

		}
	}

	def importCourses(): ImportAcademicInformationCommand.ImportResult = {
		val results = buildImportCommands().map { _.apply()._2 }

		updateCourseMap()

		ImportAcademicInformationCommand.combineResults(results)
	}

	def buildImportCommands(): Seq[ImportCourseCommand]
}

@Profile(Array("dev", "test", "production"))
@Service
class SitsCourseImporter extends CourseImporter {
	import SitsCourseImporter._

	var sits: DataSource = Wire[DataSource]("sitsDataSource")

	lazy val coursesQuery = new CoursesQuery(sits)

	override def buildImportCommands(): Seq[ImportCourseCommand] = {
		coursesQuery.execute.toSeq
	}
}

object SitsCourseImporter {
	val sitsSchema: String = Wire.property("${schema.sits}")

	def GetCourse = f"""
		select crs_code, crs_snam, crs_name, crs_titl, crs_dptc from $sitsSchema.srs_crs
		"""

	class CoursesQuery(ds: DataSource) extends MappingSqlQuery[ImportCourseCommand](ds, GetCourse) {
		compile()
		override def mapRow(resultSet: ResultSet, rowNumber: Int) =
			new ImportCourseCommand(
				CourseInfo(
					code=resultSet.getString("crs_code"),
					shortName=resultSet.getString("crs_snam"),
					fullName=resultSet.getString("crs_name"),
					title=resultSet.getString("crs_titl"),
					departmentCode=resultSet.getString("crs_dptc")
				)
			)
	}

}

case class CourseInfo(code: String, shortName: String, fullName: String, title: String, departmentCode: String)

@Profile(Array("sandbox")) @Service
class SandboxCourseImporter extends CourseImporter {

	override def buildImportCommands(): Seq[ImportCourseCommand] =
		SandboxData.Departments
			.flatMap { case (departmentCode, department) =>
				department.routes.map { case (routeCode, route) =>
					new ImportCourseCommand(
						CourseInfo(
							code="%c%s-%s".format(route.courseType.courseCodeChar, departmentCode.toUpperCase, routeCode.toUpperCase),
							shortName=StringUtils.safeSubstring(route.name, 0, 20).toUpperCase,
							fullName=route.name,
							title="%s %s".format(route.degreeType.description, route.name),
							departmentCode=departmentCode.toUpperCase
						)
					)
				}
			}
			.toSeq

}

trait CourseImporterComponent {
	def courseImporter: CourseImporter
}

trait AutowiringCourseImporterComponent extends CourseImporterComponent {
	var courseImporter: CourseImporter = Wire[CourseImporter]
}
