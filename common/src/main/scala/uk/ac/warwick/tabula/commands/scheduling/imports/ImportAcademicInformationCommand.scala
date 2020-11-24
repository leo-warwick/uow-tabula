package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportAcademicInformationCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.tabula.system.permissions._

import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.RoundingMode

object ImportAcademicInformationCommand {
  def apply() =
    new ImportAcademicInformationCommandInternal
      with ComposableCommandWithoutTransaction[ImportAcademicInformationResults]
      with ImportDepartments
      with ImportModules
      with ImportModuleTeachingDepartments
      with ImportRoutes
      with ImportRouteTeachingDepartments
      with ImportCourses
      with ImportSitsStatuses
      with ImportModesOfAttendance
      with ImportAwards
      with ImportDisabilities
      with ImportLevels
      with ImportClassifications
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringModuleImporterComponent
      with AutowiringCourseAndRouteServiceComponent
      with AutowiringCourseImporterComponent
      with AutowiringSitsStatusImporterComponent
      with AutowiringModeOfAttendanceImporterComponent
      with AutowiringAwardImporterComponent
      with AutowiringDisabilityImporterComponent
      with AutowiringLevelImporterComponent
      with AutowiringClassificationImporterComponent
      with ImportSystemDataPermissions
      with ImportAcademicInformationDescription
      with Logging

  case class ImportResult(added: Int = 0, deleted: Int = 0, changed: Int = 0)

  case class ImportAcademicInformationResults(
    departments: ImportResult,
    modules: ImportResult,
    moduleTeachingDepartments: ImportResult,
    routes: ImportResult,
    routeTeachingDepartments: ImportResult,
    courses: ImportResult,
    sitsStatuses: ImportResult,
    modesOfAttendance: ImportResult,
    awards: ImportResult,
    disabilities: ImportResult,
    levels: ImportResult,
    classifications: ImportResult,
  )

  def combineResults(results: Seq[ImportResult]): ImportResult =
    results.foldLeft(ImportResult())((acc, result) => ImportResult(
      added = acc.added + result.added,
      deleted = acc.deleted + result.deleted,
      changed = acc.changed + result.changed
    ))

  def newModuleFrom(m: ModuleInfo, dept: Department): Module = {
    val module = new Module
    module.code = m.code
    module.name = m.name
    module.shortName = m.shortName
    // TODO TAB-87 check child department rules and maybe sort it into a child department instead
    module.adminDepartment = dept
    module.degreeType = m.degreeType
    module
  }

  def newModuleTeachingDepartmentFrom(i: ModuleTeachingDepartmentInfo, moduleAndDepartmentService: ModuleAndDepartmentService): ModuleTeachingInformation = {
    val info = new ModuleTeachingInformation

    // Don't try and handle badly specified codes, just let the .get fail
    info.module = moduleAndDepartmentService.getModuleByCode(i.code).get
    info.department = moduleAndDepartmentService.getDepartmentByCode(i.departmentCode).get
    info.percentage = i.percentage

    info
  }

  def newDepartmentFrom(d: DepartmentInfo, moduleAndDepartmentService: ModuleAndDepartmentService): Department = {
    val department = new Department
    department.code = d.code
    department.fullName = d.fullName
    department.shortName = d.shortName
    department.isImportDepartment = d.parentCode.isEmpty

    d.parentCode.foreach { code =>
      // Don't try and handle a badly-specified code - just let the .get fail
      department.parent = moduleAndDepartmentService.getDepartmentByCode(code).get
    }
    d.filterName.foreach { name =>
      department.filterRule = new DepartmentFilterRuleUserType().convertToObject(name)
    }
    department
  }

  def newRouteFrom(r: RouteInfo, dept: Department): Route = {
    val route = new Route
    route.code = r.code
    route.name = r.name
    route.degreeType = r.degreeType
    route.adminDepartment = dept
    route
  }

  def newRouteTeachingDepartmentFrom(i: RouteTeachingDepartmentInfo, courseAndRouteService: CourseAndRouteService, moduleAndDepartmentService: ModuleAndDepartmentService): RouteTeachingInformation = {
    val info = new RouteTeachingInformation

    // Don't try and handle badly specified codes, just let the .get fail
    info.route = courseAndRouteService.getRouteByCode(i.code).get
    info.department = moduleAndDepartmentService.getDepartmentByCode(i.departmentCode).getOrElse {
      throw new IllegalArgumentException(s"Couldn't import ${i.code} because department ${i.departmentCode} doesn't exist")
    }
    info.percentage = i.percentage

    info
  }

  def scaled(bg: JBigDecimal): JBigDecimal =
    JBigDecimal(Option(bg).map(_.setScale(2, RoundingMode.HALF_UP)))
}

class ImportAcademicInformationCommandInternal extends CommandInternal[ImportAcademicInformationResults] with TaskBenchmarking {
  self: ImportDepartments with
    ImportModules with
    ImportModuleTeachingDepartments with
    ImportRoutes with
    ImportRouteTeachingDepartments with
    ImportCourses with
    ImportSitsStatuses with
    ImportModesOfAttendance with
    ImportAwards with
    ImportDisabilities with
    ImportLevels with
    ImportClassifications  =>

  def applyInternal() = ImportAcademicInformationResults(
    departments = benchmarkTask("Import departments") {
      transactional() {
        importDepartments()
      }
    },
    modules = benchmarkTask("Import modules") {
      transactional() {
        importModules()
      }
    },
    moduleTeachingDepartments = benchmarkTask("Import module teaching departments") {
      transactional() {
        importModuleTeachingDepartments()
      }
    },
    routes = benchmarkTask("Import routes") {
      transactional() {
        importRoutes()
      }
    },
    routeTeachingDepartments = benchmarkTask("Import route teaching departments") {
      transactional() {
        importRouteTeachingDepartments()
      }
    },
    courses = benchmarkTask("Import courses") {
      transactional() {
        importCourses()
      }
    },
    sitsStatuses = benchmarkTask("Import SITS status codes") {
      transactional() {
        importSitsStatuses()
      }
    },
    modesOfAttendance = benchmarkTask("Import modes of attendance") {
      transactional() {
        importModesOfAttendance()
      }
    },
    awards = benchmarkTask("Import awards") {
      transactional() {
        importAwards()
      }
    },
    disabilities = benchmarkTask("Import disabilities") {
      transactional() {
        importDisabilities()
      }
    },
    levels = benchmarkTask("Import levels") {
      transactional() {
        importLevels()
      }
    },
    classifications = benchmarkTask("Import classifications") {
      transactional() {
        importClassifications()
      }
    },
  )
}

trait ImportDepartments {
  self: ModuleAndDepartmentServiceComponent with ModuleImporterComponent with Logging =>

  def importDepartments(): ImportResult = {
    logger.info("Importing departments")

    val results = for (dept <- moduleImporter.getDepartments()) yield {
      moduleAndDepartmentService.getDepartmentByCode(dept.code) match {
        case None =>
          moduleAndDepartmentService.saveOrUpdate(newDepartmentFrom(dept, moduleAndDepartmentService))

          ImportResult(added = 1)
        case Some(department) =>
          if (department.fullName != dept.fullName || department.shortName != dept.shortName || !department.isImportDepartment) {
            department.fullName = dept.fullName
            department.shortName = dept.shortName
            department.isImportDepartment = true
            moduleAndDepartmentService.saveOrUpdate(department)
            ImportResult(changed = 1)
          } else {
            ImportResult()
          }
      }
    }

    combineResults(results)
  }
}

trait ImportModules {
  self: ModuleAndDepartmentServiceComponent with ModuleImporterComponent with Logging =>

  def importModules(): ImportResult = {
    logger.info("Importing modules")

    // track the module codes imported - start with an empty sequence to hold the codes
    var seenModuleCodes = Seq[String]()

    val results = for (dept <- moduleAndDepartmentService.allDepartments) yield {
      val (resultsForDepartment, seenModuleCodesForDepartment) = importModules(moduleImporter.getModules(dept.code), dept)
      seenModuleCodes = seenModuleCodes ++ seenModuleCodesForDepartment
      resultsForDepartment
    }

    moduleAndDepartmentService.stampMissingModules(seenModuleCodes)

    combineResults(results)
  }

  def importModules(dept: Department): ImportResult = {
    val (importResult: ImportResult, _) = importModules(moduleImporter.getModules(dept.code), dept)
    // don't stamp missing modules in this case since we we've only looked at one department and they could be in any
    importResult
  }

  def importModules(modules: Seq[ModuleInfo], dept: Department): (ImportResult, Seq[String]) = {

    var seenModuleCodesForDepartment = Seq[String]()

    val results = for (mod <- modules) yield {
      moduleAndDepartmentService.getModuleByCode(mod.code) match {
        case None =>
          debug("Mod code %s not found in database, so inserting", mod.code)
          moduleAndDepartmentService.saveOrUpdate(newModuleFrom(mod, dept))

          ImportResult(added = 1)
        case Some(module) =>

          seenModuleCodesForDepartment = seenModuleCodesForDepartment :+ module.code

          module.active = mod.status match {
            case Some("S-") => false
            case Some("D") => false
            case Some(_) => true
            case _ => false
          }

          // HFC-354 Update module details if they have changed.
          if (mod.name != module.name || mod.shortName != module.shortName || mod.degreeType != module.degreeType || dept.code != module.sitsDepartmentCode) {
            logger.info("Updating module info for %s, Name - %s, Shortname - %s, Degreetype - %s, SITS department - %s".format(mod.code, mod.name, mod.shortName, mod.degreeType, dept.code))
            module.name = mod.name
            module.shortName = mod.shortName
            module.degreeType = mod.degreeType
            module.sitsDepartmentCode = dept.code
            module.missingFromImportSince = null
            moduleAndDepartmentService.saveOrUpdate(module)
            ImportResult(changed = 1)
          } else {
            module.missingFromImportSince = null
            moduleAndDepartmentService.saveOrUpdate(module)

            ImportResult()
          }
      }
    }

    (combineResults(results), seenModuleCodesForDepartment)
  }
}

trait ImportModuleTeachingDepartments {
  self: ModuleAndDepartmentServiceComponent with ModuleImporterComponent with Logging =>

  def importModuleTeachingDepartments(): ImportResult = {
    logger.info("Importing module teaching departments")

    val results = for (module <- moduleAndDepartmentService.allModules) yield {
      importModuleTeachingDepartments(moduleImporter.getModuleTeachingDepartments(module.code), module)
    }

    combineResults(results)
  }

  def importModuleTeachingDepartments(module: Module): ImportResult = {
    importModuleTeachingDepartments(moduleImporter.getModuleTeachingDepartments(module.code), module)
  }

  def importModuleTeachingDepartments(moduleTeachingDepartments: Seq[ModuleTeachingDepartmentInfo], module: Module): ImportResult = {
    val seenDepartments = moduleTeachingDepartments.map(_.departmentCode).flatMap(moduleAndDepartmentService.getDepartmentByCode)

    val deletions =
      module.teachingInfo.asScala
        .filterNot { info => seenDepartments.contains(info.department) }
        .map { info =>
          module.teachingInfo.remove(info)
          moduleAndDepartmentService.delete(info)
          ImportResult(deleted = 1)
        }

    val additions =
      moduleTeachingDepartments.map { info =>
        moduleAndDepartmentService.getModuleTeachingInformation(info.code.toLowerCase(), info.departmentCode.toLowerCase()) match {
          case None =>
            debug(s"Teaching info for ${info.departmentCode} on ${info.code} not found in database, so inserting")
            moduleAndDepartmentService.saveOrUpdate(newModuleTeachingDepartmentFrom(info, moduleAndDepartmentService))

            ImportResult(added = 1)
          case Some(teachingInfo) =>
            // Update percentage if it changes
            if (scaled(teachingInfo.percentage) != scaled(info.percentage)) {
              logger.info(s"Updating percentage of $teachingInfo to ${info.percentage}")
              teachingInfo.percentage = info.percentage
              moduleAndDepartmentService.saveOrUpdate(teachingInfo)

              ImportResult(changed = 1)
            } else {
              ImportResult()
            }
        }
      }

    combineResults((deletions ++ additions).toSeq)
  }
}

trait ImportRoutes {
  self: ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent with ModuleImporterComponent with Logging =>

  def importRoutes(): ImportResult = {
    logger.info("Importing routes")
    val results = for (dept <- moduleAndDepartmentService.allDepartments) yield {
      importRoutes(moduleImporter.getRoutes(dept.code), dept)
    }

    combineResults(results)
  }

  def importRoutes(routes: Seq[RouteInfo], dept: Department): ImportResult = {
    val results = for (rot <- routes) yield {
      courseAndRouteService.getRouteByCode(rot.code) match {
        case None =>
          debug("Route code %s not found in database, so inserting", rot.code)
          courseAndRouteService.save(newRouteFrom(rot, dept))

          ImportResult(added = 1)
        case Some(route) =>
          // HFC-354 Update route name if it changes.
          val nameChanged = rot.name != route.name
          if (nameChanged) {
            logger.info("Updating name of %s to %s".format(rot.code, rot.name))
            route.name = rot.name
          }

          val activeChanged = rot.active != route.active
          if (activeChanged) {
            logger.info(s"Setting ${route.code} to ${if (rot.active) "active" else "inactive"}")
            route.active = rot.active
          }

          val sitsDepartmentChanged = dept.code != route.sitsDepartmentCode
          if (sitsDepartmentChanged) {
            logger.info("Updating SITS department of %s to %s".format(rot.code, dept.code))
            route.sitsDepartmentCode = dept.code
          }

          val degreeTypeChanged = rot.degreeType != route.degreeType
          if (degreeTypeChanged) {
            logger.info("Updating degreeType of %s to %s".format(rot.degreeType, route.degreeType))
            route.degreeType = rot.degreeType
          }

          if (nameChanged || activeChanged || sitsDepartmentChanged || degreeTypeChanged) {
            courseAndRouteService.save(route)
            ImportResult(changed = 1)
          } else {
            ImportResult()
          }
      }
    }

    combineResults(results)
  }
}

trait ImportRouteTeachingDepartments {
  self: CourseAndRouteServiceComponent with ModuleAndDepartmentServiceComponent with ModuleImporterComponent with Logging =>

  def importRouteTeachingDepartments(): ImportResult = {
    logger.info("Importing route teaching departments")

    val results = for (route <- courseAndRouteService.allRoutes) yield {
      importRouteTeachingDepartments(moduleImporter.getRouteTeachingDepartments(route.code), route)
    }

    combineResults(results)
  }

  def importRouteTeachingDepartments(route: Route): ImportResult = {
    importRouteTeachingDepartments(moduleImporter.getRouteTeachingDepartments(route.code), route)
  }

  def importRouteTeachingDepartments(routeTeachingDepartments: Seq[RouteTeachingDepartmentInfo], route: Route): ImportResult = {
    // TAB-2943
    //		val seenDepartments = routeTeachingDepartments.map(_.departmentCode).flatMap(moduleAndDepartmentService.getDepartmentByCode)
    //
    //		val deletions =
    //			route.teachingInfo.asScala
    //				.filterNot { info => seenDepartments.contains(info.department) }
    //				.map { info =>
    //					route.teachingInfo.remove(info)
    //					courseAndRouteService.delete(info)
    //					ImportResult(deleted = 1)
    //				}

    val additions =
      routeTeachingDepartments.map { info =>
        courseAndRouteService.getRouteTeachingInformation(info.code.toLowerCase(), info.departmentCode.toLowerCase()) match {
          case None =>
            debug(s"Teaching info for ${info.departmentCode} on ${info.code} not found in database, so inserting")
            courseAndRouteService.saveOrUpdate(newRouteTeachingDepartmentFrom(info, courseAndRouteService, moduleAndDepartmentService))

            ImportResult(added = 1)
          case Some(teachingInfo) =>
            // Update percentage if it changes
            if (scaled(teachingInfo.percentage) != scaled(info.percentage)) {
              logger.info(s"Updating percentage of $teachingInfo to ${info.percentage}")
              teachingInfo.percentage = info.percentage
              courseAndRouteService.saveOrUpdate(teachingInfo)

              ImportResult(changed = 1)
            } else {
              ImportResult()
            }
        }
      }

    combineResults(additions)
  }
}

trait ImportCourses {
  self: CourseImporterComponent with Logging =>

  def importCourses(): ImportResult = {
    logger.info("Importing courses")
    courseImporter.importCourses()
  }

}

trait ImportSitsStatuses {
  self: SitsStatusImporterComponent with Logging =>

  def importSitsStatuses(): ImportResult = {
    logger.info("Importing SITS statuses")

    transactional() {
      val results = sitsStatusImporter.getSitsStatuses().map(_.apply()._2)

      combineResults(results)
    }
  }
}

trait ImportModesOfAttendance {
  self: ModeOfAttendanceImporterComponent with Logging =>

  def importModesOfAttendance(): ImportResult = {
    logger.info("Importing modes of attendance")

    transactional() {
      val results = modeOfAttendanceImporter.getImportCommands().map(_.apply()._2)

      combineResults(results)
    }
  }

}

trait ImportAwards {
  self: AwardImporterComponent with Logging =>

  def importAwards(): ImportResult = {
    logger.info("Importing awards")
    awardImporter.importAwards()
  }
}

trait ImportDisabilities {
  self: DisabilityImporterComponent with Logging =>

  def importDisabilities(): ImportResult = {
    logger.info("Importing disabilities")
    disabilityImporter.importDisabilities()
  }
}

trait ImportLevels {
  self: LevelImporterComponent with Logging =>

  def importLevels(): ImportResult = {
    logger.info("Importing levels")
    levelImporter.importLevels()
  }
}


trait ImportClassifications {
  self: ClassificationImporterComponent with Logging =>

  def importClassifications(): ImportResult = {
    logger.info("Importing classification")
    classificationImporter.importClassifications()
  }
}

trait ImportSystemDataPermissions extends RequiresPermissionsChecking {
  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.ImportSystemData)
  }
}

trait ImportAcademicInformationDescription extends Describable[ImportAcademicInformationResults] {
  override lazy val eventName = "ImportAcademicInformation"

  def describe(d: Description): Unit = {}

  override def describeResult(d: Description, result: ImportAcademicInformationResults): Unit = {
    def importProperties(d: Description, name: String, result: ImportResult): Description = {
      d.properties(
        s"${name}Added" -> result.added,
        s"${name}Deleted" -> result.deleted,
        s"${name}Changed" -> result.changed
      )
    }

    importProperties(d, "departments", result.departments)
    importProperties(d, "modules", result.modules)
    importProperties(d, "routes", result.routes)
    importProperties(d, "courses", result.courses)
    importProperties(d, "sitsStatuses", result.sitsStatuses)
    importProperties(d, "modesOfAttendance", result.modesOfAttendance)
    importProperties(d, "awards", result.awards)
    importProperties(d, "disabilities", result.disabilities)
    importProperties(d, "levels", result.levels)
  }
}

object ImportDepartmentsModulesCommand {
  def apply() =
    new ImportDepartmentsModulesCommandInternal()
      with ComposableCommand[Unit]
      with ImportModules
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringModuleImporterComponent
      with ImportDepartmentsModulesDescription
      with ImportDepartmentsModulesState
      with ImportSystemDataPermissions
      with Logging
}

class ImportDepartmentsModulesCommandInternal() extends CommandInternal[Unit]
  with TaskBenchmarking {
  self: ImportDepartmentsModulesState with ImportModules with ModuleAndDepartmentServiceComponent =>

  def applyInternal(): Unit = transactional() {
    benchmarkTask("Import modules") {
      val codes = deptCode.split(",")
      val departments = codes.flatMap(moduleAndDepartmentService.getDepartmentByCode)
      departments.foreach(dept => importModules(dept))
    }
  }
}

trait ImportDepartmentsModulesState {
  var deptCode: String = _
}

trait ImportDepartmentsModulesDescription extends Describable[Unit] {
  self: ImportDepartmentsModulesState =>
  def describe(d: Description): Unit =
    d.property("department" -> deptCode)
}
