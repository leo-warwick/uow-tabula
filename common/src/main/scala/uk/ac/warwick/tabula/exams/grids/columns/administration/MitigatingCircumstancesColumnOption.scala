package uk.ac.warwick.tabula.exams.grids.columns.administration

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.mitcircs.{MitCircsExamBoardRecommendation, MitigatingCircumstancesAffectedAssessment, MitigatingCircumstancesGrading, MitigatingCircumstancesSubmission}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.helpers.SeqUtils._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.mitcircs.AutowiringMitCircsSubmissionServiceComponent

import scala.jdk.CollectionConverters._

@Component
class MitigatingCircumstancesColumnOption extends ChosenYearExamGridColumnOption with AutowiringMitCircsSubmissionServiceComponent {

  override val identifier: ExamGridColumnOption.Identifier = "mitigating"

  override val label: String = "Administration: Mitigating Circumstances"

  override val sortOrder: Int = ExamGridColumnOption.SortOrders.MitigatingCircumstances

  case class Column(state: ExamGridColumnState) extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

    override val title: String = "Mitigating Circumstances"

    override val category: String = "Administration"

    override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.VeryLongString

    override lazy val result: Map[ExamGridEntity, ExamGridColumnValue] = {

      state.entities.map(entity => entity -> {

        def affectedAssessmentsModuleCodes(assessments: Seq[MitigatingCircumstancesAffectedAssessment]): Seq[String] = assessments.map { assessment =>
          assessment.module.map(_.code.toUpperCase).getOrElse(assessment.moduleCode)
        }.distinct

        def globalRecommendations(s: MitigatingCircumstancesSubmission, html: Boolean): Seq[String] = s.globalRecommendations
          .map{ r => r -> s.affectedAssessments.asScala.toSeq }
          .map{ case (r, a) =>
            "%s (all assessments - %s)%s".format(
              r.description,
              affectedAssessmentsModuleCodes(a).mkString(", "),
              r match {
                case MitCircsExamBoardRecommendation.Other if s.boardRecommendationOther.maybeText.nonEmpty =>
                  if (html) "<br /><span class=\"very-subtle\">%s</span>".format(s.boardRecommendationOther)
                  else "\n(%s)".format(s.boardRecommendationOther)

                case _ => ""
              }
            )
          }

        def affectedAssessmentsByRecommendation(s: MitigatingCircumstancesSubmission): Seq[String] = s.affectedAssessmentsByRecommendation.toSeq
          .map{case (r, a) => s"${r.description} ${affectedAssessmentsModuleCodes(a).mkStringOrEmpty("(", ", ", ")")}"}

        def modulesWithAcuteOutcomes(s: MitigatingCircumstancesSubmission): Seq[String] = s.assessmentsWithAcuteOutcome
          .map(aa => aa.module.map(_.code.toUpperCase).getOrElse(aa.moduleCode)).distinct

        val mitCircsCodesString = entity.mitigatingCircumstances.map(s => {
          val header = s"MIT-${s.key} Graded ${s.gradingCode.getOrElse("")} - (graded ${DateFormats.CSVDate.print(s.outcomesFinalisedOn)})\n"
          val comments = Option(s.boardRecommendationComments).toSeq.mkStringOrEmpty("", "\n", "\n")
          if (s.outcomeGrading.entryName != "Rejected") {
            val global = globalRecommendations(s, html = false).mkStringOrEmpty("", "\n", "\n")
            val affected = affectedAssessmentsByRecommendation(s).mkStringOrEmpty("", "\n", "\n")
            val acute = modulesWithAcuteOutcomes(s).mkStringOrEmpty(s"${Option(s.acuteOutcome).map(_.description).getOrElse("")} (", ", ", ")\n")

            header ++ global ++ affected ++ acute ++ comments
          } else {
            header
          }
        }).mkString("\n")

        val mitCircsHtml = entity.mitigatingCircumstances.map(s => s"""<dl class="dl-horizontal">
          <dt>MIT-${s.key}<dt><dd>Graded ${s.gradingCode.getOrElse("")} - (graded ${DateFormats.CSVDate.print(s.outcomesFinalisedOn)})<dd>
          ${if(s.outcomeGrading != MitigatingCircumstancesGrading.Rejected) {
          s"""<dt>Outcome</dt>
            ${globalRecommendations(s, html = true).mkStringOrEmpty("<dd>", "</dd><dd>", "</dd>")}
            ${affectedAssessmentsByRecommendation(s).mkStringOrEmpty("<dd>", "</dd><dd>", "</dd>")}
            ${modulesWithAcuteOutcomes(s).mkStringOrEmpty(s"<dd>${Option(s.acuteOutcome).map(_.description).getOrElse("")} (", ", ", ")</dd>")}
            ${Option(s.boardRecommendationComments).map(comments => s"<dt>Comments</dt><dd>$comments</dd>").getOrElse("")}
            """} else ""}
          </dl>""").mkString

        val notes = if (state.department.rootDepartment.code == "es") {
          entity.validYears.headOption
            .flatMap { case (_, year) => year.studentCourseYearDetails }
            .map(notes => s"\n${notes.studentCourseDetails.notes.asScala.mkString(", ")}")
        } else {
          None
        }

        ExamGridColumnValueStringWithHtml(
          value = s"$mitCircsCodesString${notes.getOrElse("")}",
          html = s"$mitCircsHtml${notes.map(n => s"<span>$n</span>").getOrElse("")}"
        )
      }).toMap
    }

  }

  override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
