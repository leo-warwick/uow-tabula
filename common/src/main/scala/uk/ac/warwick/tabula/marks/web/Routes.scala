package uk.ac.warwick.tabula.marks.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, Department, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
  * Generates URLs to various locations, to reduce the number of places where URLs
  * are hardcoded and repeated.
  *
  * For methods called "apply", you can leave out the "apply" and treat the object like a function.
  */
object Routes {

  import RoutesUtils._

  private val context = "/marks"

  def home: String = context + "/"

  object Admin {
    def home(department: Department): String = s"$context/admin/${encoded(department.code)}"
    def home(department: Department, academicYear: AcademicYear): String = s"$context/admin/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}"
    def outOfSyncMarks(department: Department, academicYear: AcademicYear): String = s"$context/admin/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/out-of-sync-marks"

    // actions that apply to a group of students
    object Cohorts {
      def apply(department: Department, academicYear: AcademicYear): String = s"$context/admin/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/cohort"
      def processMarks(department: Department, academicYear: AcademicYear): String = s"${apply(department, academicYear)}/process"
      def examBoardOutcomes(department: Department, academicYear: AcademicYear): String = s"${apply(department, academicYear)}/outcomes"
    }

    object AssessmentComponents {
      def apply(department: Department, academicYear: AcademicYear): String = s"$context/admin/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/assessment-components"
      def recordMarks(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): String = s"$context/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/marks"
      def missingMarks(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): String = s"$context/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/missing-marks"
      def scaling(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup): String = s"$context/admin/assessment-component/${assessmentComponent.id}/${upstreamAssessmentGroup.id}/scaling"
    }

    object ModuleOccurrences {
      def apply(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): String = s"$context/admin/module/${encoded(sitsModuleCode)}/${encoded(academicYear.startYear.toString)}/${encoded(occurrence)}"
      def recordMarks(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): String = s"${apply(sitsModuleCode, academicYear, occurrence)}/marks"
      def confirmMarks(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): String = s"${apply(sitsModuleCode, academicYear, occurrence)}/confirm"
      def processMarks(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): String = s"${apply(sitsModuleCode, academicYear, occurrence)}/process"
    }
  }
}
