package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object GenerateExamGridOvercatCommand {
  def overcatIdentifier(seq: Seq[_]): String = seq.map {
    case e: ModuleRegistration => e.module.code
    case e: Module => e.code
    case _ => throw new IllegalArgumentException
  }.mkString("-")

  def apply(
    department: Department,
    academicYear: AcademicYear,
    scyd: StudentCourseYearDetails,
    normalLoadLookup: NormalLoadLookup,
    routeRules: Seq[UpstreamRouteRule],
    user: CurrentUser,
    basedOnLevel: Boolean
  ) = new GenerateExamGridOvercatCommandInternal(department, academicYear, scyd, normalLoadLookup, routeRules, user, basedOnLevel)
    with ComposableCommand[Seq[Module]]
    with AutowiringStudentCourseYearDetailsDaoComponent
    with AutowiringModuleRegistrationServiceComponent
    with PopulateGenerateExamGridOvercatCommand
    with GenerateExamGridOvercatValidation
    with GenerateExamGridOvercatDescription
    with GenerateExamGridOvercatPermissions
    with GenerateExamGridOvercatCommandState
    with GenerateExamGridOvercatCommandRequest
}


class GenerateExamGridOvercatCommandInternal(
  val department: Department,
  val academicYear: AcademicYear,
  val scyd: StudentCourseYearDetails,
  val normalLoadLookup: NormalLoadLookup,
  val routeRules: Seq[UpstreamRouteRule],
  val user: CurrentUser,
  val basedOnLevel: Boolean
) extends CommandInternal[Seq[Module]] {

  self: GenerateExamGridOvercatCommandRequest with GenerateExamGridOvercatCommandState with StudentCourseYearDetailsDaoComponent =>

  override def applyInternal(): Seq[Module] = {
    val modules = chosenModuleSubset.get._2.map(_.module)

    allSCYDs.foreach(scyd => {
      scyd.overcattingModules = modules.filter(scyd.moduleRegistrations.map(_.module).contains)
      scyd.overcattingChosenBy = user.apparentUser
      scyd.overcattingChosenDate = DateTime.now
      studentCourseYearDetailsDao.saveOrUpdate(scyd)
    })
    modules
  }

}

trait PopulateGenerateExamGridOvercatCommand extends PopulateOnForm {

  self: GenerateExamGridOvercatCommandRequest with GenerateExamGridOvercatCommandState =>

  def populate(): Unit = {
    overcatChoice = scyd.overcattingModules.flatMap { overcattingModules =>
      val overcatId = GenerateExamGridOvercatCommand.overcatIdentifier(overcattingModules)
      val idSubsets = overcattedModuleSubsets.map { case (_, subset) => GenerateExamGridOvercatCommand.overcatIdentifier(subset) }
      idSubsets.find(_ == overcatId)
    }.orNull
  }
}

trait GenerateExamGridOvercatValidation extends SelfValidating {

  self: GenerateExamGridOvercatCommandRequest =>

  override def validate(errors: Errors) {
    if (chosenModuleSubset.isEmpty) {
      errors.reject("examGrid.overcatting.overcatChoice.invalid")
    }
  }

}

trait GenerateExamGridOvercatPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

  self: GenerateExamGridOvercatCommandState =>

  override def permissionsCheck(p: PermissionsChecking) {
    p.PermissionCheck(Permissions.Department.ExamGrids, department)
  }

}

trait GenerateExamGridOvercatDescription extends Describable[Seq[Module]] {

  self: GenerateExamGridOvercatCommandState with GenerateExamGridOvercatCommandRequest =>

  override lazy val eventName = "GenerateExamGridOvercat"

  override def describe(d: Description) {
    d.studentIds(Seq(scyd.studentCourseDetails.student.universityId))
    d.properties(Map(
      "studentCourseYearDetails" -> scyd.id,
      "modules" -> chosenModuleSubset.map { case (_, modules) => modules.map(_.module.id) }.getOrElse(Seq()),
      "mark" -> chosenModuleSubset.map { case (mark, _) => mark.toString }.getOrElse("")
    ))
  }
}

trait GenerateExamGridOvercatCommandState {

  self: GenerateExamGridOvercatCommandRequest with ModuleRegistrationServiceComponent =>

  def department: Department

  def academicYear: AcademicYear

  def scyd: StudentCourseYearDetails

  def normalLoadLookup: NormalLoadLookup

  def routeRules: Seq[UpstreamRouteRule]

  def user: CurrentUser

  def basedOnLevel: Boolean

  val allSCYDs: Seq[StudentCourseYearDetails] = if (basedOnLevel)
    scyd.studentCourseDetails.freshOrStaleStudentCourseYearDetails.filter(_.level == scyd.level).toSeq.sorted.takeWhile(_ != scyd) ++ Seq(scyd)
  else
    Seq(scyd)

  def examGridEntityYear: ExamGridEntityYear = allSCYDs match {
    case scyd :: Seq() => scyd.toExamGridEntityYear
    case scyds => StudentCourseYearDetails.toExamGridEntityYearGrouped(1, scyds: _*)
  }

  lazy val overcattedModuleSubsets: Seq[(BigDecimal, Seq[ModuleRegistration])] = moduleRegistrationService.overcattedModuleSubsets(
    examGridEntityYear.moduleRegistrations,
    overwrittenMarks,
    normalLoadLookup(examGridEntityYear.studentCourseYearDetails.get.studentCourseDetails.currentRoute),
    routeRules
  )

}

trait GenerateExamGridOvercatCommandRequest {

  self: GenerateExamGridOvercatCommandState =>

  var overcatChoice: String = _

  def chosenModuleSubset: Option[(BigDecimal, Seq[ModuleRegistration])] =
    Option(overcatChoice).flatMap(overcatChoiceString =>
      overcattedModuleSubsets.find { case (_, subset) =>
        overcatChoiceString == GenerateExamGridOvercatCommand.overcatIdentifier(subset)
      }
    )

  var newModuleMarks: JMap[Module, String] = LazyMaps.create { module: Module => null: String }.asJava

  def overwrittenMarks: Map[Module, BigDecimal] = newModuleMarks.asScala.map { case (module, markString) =>
    module -> markString.maybeText.map(mark => BigDecimal.apply(mark))
  }.filter(_._2.isDefined).mapValues(_.get).toMap

}
