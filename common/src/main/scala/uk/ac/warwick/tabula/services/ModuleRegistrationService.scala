package uk.ac.warwick.tabula.services

import org.joda.time.LocalDate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringModuleRegistrationDaoComponent, ModuleRegistrationDaoComponent}

import scala.math.BigDecimal.RoundingMode

trait ModuleRegistrationService {

  def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit

  def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit

  def delete(coreRequiredModule: CoreRequiredModule): Unit

  def getByNotionalKey(
    studentCourseDetails: StudentCourseDetails,
    module: Module,
    cats: JBigDecimal,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration]

  def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByYears(academicYears: Seq[AcademicYear]): Seq[ModuleRegistration]

  def getByUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistration]

  /**
    * Gets the weighted mean mark for the given module registrations.
    * Each agreed mark is multiplied by the CAT weighing of the module then added together, and the result is divided by the total CATS.
    * This is then rounded to 1 decimal place.
    *
    * @param moduleRegistrations The module registrations to use
    * @param allowEmpty          Whether this method should return 0 if no module registrations are found
    * @return The weighted mean mark, if all the provided registration has an agreed mark
    */
  def weightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean): Either[String, BigDecimal]

  /**
    * Like weightedMeanYearMark but only returns year marks calculated from agreed (post board) marks
    */
  def agreedWeightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean): Either[String, BigDecimal]

  def overcattedModuleSubsets(
    moduleRegistrations: Seq[ModuleRegistration],
    markOverrides: Map[Module, BigDecimal],
    normalLoad: BigDecimal,
    rules: Seq[UpstreamRouteRule]
  ): Seq[(BigDecimal, Seq[ModuleRegistration])]

  def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule]

  def findRegisteredUsercodes(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String]): Seq[String]

}

abstract class AbstractModuleRegistrationService extends ModuleRegistrationService {

  self: ModuleRegistrationDaoComponent =>

  def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit = moduleRegistrationDao.saveOrUpdate(moduleRegistration)

  def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit = moduleRegistrationDao.saveOrUpdate(coreRequiredModule)

  def delete(coreRequiredModule: CoreRequiredModule): Unit = moduleRegistrationDao.delete(coreRequiredModule)

  def getByNotionalKey(
    studentCourseDetails: StudentCourseDetails,
    module: Module,
    cats: JBigDecimal,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration] =
    moduleRegistrationDao.getByNotionalKey(studentCourseDetails, module, cats, academicYear, occurrence)

  def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByUsercodesAndYear(usercodes, academicYear)

  def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByModuleAndYear(module, academicYear)

  def getByYears(academicYears: Seq[AcademicYear]): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByYears(academicYears)

  def getByUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByUniversityIds(universityIds)

  private def calculateYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean)(marksFn: ModuleRegistration => Option[JBigDecimal]): Either[String, BigDecimal] = {
    val nonNullReplacedMarksAndCats: Seq[(BigDecimal, BigDecimal)] = moduleRegistrations.map(mr => {
      val mark: BigDecimal = markOverrides.getOrElse(mr.module, marksFn(mr).map(mark => BigDecimal(mark)).orNull)
      val cats: BigDecimal = Option(mr.cats).map(c => BigDecimal(c)).orNull
      (mark, cats)
    }).filter { case (mark, cats) => mark != null & cats != null }
    if (nonNullReplacedMarksAndCats.nonEmpty && nonNullReplacedMarksAndCats.size == moduleRegistrations.filterNot(_.passFail).size) {
      Right(
        (nonNullReplacedMarksAndCats.map { case (mark, cats) => mark * cats }.sum / nonNullReplacedMarksAndCats.map { case (_, cats) => cats }.sum)
          .setScale(1, RoundingMode.HALF_UP)
      )
    } else {
      if (nonNullReplacedMarksAndCats.isEmpty)
        if (allowEmpty)
          Right(BigDecimal(0))
        else
          Left(s"The year mark cannot be calculated because there are no module marks")
      else
        Left(s"The year mark cannot be calculated because the following module registrations have no mark: ${moduleRegistrations.filter(mr => !mr.passFail && mr.firstDefinedMark.isEmpty).map(_.module.code.toUpperCase).mkString(", ")}")
    }
  }

  def weightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean): Either[String, BigDecimal] =
    calculateYearMark(moduleRegistrations, markOverrides, allowEmpty) { mr => mr.firstDefinedMark }

  def agreedWeightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean): Either[String, BigDecimal] =
    calculateYearMark(moduleRegistrations, markOverrides, allowEmpty) { mr => Option(mr.agreedMark) }

  def overcattedModuleSubsets(
    moduleRegistrations: Seq[ModuleRegistration],
    markOverrides: Map[Module, BigDecimal],
    normalLoad: BigDecimal,
    rules: Seq[UpstreamRouteRule]
  ): Seq[(BigDecimal, Seq[ModuleRegistration])] = {
    val validRecords = moduleRegistrations.filterNot(_.deleted)
    if (validRecords.exists(_.firstDefinedMark.isEmpty)) {
        Seq((null, validRecords))
    } else {
      // TAB-6331 - Overcat subsets don't _have_ to contain all optional core modules. If a minimum number of optional core modules must be passed that should
      // be handled by pathway rules instead (which means as far as grids are concerned there is no difference between Optional and OptionalCore modules)
      val coreModules = validRecords.filter(mr =>
        mr.selectionStatus == ModuleSelectionStatus.Core
      )
      val subsets = validRecords.toSet.subsets.toSeq
      val validSubsets = subsets.filter(_.nonEmpty).filter(modRegs =>
        // CATS total of at least the normal load
        modRegs.toSeq.map(mr => BigDecimal(mr.cats)).sum >= normalLoad &&
          // Contains all the core modules
          coreModules.forall(modRegs.contains) &&
          // All the registrations have agreed or actual marks
          modRegs.forall(mr => mr.firstDefinedMark.isDefined || markOverrides.get(mr.module).isDefined && markOverrides(mr.module) != null)
      )
      val ruleFilteredSubsets = validSubsets.filter(modRegs => rules.forall(_.passes(modRegs.toSeq)))
      val subsetsToReturn = {
        if (ruleFilteredSubsets.isEmpty) {
          // Something is wrong with the rules, as at the very least the the subset of all the modules should match,
          // so don't do the rule filtering
          validSubsets
        } else {
          ruleFilteredSubsets
        }
      }
      subsetsToReturn.map(modRegs => (weightedMeanYearMark(modRegs.toSeq, markOverrides, allowEmpty = false), modRegs.toSeq.sortBy(_.module.code)))
        .collect { case (Right(mark), modRegs) => (mark, modRegs) }
        .sortBy { case (mark, modRegs) =>
          // Add a definitive sort so subsets with the same mark always come out the same order
          (mark.doubleValue, modRegs.size, modRegs.map(_.module.code).mkString(","))
        }
        .reverse
    }
  }

  def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule] =
    moduleRegistrationDao.findCoreRequiredModules(route, academicYear, yearOfStudy)

  def findRegisteredUsercodes(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String]): Seq[String] =
    moduleRegistrationDao.findRegisteredUsercodes(module, academicYear, endDate, occurrence)

}

@Service("moduleRegistrationService")
class ModuleRegistrationServiceImpl
  extends AbstractModuleRegistrationService
    with AutowiringModuleRegistrationDaoComponent

trait ModuleRegistrationServiceComponent {
  def moduleRegistrationService: ModuleRegistrationService
}

trait AutowiringModuleRegistrationServiceComponent extends ModuleRegistrationServiceComponent {
  var moduleRegistrationService: ModuleRegistrationService = Wire[ModuleRegistrationService]
}
