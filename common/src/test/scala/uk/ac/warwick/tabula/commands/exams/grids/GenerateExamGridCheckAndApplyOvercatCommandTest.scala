package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{StudentCourseYearDetailsDao, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids.{NormalCATSLoadService, NormalCATSLoadServiceComponent, UpstreamRouteRuleService, UpstreamRouteRuleServiceComponent}

class GenerateExamGridCheckAndApplyOvercatCommandTest extends TestBase with Mockito {

	val thisDepartment: Department = Fixtures.department("its")
	val thisAcademicYear = AcademicYear(2014)
	val thisRoute: Route = Fixtures.route("a100")
	thisRoute.degreeType = DegreeType.Undergraduate
	val thisYearOfStudy = 3
	val module1: Module = Fixtures.module("its01")
	val module2: Module = Fixtures.module("its02")
	val mads: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
	mads.getModuleByCode(module1.code) returns Some(module1)
	mads.getModuleByCode(module2.code) returns Some(module2)
	val scd: StudentCourseDetails = Fixtures.student("1234").mostSignificantCourse
	val scyd: StudentCourseYearDetails = scd.latestStudentCourseYearDetails
	scyd.moduleAndDepartmentService = mads
	val mr1: ModuleRegistration = Fixtures.moduleRegistration(scd, module1, null, null)
	val mr2: ModuleRegistration = Fixtures.moduleRegistration(scd, module2, null, null)

	trait StateFixture {
		val state = new GenerateExamGridCheckAndApplyOvercatCommandState with UpstreamRouteRuleServiceComponent
			with ModuleRegistrationServiceComponent with NormalCATSLoadServiceComponent {
			override val department: Department = thisDepartment
			override val academicYear: AcademicYear = thisAcademicYear
			override val upstreamRouteRuleService: UpstreamRouteRuleService = smartMock[UpstreamRouteRuleService]
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val normalCATSLoadService: NormalCATSLoadService = smartMock[NormalCATSLoadService]
		}
		// Just use the default normal load
		state.normalCATSLoadService.find(thisRoute, thisAcademicYear, thisYearOfStudy) returns None
		// Have to have at least 1 route rule as otherwise it won't apply the change
		val routeRule = new UpstreamRouteRule(None, null, null)
		state.upstreamRouteRuleService.list(thisRoute, thisAcademicYear, thisYearOfStudy) returns Seq(routeRule)
	}

	@Test
	def stateFilterNotOvercat(): Unit = { new StateFixture {
		val entity = ExamGridEntity(null, null, null, null, Map(thisYearOfStudy -> ExamGridEntityYear(null, thisRoute.degreeType.normalCATSLoad, thisRoute, None, None, None)))
		val selectCourseCommand = new Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest {
			override def apply(): Seq[ExamGridEntity] = Seq(entity)
			yearOfStudy = thisYearOfStudy
		}
		state.selectCourseCommand = selectCourseCommand
		state.moduleRegistrationService.overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule)) returns Seq(null)
		state.filteredEntities.isEmpty should be {true}
	}}

	@Test
	def stateFilterNoOvercatSelection(): Unit = { new StateFixture {
		val entity = ExamGridEntity(null, null, null, null, Map(thisYearOfStudy -> ExamGridEntityYear(null, thisRoute.degreeType.normalCATSLoad + 15, thisRoute, None, None, None)))
		val selectCourseCommand = new Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest {
			override def apply(): Seq[ExamGridEntity] = Seq(entity)
			yearOfStudy = thisYearOfStudy
		}
		state.selectCourseCommand = selectCourseCommand
		state.moduleRegistrationService.overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule)) returns Seq(null, null)
		state.filteredEntities.isEmpty should be {false}
		state.filteredEntities.head should be (entity)
		verify(state.moduleRegistrationService, times(1)).overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule))
	}}

	@Test
	def stateFilterSameModule(): Unit = { new StateFixture {
		val entity = ExamGridEntity(null, null, null, null, Map(thisYearOfStudy -> ExamGridEntityYear(null, thisRoute.degreeType.normalCATSLoad + 15, thisRoute, Some(Seq(module1)), None, None)))

		val selectCourseCommand = new Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest {
			override def apply(): Seq[ExamGridEntity] = Seq(entity)
			yearOfStudy = thisYearOfStudy
		}
		state.selectCourseCommand = selectCourseCommand

		state.moduleRegistrationService.overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule)) returns Seq((BigDecimal(0), Seq(mr1)))

		state.filteredEntities.isEmpty should be {true}
		verify(state.moduleRegistrationService, times(1)).overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule))
	}}

	@Test
	def stateFilterDifferentModule(): Unit = { new StateFixture {
		val entity = ExamGridEntity(null, null, null, null, Map(thisYearOfStudy -> ExamGridEntityYear(null, thisRoute.degreeType.normalCATSLoad + 15, thisRoute, Some(Seq(module2)), None, None)))

		val selectCourseCommand = new Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest {
			override def apply(): Seq[ExamGridEntity] = Seq(entity)
			yearOfStudy = thisYearOfStudy
		}
		state.selectCourseCommand = selectCourseCommand

		state.moduleRegistrationService.overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule)) returns Seq(
			(BigDecimal(50), Seq(mr1)),
			(BigDecimal(40), Seq(mr2))
		)

		state.filteredEntities.isEmpty should be {false}
		state.filteredEntities.head should be (entity)
		state.overcatSubsets(state.filteredEntities.head).head should be (BigDecimal(50), Seq(mr1))
		verify(state.moduleRegistrationService, times(1)).overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule))
	}}

	@Test
	def stateFilterExtraModule(): Unit = { new StateFixture {
		val entity = ExamGridEntity(null, null, null, null, Map(thisYearOfStudy -> ExamGridEntityYear(null, thisRoute.degreeType.normalCATSLoad + 15, thisRoute, Some(Seq(module1)), None, None)))

		val selectCourseCommand = new Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest {
			override def apply(): Seq[ExamGridEntity] = Seq(entity)
			yearOfStudy = thisYearOfStudy
		}
		state.selectCourseCommand = selectCourseCommand

		state.moduleRegistrationService.overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule)) returns Seq(
			(BigDecimal(50), Seq(mr1, mr2)),
			(BigDecimal(40), Seq(mr1))
		)

		state.filteredEntities.isEmpty should be {false}
		state.filteredEntities.head should be (entity)
		state.overcatSubsets(state.filteredEntities.head).head should be (BigDecimal(50), Seq(mr1, mr2))
		verify(state.moduleRegistrationService, times(1)).overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule))
	}}

	@Test
	def stateFilterRemovedModule(): Unit = { new StateFixture {
		val entity = ExamGridEntity(null, null, null, null, Map(thisYearOfStudy -> ExamGridEntityYear(null, thisRoute.degreeType.normalCATSLoad + 15, thisRoute, Some(Seq(module1, module2)), None, None)))

		val selectCourseCommand = new Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest {
			override def apply(): Seq[ExamGridEntity] = Seq(entity)
			yearOfStudy = thisYearOfStudy
		}
		state.selectCourseCommand = selectCourseCommand

		state.moduleRegistrationService.overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule)) returns Seq(
			(BigDecimal(50), Seq(mr2)),
			(BigDecimal(40), Seq(mr1, mr2))
		)

		state.filteredEntities.isEmpty should be {false}
		state.filteredEntities.head should be (entity)
		state.overcatSubsets(state.filteredEntities.head).head should be (BigDecimal(50), Seq(mr2))
		verify(state.moduleRegistrationService, times(1)).overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule))
	}}

	@Test
	def apply(): Unit = {
		val entity = ExamGridEntity(null, null, null, null, Map(thisYearOfStudy -> ExamGridEntityYear(null, thisRoute.degreeType.normalCATSLoad + 15, thisRoute, None, None, Option(scyd))))

		var fetchCount = 0

		val selectCourseCommand = new Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest {
			override def apply(): Seq[ExamGridEntity] = {
				fetchCount = fetchCount + 1
				Seq(entity)
			}
			yearOfStudy = thisYearOfStudy
		}

		val cmd = new GenerateExamGridCheckAndApplyOvercatCommandInternal(null, thisAcademicYear, NoCurrentUser())
			with ModuleRegistrationServiceComponent with UpstreamRouteRuleServiceComponent
			with GenerateExamGridCheckAndApplyOvercatCommandState	with StudentCourseYearDetailsDaoComponent
			with NormalCATSLoadServiceComponent {
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val studentCourseYearDetailsDao: StudentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
			override val upstreamRouteRuleService: UpstreamRouteRuleService = smartMock[UpstreamRouteRuleService]
			override val normalCATSLoadService: NormalCATSLoadService = smartMock[NormalCATSLoadService]
		}
		// Just use the default normal load
		cmd.normalCATSLoadService.find(thisRoute, thisAcademicYear, thisYearOfStudy) returns None
		// Have to have at least 1 route rule as otherwise it won't apply the change
		val routeRule = new UpstreamRouteRule(None, null, null)
		cmd.upstreamRouteRuleService.list(thisRoute, thisAcademicYear, thisYearOfStudy) returns Seq(routeRule)
		cmd.selectCourseCommand = selectCourseCommand
		cmd.moduleRegistrationService.overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule)) returns Seq(
			(BigDecimal(50), Seq(mr1, mr2)),
			(BigDecimal(40), Seq(mr1))
		)
		cmd.filteredEntities.isEmpty should be {false}
		cmd.filteredEntities.head should be (entity)
		cmd.overcatSubsets(cmd.filteredEntities.head).head should be (BigDecimal(50), Seq(mr1, mr2))
		verify(cmd.moduleRegistrationService, times(1)).overcattedModuleSubsets(entity.years(thisYearOfStudy), Map(), thisRoute.degreeType.normalCATSLoad, Seq(routeRule))

		val result = cmd.applyInternal()
		result.entities.size should be (1)
		result.entities.head should be (entity)
		result.updatedEntities.keys.head should be (entity)
		entity.years(thisYearOfStudy).studentCourseYearDetails.get.overcattingModules.nonEmpty should be {true}
		entity.years(thisYearOfStudy).studentCourseYearDetails.get.overcattingModules.get should be (Seq(module1, module2))
		assert(fetchCount == 2)
	}

}
