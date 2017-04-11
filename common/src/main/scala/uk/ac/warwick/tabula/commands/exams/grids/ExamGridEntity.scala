package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Module, ModuleRegistration, Route, StudentCourseYearDetails}

case class ExamGridEntity(
	firstName: String,
	lastName: String,
	universityId: String,
	lastImportDate: Option[DateTime],
	years: Map[YearOfStudy, ExamGridEntityYear] // Int = year of study
)

case class ExamGridEntityYear(
	moduleRegistrations: Seq[ModuleRegistration],
	cats: BigDecimal,
	route: Route,
	overcattingModules: Option[Seq[Module]],
	markOverrides: Option[Map[Module, BigDecimal]],
	studentCourseYearDetails: Option[StudentCourseYearDetails]
)
