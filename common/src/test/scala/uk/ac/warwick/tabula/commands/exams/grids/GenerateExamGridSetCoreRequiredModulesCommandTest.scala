package uk.ac.warwick.tabula.commands.exams.grids

import org.mockito.ArgumentCaptor
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.validation.{BindException, BindingResult}
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.tabula.JavaImports.{JArrayList, JHashSet}
import uk.ac.warwick.tabula.data.convert.RouteCodeConverter
import uk.ac.warwick.tabula.data.{StudentCourseYearDetailsDao, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model.{CoreRequiredModule, Department, Module, Route}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

import collection.JavaConverters._

class GenerateExamGridSetCoreRequiredModulesCommandTest extends TestBase with Mockito {

	val thisDepartment: Department = Fixtures.department("its")
	val thisAcademicYear = AcademicYear(2014)
	val route1: Route = Fixtures.route("its1")
	val route2: Route = Fixtures.route("its2")
	val thisYearOfStudy = 2
	val module1: Module = Fixtures.module("its01")
	val module2: Module = Fixtures.module("its02")
	val module3: Module = Fixtures.module("its03")

	@Test
	def populate(): Unit = {
		val coreRequiredModule1 = new CoreRequiredModule(route1, thisAcademicYear, thisYearOfStudy, module1)
		val coreRequiredModule2 = new CoreRequiredModule(route2, thisAcademicYear, thisYearOfStudy, module2)
		val command = new PopulateGenerateExamGridSetCoreRequiredModulesCommand()
			with GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
			with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent
			with ModuleAndDepartmentServiceComponent with StudentCourseYearDetailsDaoComponent {
			override val department: Department = thisDepartment
			override val academicYear: AcademicYear = thisAcademicYear
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
			override val studentCourseYearDetailsDao: StudentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
		}
		command.routes = JArrayList(route1, route2)
		command.yearOfStudy = thisYearOfStudy
		command.moduleRegistrationService.findCoreRequiredModules(route1, thisAcademicYear, thisYearOfStudy) returns Seq(coreRequiredModule1)
		command.moduleRegistrationService.findCoreRequiredModules(route2, thisAcademicYear, thisYearOfStudy) returns Seq(coreRequiredModule2)
		command.populate()
		verify(command.moduleRegistrationService, times(1)).findCoreRequiredModules(route1, thisAcademicYear, thisYearOfStudy)
		verify(command.moduleRegistrationService, times(1)).findCoreRequiredModules(route2, thisAcademicYear, thisYearOfStudy)
		command.modules.asScala(route1).asScala should be (Set(module1))
		command.modules.asScala(route2).asScala should be (Set(module2))
	}

	@Test
	def validate(): Unit = {
		val command = new GenerateExamGridSetCoreRequiredModulesCommandValidation()
			with GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
			with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent
			with ModuleAndDepartmentServiceComponent with StudentCourseYearDetailsDaoComponent {
			override val department: Department = thisDepartment
			override val academicYear: AcademicYear = thisAcademicYear
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
			override val studentCourseYearDetailsDao: StudentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
		}

		command.routes = JArrayList(route1)
		command.yearOfStudy = thisYearOfStudy
		command.moduleAndDepartmentService.findByRouteYearAcademicYear(route1, thisYearOfStudy, thisAcademicYear) returns Seq(module1)

		val duffModule = Fixtures.module("its2")
		command.modules.put(route1, JHashSet(duffModule))

		val conversionService = new GenericConversionService()
		val routeCodeConverter = new RouteCodeConverter
		routeCodeConverter.service = smartMock[CourseAndRouteService]
		routeCodeConverter.service.getRouteByCode(route1.code) returns Option(route1)
		conversionService.addConverter(routeCodeConverter)
		val binder = new WebDataBinder(command, "command")
		binder.setConversionService(conversionService)
		val errors: BindingResult = binder.getBindingResult

		command.validate(errors)
		verify(command.moduleAndDepartmentService, times(1)).findByRouteYearAcademicYear(route1, thisYearOfStudy, thisAcademicYear)
		errors.hasErrors should be {true}
		errors.getAllErrors.get(0).getArguments.apply(0).asInstanceOf[String].contains(duffModule.code) should be {true}

		command.modules.clear()
		command.modules.put(route1, JHashSet(module1))
		val errors1 = new BindException(command, "command")
		command.validate(errors1)
		verify(command.moduleAndDepartmentService, times(1)).findByRouteYearAcademicYear(route1, thisYearOfStudy, thisAcademicYear)
		errors1.hasErrors should be {false}
	}

	@Test
	def apply(): Unit = {
		val existingModule = new CoreRequiredModule(route1, thisAcademicYear, thisYearOfStudy, module1)
		val moduleToRemove = new CoreRequiredModule(route2, thisAcademicYear, thisYearOfStudy, module2)

		val command = new GenerateExamGridSetCoreRequiredModulesCommandInternal(thisDepartment, thisAcademicYear)
			with GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
			with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent
			with ModuleAndDepartmentServiceComponent with StudentCourseYearDetailsDaoComponent {
			override val moduleRegistrationService: ModuleRegistrationService = smartMock[ModuleRegistrationService]
			override val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
			override val studentCourseYearDetailsDao: StudentCourseYearDetailsDao = smartMock[StudentCourseYearDetailsDao]
		}
		command.routes = JArrayList(route1, route2)
		command.yearOfStudy = thisYearOfStudy
		command.moduleRegistrationService.findCoreRequiredModules(route1, thisAcademicYear, thisYearOfStudy) returns Seq(existingModule)
		command.moduleRegistrationService.findCoreRequiredModules(route2, thisAcademicYear, thisYearOfStudy) returns Seq(moduleToRemove)
		command.moduleAndDepartmentService.findByRouteYearAcademicYear(route1, thisYearOfStudy, thisAcademicYear) returns Seq(module1, module3)
		command.moduleAndDepartmentService.findByRouteYearAcademicYear(route2, thisYearOfStudy, thisAcademicYear) returns Seq(module2)

		command.modules.put(route1, JHashSet(module1, module3)) // New module, with existing module
		// No route2, so remove all its modules

		val result = command.applyInternal()
		verify(command.moduleRegistrationService, times(1)).findCoreRequiredModules(route1, thisAcademicYear, thisYearOfStudy)
		verify(command.moduleRegistrationService, times(1)).findCoreRequiredModules(route2, thisAcademicYear, thisYearOfStudy)
		verify(command.moduleRegistrationService, times(1)).delete(moduleToRemove)
		val argument: ArgumentCaptor[CoreRequiredModule]  = ArgumentCaptor.forClass(classOf[CoreRequiredModule])
		verify(command.moduleRegistrationService, times(2)).saveOrUpdate(argument.capture())
		val values = argument.getAllValues
		values.asScala.exists(m => m.module == module1 && m.route == route1 && m.academicYear == thisAcademicYear && m.yearOfStudy == thisYearOfStudy) should be (true)
		values.asScala.exists(m => m.module == module3 && m.route == route1 && m.academicYear == thisAcademicYear && m.yearOfStudy == thisYearOfStudy) should be (true)
		result(route1).size should be (2)
		result(route1).contains(existingModule) should be (true)
		result(route2).isEmpty should be (true)
	}

}
