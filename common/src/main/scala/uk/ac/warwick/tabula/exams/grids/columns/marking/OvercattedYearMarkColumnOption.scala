package uk.ac.warwick.tabula.exams.grids.columns.marking

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.commands.exams.grids.{ExamGridEntity, ExamGridEntityYear}
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.services.AutowiringModuleRegistrationServiceComponent

@Component
class OvercattedYearMarkColumnOption extends ChosenYearExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "overcatted"

	override val label: String = "Marking: Overcatted year mark"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.OvercattedYearMark

	case class Column(state: ExamGridColumnState)	extends ChosenYearExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Over Catted Mark"

		override val category: String = s"Year ${state.yearOfStudy} Marks"

		override val excelColumnWidth: Int = ExamGridColumnOption.ExcelColumnSizes.Decimal

		override def values: Map[ExamGridEntity, ExamGridColumnValue] = {
			state.entities.map(entity =>
				entity -> entity.years.get(state.yearOfStudy).map(entityYear => result(entityYear) match {
					case Right(mark) => ExamGridColumnValueDecimal(mark)
					case Left(message) => ExamGridColumnValueMissing(message)
				}).getOrElse(ExamGridColumnValueMissing(s"Could not find course details for ${entity.universityId} for ${state.academicYear}"))
			).toMap
		}

		private def result(entity: ExamGridEntityYear): Either[String, BigDecimal] = {
			// If the entity isn't based on an SCYD i.e. when we're showing the overcatting options, just show the mean mark for this student
			if (entity.studentCourseYearDetails.isEmpty) {
				moduleRegistrationService.weightedMeanYearMark(entity.moduleRegistrations, entity.markOverrides.getOrElse(Map()))
			} else {
				val overcatSubsets = state.overcatSubsets(entity)
				if (overcatSubsets.size == 1) {
					// If the student only has one valid subset, just show the mark for that subset
					Right(overcatSubsets.head._1)
				} else if (entity.overcattingModules.isDefined) {
					// If the student has overcatted and a subset of modules has been chosen for the overcatted mark,
					// find the subset that matches those modules, and show that mark if found
					overcatSubsets.find { case (_, subset) => subset.size == entity.overcattingModules.get.size && subset.map(_.module).forall(entity.overcattingModules.get.contains) } match {
						case Some((mark, subset)) => Right(mark)
						case _ => Left("Could not find valid module registration subset matching chosen subset")
					}
				} else {
					Left("The overcat adjusted mark subset has not been chosen")
				}
			}
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ChosenYearExamGridColumn] = Seq(Column(state))

}
