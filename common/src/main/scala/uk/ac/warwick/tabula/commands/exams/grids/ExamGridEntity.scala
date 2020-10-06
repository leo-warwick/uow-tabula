package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesSubmission

case class ExamGridEntity(
  firstName: String,
  lastName: String,
  universityId: String,
  lastImportDate: Option[DateTime],
  years: Map[YearOfStudy, Option[ExamGridEntityYear]], // Int = year of study
  yearWeightings: Seq[CourseYearWeighting],
  mitigatingCircumstances: Seq[MitigatingCircumstancesSubmission] = Nil
) {
  def fullName = s"$firstName $lastName"
  def validYears: Map[YearOfStudy, ExamGridEntityYear] = years.filter { case (_, year) => year.nonEmpty }.view.mapValues(_.get).toMap
}

case class ExamGridEntityYear(
  moduleRegistrations: Seq[ModuleRegistration],
  cats: BigDecimal,
  route: Route,
  baseAcademicYear: AcademicYear, // This is used to look for UpstreamRouteRules so is tied to the route used
  overcattingModules: Option[Seq[Module]],
  markOverrides: Option[Map[Module, BigDecimal]],
  studentCourseYearDetails: Option[StudentCourseYearDetails],
  agreedMark: Option[BigDecimal],
  yearAbroad: Boolean,
  level: Option[Level],
  yearOfStudy: YearOfStudy, // a pointer back to this ExamGridEntityYears key in the parent ExamGridEntity.years map
)
