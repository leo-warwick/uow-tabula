package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.exams.grids.ExamGridEntity
import uk.ac.warwick.tabula.commands.marks.CohortCommand._
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.services.AssessmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.marks.ModuleRegistrationMarksServiceComponent

import scala.collection.SortedMap

object CohortCommand {
  type SprCode = String
  type ModuleCode = String
  type Occurrence = String
  type Sequence = String
}

trait CohortState {
  var entities: Seq[ExamGridEntity] = _
}

trait CohortModuleMarksRecords {

  self: CohortState with ModuleRegistrationMarksServiceComponent with AssessmentMembershipServiceComponent =>

  val academicYear: AcademicYear

  lazy val entitiesBySprCode: SortedMap[SprCode, (ExamGridEntity, Seq[ModuleRegistration])] = SortedMap(entities.flatMap { e =>
    val entityYear = e.validYears.lastOption.map(_._2)
    entityYear.map { ey =>
      ey.studentCourseYearDetails.map(_.studentCourseDetails.sprCode).orNull -> (e, ey.moduleRegistrations)
    }
  }.sortBy(_._1): _*)

  lazy val moduleRegistrations: Seq[ModuleRegistration] = entitiesBySprCode.values.flatMap(_._2).toSeq

  lazy val studentModuleMarkRecords: Map[ModuleCode, Map[Occurrence, Seq[StudentModuleMarkRecord]]] = moduleRegistrations
    .groupBy(_.sitsModuleCode)
    .view.mapValues(_.groupBy(_.occurrence))
    .toMap
    .map { case (moduleCode, occurrences) => moduleCode ->
      occurrences.map { case (occ, mr) => occ ->
        MarksDepartmentHomeCommand.studentModuleMarkRecords(moduleCode, academicYear, occ, mr, moduleRegistrationMarksService, assessmentMembershipService)
      }
    }


}
