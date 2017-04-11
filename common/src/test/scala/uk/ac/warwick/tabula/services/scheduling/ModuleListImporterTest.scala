package uk.ac.warwick.tabula.services.scheduling

import org.junit.After
import org.springframework.jdbc.datasource.embedded.{EmbeddedDatabase, EmbeddedDatabaseBuilder}
import uk.ac.warwick.tabula.data.model.{Route, UpstreamModuleList}
import uk.ac.warwick.tabula.services.CourseAndRouteService
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class ModuleListImporterTest extends TestBase with Mockito {
	
	val sits: EmbeddedDatabase = new EmbeddedDatabaseBuilder().addScript("sits-module-lists.sql").build()

	@After def afterTheFeast() {
		sits.shutdown()
	}

	val moduleListImporter = new ModuleListImporterImpl
	moduleListImporter.sits = sits
	ModuleListImporter.sitsSchema = "public"
	ModuleListImporter.dialectRegexpLike = "regexp_matches"
	moduleListImporter.afterPropertiesSet()

	val route: Route = Fixtures.route("a100")

	@Test
	def moduleLists(): Unit = {
		moduleListImporter.courseAndRouteService = smartMock[CourseAndRouteService]
		moduleListImporter.courseAndRouteService.getRoutesByCodes(Seq("a100", "b100")) returns Seq(route)
		val result = moduleListImporter.getModuleLists
		result.size should be (1)
		result.head.route.code should be ("a100")
	}

	@Test
	def moduleListEntities(): Unit = {
		val list1 = new UpstreamModuleList("A100-1-14-CAA", AcademicYear(2014), route, 1)
		val result = moduleListImporter.getModuleListEntries(Seq(list1))
		result.size should be (3)
		result.forall(entry => entry.pattern.matcher("CH100-7.5").matches()) should be {true}
	}

}
