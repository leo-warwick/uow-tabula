package uk.ac.warwick.tabula.services

import org.joda.time.LocalDate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.ModuleResult.Pass
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringModuleRegistrationDaoComponent, AutowiringTransactionalComponent, ModuleRegistrationDaoComponent, TransactionalComponent}
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

import scala.math.BigDecimal.RoundingMode

case class ComponentAndMarks(assessmentComponent: Option[AssessmentComponent], member: UpstreamAssessmentGroupMember, cats:BigDecimal)

trait ModuleRegistrationService {

  def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit

  def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit

  def delete(coreRequiredModule: CoreRequiredModule): Unit

  def getByNotionalKey(
    studentCourseDetails: StudentCourseDetails,
    sitsModuleCode: String,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration]

  def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByDepartmentAndYear(department: Department, academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByModuleOccurrence(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[ModuleRegistration]

  def getByYears(academicYears: Seq[AcademicYear], includeDeleted: Boolean): Seq[ModuleRegistration]

  def getByUniversityIds(universityIds: Seq[String], includeDeleted: Boolean): Seq[ModuleRegistration]

  def getByRecordedAssessmentComponentStudentsNeedsWritingToSits(students: Seq[RecordedAssessmentComponentStudent]): Map[RecordedAssessmentComponentStudent, Seq[ModuleRegistration]]

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

  def benchmarkComponentsAndMarks(moduleRegistration: ModuleRegistration): Seq[ComponentAndMarks]

  def componentsAndMarksExcludedFromBenchmark(moduleRegistration: ModuleRegistration): Seq[ComponentAndMarks]

  def percentageOfAssessmentTaken(moduleRegistrations: Seq[ModuleRegistration], normalLoad: BigDecimal): BigDecimal

  def benchmarkWeightedAssessmentMark(moduleRegistrations: Seq[ModuleRegistration]): BigDecimal

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

  def findRegisteredUsercodes(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String], includeUniversityIds: Boolean): Seq[String]

}

abstract class AbstractModuleRegistrationService extends ModuleRegistrationService {
  self: ModuleRegistrationDaoComponent
    with TransactionalComponent =>

  def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit = moduleRegistrationDao.saveOrUpdate(moduleRegistration)

  def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit = moduleRegistrationDao.saveOrUpdate(coreRequiredModule)

  def delete(coreRequiredModule: CoreRequiredModule): Unit = moduleRegistrationDao.delete(coreRequiredModule)

  def getByNotionalKey(
    studentCourseDetails: StudentCourseDetails,
    sitsModuleCode: String,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration] =
    moduleRegistrationDao.getByNotionalKey(studentCourseDetails, sitsModuleCode, academicYear, occurrence)

  override def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByUsercodesAndYear(usercodes, academicYear)

  override def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByModuleAndYear(module, academicYear)

  override def getByModuleOccurrence(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[ModuleRegistration] = transactional(readOnly = true) {
    moduleRegistrationDao.getByModuleOccurrence(sitsModuleCode, academicYear, occurrence)
  }

  override def getByDepartmentAndYear(department: Department, academicYear: AcademicYear): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByDepartmentAndYear(department, academicYear)

  override def getByYears(academicYears: Seq[AcademicYear], includeDeleted: Boolean): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByYears(academicYears, includeDeleted)

  override def getByUniversityIds(universityIds: Seq[String], includeDeleted: Boolean): Seq[ModuleRegistration] =
    moduleRegistrationDao.getByUniversityIds(universityIds, includeDeleted)

  override def getByRecordedAssessmentComponentStudentsNeedsWritingToSits(students: Seq[RecordedAssessmentComponentStudent]): Map[RecordedAssessmentComponentStudent, Seq[ModuleRegistration]] = transactional(readOnly = true) {
    val allModuleRegistrations =
      moduleRegistrationDao.getByRecordedAssessmentComponentStudentsNeedsWritingToSits
        .groupBy(mr => (SprCode.getUniversityId(mr.sprCode), mr.sitsModuleCode, mr.academicYear, mr.occurrence))

    students.map { student =>
      student -> allModuleRegistrations.getOrElse((student.universityId, student.moduleCode, student.academicYear, student.occurrence), Seq.empty)
    }.toMap
  }

  private def calculateYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean)(marksFn: ModuleRegistration => Option[Int], gradeFn: ModuleRegistration => Option[String]): Either[String, BigDecimal] = {
    val nonNullReplacedMarksAndCats: Seq[(BigDecimal, BigDecimal)] = moduleRegistrations.map(mr => {
      val mark: BigDecimal = markOverrides.getOrElse(mr.module, marksFn(mr).map(mark => BigDecimal(mark)).orNull)
      val cats: BigDecimal = mr.safeCats.orNull
      (mark, cats)
    }).filter { case (mark, cats) => mark != null && cats != null && cats > 0 }
    if (nonNullReplacedMarksAndCats.nonEmpty && nonNullReplacedMarksAndCats.size == moduleRegistrations.filterNot(mr => mr.passFail || gradeFn(mr).contains(GradeBoundary.ForceMajeureMissingComponentGrade) || mr.safeCats.contains(0)).size) {
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
        Left(s"The year mark cannot be calculated because the following module registrations have no mark: ${moduleRegistrations.filter(mr => !mr.passFail && marksFn(mr).isEmpty && !gradeFn(mr).contains(GradeBoundary.ForceMajeureMissingComponentGrade)).map(_.module.code.toUpperCase).mkString(", ")}")
    }
  }

  def weightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean): Either[String, BigDecimal] =
    calculateYearMark(moduleRegistrations, markOverrides, allowEmpty)(_.firstDefinedMark, _.firstDefinedGrade)

  def agreedWeightedMeanYearMark(moduleRegistrations: Seq[ModuleRegistration], markOverrides: Map[Module, BigDecimal], allowEmpty: Boolean): Either[String, BigDecimal] =
    calculateYearMark(moduleRegistrations, markOverrides, allowEmpty)(_.agreedMark, _.agreedGrade)

  def benchmarkComponentsAndMarks(moduleRegistration: ModuleRegistration): Seq[ComponentAndMarks] = {
    getComponentsAndMarks(moduleRegistration.componentsForBenchmark, moduleRegistration)
  }

  def componentsAndMarksExcludedFromBenchmark(moduleRegistration: ModuleRegistration): Seq[ComponentAndMarks] = {
    getComponentsAndMarks(moduleRegistration.componentsIgnoredForBenchmark, moduleRegistration)
  }

  private def getComponentsAndMarks(components: Seq[UpstreamAssessmentGroupMember], moduleRegistration: ModuleRegistration) = {
    // We need to get marks for _all_ components for the Module Registration in order to calculate a VAW weighting
    lazy val marks: Seq[(AssessmentType, String, Option[Int])] = moduleRegistration.componentMarks(includeActualMarks = true)

    val hasResitWeightings: Boolean = components.exists(_.resitAssessmentWeighting.nonEmpty)

    val totalRawWeighting: Int =
      if (hasResitWeightings)
        components.map { uagm =>
          uagm.resitAssessmentWeighting
            .orElse(uagm.upstreamAssessmentGroup.assessmentComponent.flatMap(ac => Option(ac.rawWeighting).map(_.toInt)))
            .getOrElse(0)
        }.sum
      else 100

    def scaleWeighting(raw: Int): BigDecimal =
      if (raw == 0 || totalRawWeighting == 0) BigDecimal(0) // 0 will always scale to 0 and a total of 0 will always lead to a weighting of 0
      else if (totalRawWeighting == 100) BigDecimal(raw)
      else {
        val bd = BigDecimal(raw * 100) / BigDecimal(totalRawWeighting)
        bd.setScale(1, BigDecimal.RoundingMode.HALF_UP)
        bd
      }

    components.map { uagm =>
      val weighting: BigDecimal =
        if (hasResitWeightings) {
          scaleWeighting(
            uagm.resitAssessmentWeighting
              .orElse(uagm.upstreamAssessmentGroup.assessmentComponent.flatMap(ac => Option(ac.rawWeighting).map(_.toInt)))
              .getOrElse(0)
          )
        } else {
          uagm.upstreamAssessmentGroup.assessmentComponent
            .flatMap(ac => ac.weightingFor(marks).getOrElse(ac.scaledWeighting))
            .getOrElse(BigDecimal(0))
        }



      val cats = (weighting / 100) * moduleRegistration.safeCats.getOrElse(BigDecimal(0))
      ComponentAndMarks(uagm.upstreamAssessmentGroup.assessmentComponent, uagm, cats)
    }
  }

  def percentageOfAssessmentTaken(moduleRegistrations: Seq[ModuleRegistration], normalLoad: BigDecimal): BigDecimal = {
    val completedCats = moduleRegistrations.flatMap(benchmarkComponentsAndMarks).map(_.cats).sum
    if (normalLoad == 0) BigDecimal(0) else (completedCats / normalLoad) * 100
  }

  def benchmarkWeightedAssessmentMark(moduleRegistrations: Seq[ModuleRegistration]): BigDecimal = {
    val marksForBenchmark = moduleRegistrations.map(mr => mr -> benchmarkComponentsAndMarks(mr)).toMap

    val cats = marksForBenchmark.values.flatten.map(_.cats).sum

    val benchmark = if(cats == 0) BigDecimal(0) else {
      marksForBenchmark.values.flatten.map { componentAndMarks =>
        val mark = componentAndMarks.member.firstDefinedMark.getOrElse(throw new IllegalStateException("Components without marks shouldn't be used to calculate the benchmark"))
        mark * componentAndMarks.cats
      }.sum / marksForBenchmark.values.flatten.map(_.cats).sum
    }

    benchmark.setScale(1, RoundingMode.HALF_UP)
  }

  def overcattedModuleSubsets(
    moduleRegistrations: Seq[ModuleRegistration],
    markOverrides: Map[Module, BigDecimal],
    normalLoad: BigDecimal,
    rules: Seq[UpstreamRouteRule]
  ): Seq[(BigDecimal, Seq[ModuleRegistration])] = {
    val validRecords = moduleRegistrations
      .filterNot(_.deleted)
      .filterNot(mr => mr.moduleResult == Pass && mr.safeCats.contains(0)) // 0 CAT modules don't count towards the overall mark so ignore them
      .filterNot(mr => mr.firstDefinedGrade.contains(GradeBoundary.WithdrawnGrade)) // Remove withdrawn modules

    if (validRecords.exists(mr => mr.firstDefinedMark.isEmpty && !mr.firstDefinedGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))) {
      Seq((null, validRecords))
    } else {
      // TAB-6331 - Overcat subsets don't _have_ to contain all optional core modules. If a minimum number of optional core modules must be passed that should
      // be handled by pathway rules instead (which means as far as grids are concerned there is no difference between Optional and OptionalCore modules)
      val coreModules = validRecords.filter(mr =>
        mr.selectionStatus == ModuleSelectionStatus.Core
      )
      val subsets = validRecords.toSet.subsets().toSeq
      val validSubsets = subsets.filter(_.nonEmpty).filter { modRegs =>
        val (forceMajeureModRegs, modRegsWithoutForceMajeure) = modRegs.partition(_.firstDefinedGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))
        val forceMajeureCats = forceMajeureModRegs.toSeq.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum

        // CATS total of at least the normal load, less any FM module weightings
        modRegsWithoutForceMajeure.toSeq.map(mr => mr.safeCats.getOrElse(BigDecimal(0))).sum >= (normalLoad - forceMajeureCats) &&
        // Contains all the core modules (including if FM)
        coreModules.forall(modRegs.contains) &&
        // All the registrations have agreed or actual marks
        modRegsWithoutForceMajeure.forall(mr => mr.firstDefinedMark.isDefined || markOverrides.contains(mr.module) && markOverrides(mr.module) != null)
      }
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

      // Explicitly specify how to order doubles
      import Ordering.Double.TotalOrdering
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

  def findRegisteredUsercodes(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String], includeUniversityIds: Boolean = false): Seq[String] =
    moduleRegistrationDao.findRegisteredUsers(module, academicYear, endDate, occurrence, includeUniversityIds)

}

@Service("moduleRegistrationService")
class ModuleRegistrationServiceImpl
  extends AbstractModuleRegistrationService
    with AutowiringModuleRegistrationDaoComponent
    with AutowiringTransactionalComponent

trait ModuleRegistrationServiceComponent {
  def moduleRegistrationService: ModuleRegistrationService
}

trait AutowiringModuleRegistrationServiceComponent extends ModuleRegistrationServiceComponent {
  var moduleRegistrationService: ModuleRegistrationService = Wire[ModuleRegistrationService]
}
