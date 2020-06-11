package uk.ac.warwick.tabula.exams.grids.columns.modules

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntityYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumnValue, ExamGridColumnValueType, _}
import uk.ac.warwick.tabula.services.AutowiringAssessmentMembershipServiceComponent

import scala.collection.mutable
import scala.math.BigDecimal.RoundingMode

object ModuleExamGridColumn {

  case class SITSIndicator(grade: String, description: String)

  // grades with a special meaning in SITS
  final val SITSIndicators = Seq(
    SITSIndicator("AB", "Absent"),
    SITSIndicator("AM", "Academic Misconduct"),
    SITSIndicator("L", "Late or No submission"),
    SITSIndicator("M", "Mitigating circumstances"),
    SITSIndicator("PL", "Plagiarism"),
    SITSIndicator("RF", "Resit Fail"),
    SITSIndicator("S", "First Sit in September"),
    SITSIndicator("W", "Withdrawn"),
    SITSIndicator("R", "Forced Resit"),
    SITSIndicator("CP", "Compensated Pass"),
    SITSIndicator("N", "Resit refused/not taken"),
    SITSIndicator("QF", "Qualified Fail"),
    SITSIndicator("T", "Temporary Withdraw"),
    SITSIndicator("FM", "Force majeure")
  )
}

abstract class ModuleExamGridColumn(state: ExamGridColumnState, val module: Module, val moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal)
  extends PerYearExamGridColumn(state) with HasExamGridColumnCategory with HasExamGridColumnSecondaryValue with AutowiringAssessmentMembershipServiceComponent {

  def moduleSelectionStatus: Option[ModuleSelectionStatus]

  override val title: String = state.showModuleNames match {
    case ExamGridDisplayModuleNameColumnValue.LongNames => s"${module.code.toUpperCase} ${module.name}"
    case ExamGridDisplayModuleNameColumnValue.ShortNames =>
      if (module.shortName == null || module.shortName == module.code.toUpperCase) {
        s"${module.code.toUpperCase}"
      } else {
        s"${module.code.toUpperCase} ${module.shortName}"
      }
    case _ => s"${module.code.toUpperCase}"
  }

  override val boldTitle: Boolean = isDuplicate

  override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.WholeMark

  val categoryShortForm: String

  private lazy val _values = mutable.Map[ExamGridEntityYear, ExamGridColumnValues]()

  private lazy val _assessmentComponents = assessmentMembershipService.getAssessmentComponents(module, inUseOnly = false)

  def result(entity: ExamGridEntityYear): ExamGridColumnValues = {
    _values.getOrElseUpdate(entity, {
      val values = getModuleRegistration(entity).map(mr => {
        val overallMark = {
          if (entity.markOverrides.exists(_.contains(module))) {
            ExamGridColumnValueOverrideDecimal(entity.markOverrides.get(module))
          } else {
            if (mr.passFail) {
              val (grade, isActual) = mr.agreedGrade match {
                case Some(grade: String) => (grade, false)
                case _ => mr.actualGrade.map(g => (g, true)).getOrElse(null, false)
              }
              if (grade == null) {
                ExamGridColumnValueMissing("Agreed and actual grade missing")
              } else {
                ExamGridColumnValueString(grade, isActual, isFail = grade == "F")
              }
            } else {
              val (mark, isActual) = mr.agreedMark match {
                case Some(mark: Int) => (BigDecimal(mark), false)
                case _ => mr.actualMark.map(m => (BigDecimal(m), true)).getOrElse(null, false)
              }
              if (mark == null) {
                ExamGridColumnValueMissing("Agreed and actual mark missing")
              } else if (isActual && mr.actualGrade.contains("F") || mr.agreedGrade.contains("F")) {
                ExamGridColumnValueDecimal(mark, isActual, isFail = true)
              } else if (entity.studentCourseYearDetails.isDefined && entity.overcattingModules.exists(_.contains(mr.module))) {
                ExamGridColumnValueOvercatDecimal(mark, isActual)
              } else if (mr.firstDefinedGrade.exists(g => ModuleExamGridColumn.SITSIndicators.exists(_.grade == g))) {
                val indicator = ModuleExamGridColumn.SITSIndicators.find(_.grade == mr.firstDefinedGrade.get).get
                ExamGridColumnValueWithTooltip(s"${mr.firstDefinedGrade.get} ($mark)", isActual, indicator.description)
              } else {
                ExamGridColumnValueDecimal(mark, isActual)
              }
            }
          }
        }
        if (state.showComponentMarks) {
          def toValue(member: UpstreamAssessmentGroupMember, weighting: BigDecimal): ExamGridColumnValue = {
            val (mark, isActual) = (member.firstDefinedMark, !member.isAgreedMark)

            def markAsString: String = {
              val raw = mark.get.toString
              val checkResit = if (member.isResitMark) s"[$raw ${member.firstOriginalMark.map(s => s"($s)").getOrElse("")}]" else raw
              val checkShowSequence = if (state.showComponentSequence) s"$checkResit (${member.upstreamAssessmentGroup.sequence})" else checkResit
              checkShowSequence
            }

            if (mark.isEmpty) {
              ExamGridColumnValueMissing("Agreed and actual mark missing")
            } else {
              val sequence = member.upstreamAssessmentGroup.sequence
              val name = member.upstreamAssessmentGroup.assessmentComponent.map(_.name).getOrElse("")
              val tooltip = s"$sequence $name - $weighting%"
              val isFail = member.firstDefinedGrade.contains("F")
              ExamGridColumnValueWithTooltip(markAsString, isActual, tooltip, isFail)
            }
          }

          val assessmentComponents = {
            lazy val marks: Seq[(AssessmentType, String, Option[Int])] = mr.componentMarks(includeActualMarks = true)

            val allComponents = mr.upstreamAssessmentGroupMembers.filter(m => m.firstDefinedMark.isDefined).sortBy(_.upstreamAssessmentGroup.sequence)
            val componentsAndWeights = allComponents.map { uagm => uagm ->
              _assessmentComponents.find { component =>
                component.moduleCode == uagm.upstreamAssessmentGroup.moduleCode &&
                component.assessmentGroup == uagm.upstreamAssessmentGroup.assessmentGroup &&
                component.sequence == uagm.upstreamAssessmentGroup.sequence
              }.flatMap(ac => ac.weightingFor(marks))
            }.toMap.collect { case (key, Some(value)) => (key, value) }

            if (state.showZeroWeightedComponents)
              componentsAndWeights
            else {
              componentsAndWeights.filter(_._2 > 0)
            }
          }

          val (exams, assignments) = assessmentComponents.partition(_._1.upstreamAssessmentGroup.assessmentComponent.exists(_.assessmentType.subtype == TabulaAssessmentSubtype.Exam))

          ExamGridColumnValueType.toMap(
            overall = overallMark,
            assignments = assignments.map { case (uagm, w) => toValue(uagm, w) }.toSeq,
            exams = exams.map { case (uagm, w) => toValue(uagm, w) }.toSeq
          )
        } else {
          ExamGridColumnValueType.toMap(overallMark)
        }

      }).getOrElse(
        ExamGridColumnValueType.toMap(ExamGridColumnValueString(""))
      )

      val isEmpty = values.values.flatten.forall(_.isEmpty)
      ExamGridColumnValues(values, isEmpty)
    })
  }

  protected def scaled(bg: JBigDecimal): JBigDecimal =
    JBigDecimal(Option(bg).map(_.setScale(1, RoundingMode.HALF_UP)))

  private def getModuleRegistration(entity: ExamGridEntityYear): Option[ModuleRegistration] = {
    entity.moduleRegistrations.find(mr =>
      mr.module == module &&
      scaled(mr.cats) == scaled(cats) &&
      (moduleSelectionStatus.isEmpty || mr.selectionStatus == moduleSelectionStatus.get)
    )
  }

  override val secondaryValue: String = scaled(cats).toPlainString

}

abstract class ModuleExamGridColumnOption extends PerYearExamGridColumnOption {

  // isDuplicate is true if this module appears more than once in the grid (as a core and optional module for example)
  def Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn

  def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean

  override final def getColumns(state: ExamGridColumnState): Map[YearOfStudy, Seq[PerYearExamGridColumn]] = {

    val allYears = state.entities.flatMap(_.validYears.values).toList

    val allCoreRequiredModules =
      allYears.map(_.route).flatMap(state.coreRequiredModuleLookup.apply).map(cr => (cr.module, "CoreRequired"))

    val allOtherModules = allYears
      .flatMap(_.moduleRegistrations).map(mr => (mr.module, mr.selectionStatus))

    val duplicateModules = (allCoreRequiredModules ++ allOtherModules).distinct
      .groupBy { case (module, _) => module }
      // collect modules with 2 or more different selection statuses
      .collect { case (module, _ :: _ :: _) => module }
      .toSeq

    state.entities.flatMap(_.years.keys).distinct.map(academicYear => academicYear -> {
      val years = state.entities.flatMap(_.validYears.get(academicYear))

      val moduleInfo = years.flatMap { entityYear =>
        val coreRequiredModules = state.coreRequiredModuleLookup(entityYear.route).map(_.module)
        val registrations = entityYear.moduleRegistrations.filter(moduleRegistrationFilter(_, coreRequiredModules))
        registrations.map(r => (r, r.moduleList(entityYear.route)))
      } .groupBy { case (mr, _) => (mr.module, mr.cats)}
        .view.mapValues(_.headOption.flatMap { case (_, m) => m })
        .map { case ((module, cats), optionList) => (module, cats, optionList)}

      moduleInfo
        .toSeq
        .sortBy { case (module, cats, _) => (module, cats) }
        .map { case (module, cats, moduleList) => Column(state, module, moduleList, duplicateModules.contains(module), cats) }
    }).toMap
  }
}

abstract class OptionalModuleExamGridColumn(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal)
  extends ModuleExamGridColumn(state, module, moduleList, isDuplicate, cats) {

  // TAB-6883 - show the FMC that this module belongs to for stats
  override val secondaryValue: String = if(state.department.rootDepartment.code == "st") {
    scaled(cats).toPlainString + moduleList.map(ml => s" - ${ml.shortName}").getOrElse("")
  } else {
    scaled(cats).toPlainString
  }
}

@Component
class CoreModulesColumnOption extends ModuleExamGridColumnOption {

  override val identifier: ExamGridColumnOption.Identifier = "core"

  override val label: String = "Modules: Core Modules"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreModules

  class Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal)
    extends ModuleExamGridColumn(state, module, moduleList, isDuplicate, cats) {

    override val category: String = "Core Modules"
    override val categoryShortForm: String = "CM"

    override val moduleSelectionStatus: Option[ModuleSelectionStatus] = Option(ModuleSelectionStatus.Core)

  }

  override def Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
  = new Column(state, module, moduleList, isDuplicate, cats)

  override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
    mr.selectionStatus == ModuleSelectionStatus.Core && !coreRequiredModules.contains(mr.module)

}

@Component
class CoreRequiredModulesColumnOption extends ModuleExamGridColumnOption {

  override val identifier: ExamGridColumnOption.Identifier = "corerequired"

  override val label: String = "Modules: Core Required Modules"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreRequiredModules

  class Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal)
    extends ModuleExamGridColumn(state, module, moduleList, isDuplicate, cats) {

    override val category: String = "Core Required Modules"
    override val categoryShortForm: String = "CR"

    override val moduleSelectionStatus: Option[ModuleSelectionStatus] = None

  }

  override def Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
  = new Column(state, module, moduleList, isDuplicate, cats)

  override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
    coreRequiredModules.contains(mr.module)

}

@Component
class CoreOptionalModulesColumnOption extends ModuleExamGridColumnOption {

  override val identifier: ExamGridColumnOption.Identifier = "coreoptional"

  override val label: String = "Modules: Core Optional Modules"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.CoreOptionalModules

  class Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal)
    extends OptionalModuleExamGridColumn(state, module, moduleList, isDuplicate, cats) {

    override val category: String = "Core Optional Modules"
    override val categoryShortForm: String = "CO"

    override val moduleSelectionStatus: Option[ModuleSelectionStatus] = Option(ModuleSelectionStatus.OptionalCore)

  }

  override def Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
  = new Column(state, module, moduleList, isDuplicate, cats)

  override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
    mr.selectionStatus == ModuleSelectionStatus.OptionalCore && !coreRequiredModules.contains(mr.module)

}

@Component
class OptionalModulesColumnOption extends ModuleExamGridColumnOption {

  override val identifier: ExamGridColumnOption.Identifier = "optional"

  override val label: String = "Modules: Optional Modules"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.OptionalModules

  class Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal)
    extends OptionalModuleExamGridColumn(state, module, moduleList, isDuplicate, cats) {

    override val category: String = "Optional Modules"
    override val categoryShortForm: String = "OM"

    override val moduleSelectionStatus: Option[ModuleSelectionStatus] = Option(ModuleSelectionStatus.Option)

  }

  override def Column(state: ExamGridColumnState, module: Module, moduleList: Option[UpstreamModuleList], isDuplicate: Boolean, cats: JBigDecimal): ModuleExamGridColumn
  = new Column(state, module, moduleList, isDuplicate, cats)

  override def moduleRegistrationFilter(mr: ModuleRegistration, coreRequiredModules: Seq[Module]): Boolean =
    mr.selectionStatus == ModuleSelectionStatus.Option && !coreRequiredModules.contains(mr.module)

}
