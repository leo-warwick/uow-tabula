package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

import scala.collection.JavaConverters._

abstract class AbstractAssignmentController extends CourseworkController with CurrentSITSAcademicYear {
	val createMode: String = "new"
	val editMode: String = "edit"

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters


	@ModelAttribute("academicYearChoices") def academicYearChoices: JList[AcademicYear] = {
		academicYear.yearsSurrounding(2, 2).asJava
	}
}
