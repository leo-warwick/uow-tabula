package uk.ac.warwick.tabula.exams.grids.data

import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Course, CourseYearWeighting, UpstreamRouteRuleLookup}
import uk.ac.warwick.tabula.exams.grids.columns.{ChosenYearExamGridColumn, PerYearExamGridColumn}
import uk.ac.warwick.tabula.services.exams.grids.NormalLoadLookup

case class GridData(
	entities: Seq[ExamGridEntity],
	studentInformationColumns: Seq[ChosenYearExamGridColumn],
	perYearColumns: Map[YearOfStudy, Seq[PerYearExamGridColumn]],
	summaryColumns: Seq[ChosenYearExamGridColumn],
	weightings: Map[Course, Seq[CourseYearWeighting]],
	normalLoadLookup: NormalLoadLookup,
	routeRulesLookup: UpstreamRouteRuleLookup
)
