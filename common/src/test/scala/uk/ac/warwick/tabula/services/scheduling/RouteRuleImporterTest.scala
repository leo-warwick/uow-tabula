package uk.ac.warwick.tabula.services.scheduling

import org.junit.After
import org.springframework.jdbc.datasource.embedded.{EmbeddedDatabase, EmbeddedDatabaseBuilder}
import uk.ac.warwick.tabula.data.model.{Level, Route, UpstreamModuleList}
import uk.ac.warwick.tabula.services.{CourseAndRouteService, LevelService}
import uk.ac.warwick.tabula.services.exams.grids.UpstreamModuleListService
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import scala.jdk.CollectionConverters._

class RouteRuleImporterTest extends TestBase with Mockito {

  val sits: EmbeddedDatabase = new EmbeddedDatabaseBuilder().addScript("sits-module-lists.sql").build()

  @After def afterTheFeast(): Unit = {
    sits.shutdown()
  }

  val routeRuleImporter = new RouteRuleImporterImpl
  routeRuleImporter.sits = sits
  RouteRuleImporter.sitsSchema = "public"
  routeRuleImporter.afterPropertiesSet()

  val academicYear = AcademicYear(2014)

  val route1: Route = Fixtures.route("a100")
  val route2: Route = Fixtures.route("b100")
  val route3: Route = Fixtures.route("c100")
  val route4: Route = Fixtures.route("d100")
  val allRoutes = Seq(route1, route2, route3, route4)

  val moduleList = new UpstreamModuleList("A100-1-14-CAA", "A100 year 1 options list A", "A100 CA A", academicYear, route1, 1)
  val allLevels = Seq(new Level("1", "level 1"), new Level("2", "level 2"))

  @Test
  def routeRules(): Unit = {
    routeRuleImporter.courseAndRouteService = smartMock[CourseAndRouteService]
    routeRuleImporter.courseAndRouteService.getRoutesByCodes(any[Seq[String]]) returns allRoutes
    routeRuleImporter.upstreamModuleListService = smartMock[UpstreamModuleListService]
    routeRuleImporter.upstreamModuleListService.findByCodes(any[Seq[String]]) returns Seq(moduleList)
    routeRuleImporter.levelService = smartMock[LevelService]
    routeRuleImporter.levelService.getAllLevels returns allLevels

    val result = routeRuleImporter.getRouteRules
    result.size should be(3)
    result.flatMap(_.entries.asScala).size should be(4)
    val route1Rule = result.find(_.route == route1).get
    route1Rule.entries.size should be(2)
  }

}
