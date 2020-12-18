package uk.ac.warwick.tabula.commands

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets

import org.apache.http.client.utils.URLEncodedUtils
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.convert._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringModeOfAttendanceDaoComponent, AutowiringSitsStatusDaoComponent, ModeOfAttendanceDaoComponent, SitsStatusDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, AutowiringModuleAndDepartmentServiceComponent, CourseAndRouteServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.util.web.UriBuilder

import scala.jdk.CollectionConverters._

trait FiltersStudentsBase {

  def courseTypes: JList[CourseType]

  def specificCourseTypes: JList[SpecificCourseType]

  def routes: JList[Route]

  def courses: JList[Course]

  def modesOfAttendance: JList[ModeOfAttendance]

  def yearsOfStudy: JList[JInteger]

  def levelCodes: JList[String]

  def studyLevelCodes: JList[String]

  def sprStatuses: JList[SitsStatus]

  def modules: JList[Module]

  def defaultOrder: Seq[Order]

  def sortOrder: JList[Order]

  def hallsOfResidence: JList[String]

  def postcodes: JList[String]

  var otherCriteria: JList[String] = JArrayList()

  protected def modulesForDepartmentAndSubDepartments(department: Department): Seq[Module] =
    (department.modules.asScala.toSeq ++ department.children.asScala.toSeq.flatMap {
      modulesForDepartmentAndSubDepartments
    }).sorted

  protected def routesForDepartmentAndSubDepartments(department: Department): Seq[Route] =
    (department.routes.asScala.toSeq ++ department.children.asScala.toSeq.flatMap {
      routesForDepartmentAndSubDepartments
    }).sorted

  def serializeFilter: String = {
    val result = new UriBuilder()
    courseTypes.asScala.foreach(p => result.addQueryParameter("courseTypes", p.code))
    specificCourseTypes.asScala.foreach(p => result.addQueryParameter("specificCourseTypes", p.code.toString))
    routes.asScala.foreach(p => result.addQueryParameter("routes", p.code))
    courses.asScala.foreach(p => result.addQueryParameter("courses", p.code))
    modesOfAttendance.asScala.foreach(p => result.addQueryParameter("modesOfAttendance", p.code))
    yearsOfStudy.asScala.foreach(p => result.addQueryParameter("yearsOfStudy", p.toString))
    levelCodes.asScala.foreach(p => result.addQueryParameter("levelCodes", p))
    studyLevelCodes.asScala.foreach(p => result.addQueryParameter("studyLevelCodes", p))
    sprStatuses.asScala.foreach(p => result.addQueryParameter("sprStatuses", p.code))
    modules.asScala.foreach(p => result.addQueryParameter("modules", p.code))
    hallsOfResidence.asScala.foreach(p => result.addQueryParameter("hallsOfResidence", p))
    postcodes.asScala.foreach(p => result.addQueryParameter("postcodes", p))
    otherCriteria.asScala.foreach(p => result.addQueryParameter("otherCriteria", p))
    if (result.getQuery == null)
      ""
    else
      result.getQuery
  }

  def filterMap: Map[String, String] = {
    Map(
      "courseTypes" -> courseTypes.asScala.map(_.code).mkString(","),
      "specificCourseTypes" -> specificCourseTypes.asScala.map(_.code).mkString(","),
      "routes" -> routes.asScala.map(_.code).mkString(","),
      "courses" -> courses.asScala.map(_.code).mkString(","),
      "modesOfAttendance" -> modesOfAttendance.asScala.map(_.code).mkString(","),
      "yearsOfStudy" -> yearsOfStudy.asScala.mkString(","),
      "levelCodes" -> levelCodes.asScala.mkString(","),
      "sprStatuses" -> sprStatuses.asScala.map(_.code).mkString(","),
      "modules" -> modules.asScala.map(_.code).mkString(","),
      "hallsOfResidence" -> hallsOfResidence.asScala.mkString(","),
      "postcodes" -> postcodes.asScala.mkString(","),
      "otherCriteria" -> otherCriteria.asScala.mkString(",")
    )
  }

}

trait DeserializesFilter {
  def deserializeFilter(filterString: String): Unit
}

trait DeserializesFilterImpl extends DeserializesFilter with Logging with FiltersStudentsBase with CourseAndRouteServiceComponent with ModeOfAttendanceDaoComponent
  with SitsStatusDaoComponent with ModuleAndDepartmentServiceComponent {

  def deserializeFilter(filterString: String): Unit = {
    val params: Map[String, Seq[String]] = URLEncodedUtils.parse(new URI(null, null, null, URLDecoder.decode(filterString, "UTF-8"), null), StandardCharsets.UTF_8).asScala.toSeq.groupBy(_.getName).map {
      case (name, nameValuePairs) => name -> nameValuePairs.map(_.getValue)
    }
    courseTypes.clear()
    params.get("courseTypes").foreach(_.foreach { item =>
      try {
        courseTypes.add(CourseType(item))
      } catch {
        case _: IllegalArgumentException =>
          logger.warn(s"Could not deserialize filter with courseType $item")
      }
    })
    specificCourseTypes.clear()
    params.get("specificCourseTypes").foreach(_.foreach { item =>
      try {
        specificCourseTypes.add(SpecificCourseType.fromCourseCode(item))
      } catch {
        case _: IllegalArgumentException =>
          logger.warn(s"Could not deserialize filter with specificCourseTypes $item")
      }
    })
    routes.clear()
    params.get("routes").foreach(_.foreach { item =>
      val routeCodeConverter = new RouteCodeConverter
      routeCodeConverter.service = courseAndRouteService
      routeCodeConverter.convertRight(item) match {
        case route: Route => routes.add(route)
        case _ => logger.warn(s"Could not deserialize filter with route $item")
      }
    })
    courses.clear()
    params.get("courses").foreach(_.foreach { item =>
      val courseCodeConverter = new CourseCodeConverter
      courseCodeConverter.service = courseAndRouteService
      courseCodeConverter.convertRight(item) match {
        case course: Course => courses.add(course)
        case _ => logger.warn(s"Could not deserialize filter with course $item")
      }
    }
    )
    modesOfAttendance.clear()
    params.get("modesOfAttendance").foreach(_.foreach { item =>
      val modeOfAttendanceCodeConverter = new ModeOfAttendanceCodeConverter
      modeOfAttendanceCodeConverter.dao = modeOfAttendanceDao
      modeOfAttendanceCodeConverter.convertRight(item) match {
        case moa: ModeOfAttendance => modesOfAttendance.add(moa)
        case _ => logger.warn(s"Could not deserialize filter with modeOfAttendance $item")
      }
    }
    )
    yearsOfStudy.clear()
    params.get("yearsOfStudy").foreach(_.foreach { item =>
      try {
        yearsOfStudy.add(item.toInt)
      } catch {
        case e: NumberFormatException =>
          logger.warn(s"Could not deserialize filter with yearOfStudy $item")
      }
    }
    )
    levelCodes.clear()
    params.get("levelCodes").foreach(_.foreach { item => levelCodes.add(item) })
    sprStatuses.clear()
    params.get("sprStatuses").foreach(_.foreach { item =>
      val sitsStatusCodeConverter = new SitsStatusCodeConverter
      sitsStatusCodeConverter.dao = sitsStatusDao
      sitsStatusCodeConverter.convertRight(item) match {
        case sprStatus: SitsStatus => sprStatuses.add(sprStatus)
        case _ => logger.warn(s"Could not deserialize filter with sprStatus $item")
      }
    })
    modules.clear()
    params.get("modules").foreach(_.foreach { item =>
      val moduleCodeConverter = new ModuleCodeConverter
      moduleCodeConverter.service = moduleAndDepartmentService
      moduleCodeConverter.convertRight(item) match {
        case module: Module => modules.add(module)
        case _ => logger.warn(s"Could not deserialize filter with module $item")
      }
    })
    hallsOfResidence.clear()
    params.get("hallsOfResidence").foreach(_.foreach { item => hallsOfResidence.add(item) })
    postcodes.clear()
    params.get("postcodes").foreach(_.foreach { item => postcodes.add(item) })
    otherCriteria.clear()
    params.get("otherCriteria").foreach(_.foreach { item => otherCriteria.add(item) })
  }

}

trait AutowiringDeserializesFilterImpl extends DeserializesFilterImpl
  with AutowiringCourseAndRouteServiceComponent
  with AutowiringModeOfAttendanceDaoComponent
  with AutowiringModuleAndDepartmentServiceComponent
  with AutowiringSitsStatusDaoComponent
