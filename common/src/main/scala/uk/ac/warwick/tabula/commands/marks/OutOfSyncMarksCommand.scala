package uk.ac.warwick.tabula.commands.marks

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.marks.OutOfSyncMarksCommand._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.marks._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._

object OutOfSyncMarksCommand {
  type UniversityID = String
  type SprCode = String
  class AssessmentComponentItem(val sitsModuleCode: String, val occurrence: String, val assessmentGroup: String, val sequence: String) {
    // Academic year will come from elsewhere
    var students: JMap[String, StudentComponentMarkItem] =
      LazyMaps.create { id: String =>
        id.split("_", 2) match {
          case Array(universityID, resitSequence) => new StudentComponentMarkItem(universityID, resitSequence)
          case Array(universityID) => new StudentComponentMarkItem(universityID)
        }
      }.asJava

    val bindPath: String = s"$sitsModuleCode/$occurrence/$assessmentGroup/$sequence"
  }

  class StudentComponentMarkItem(val universityID: UniversityID, val resitSequence: String) {
    def this(universityID: UniversityID) {
      this(universityID, "")
    }

    var acceptChanges: Boolean = true

    val bindPath: String = s"${universityID}_${resitSequence.maybeText.getOrElse("")}"
  }

  class ModuleOccurrenceItem(val sitsModuleCode: String, val occurrence: String) {
    // Academic year will come from elsewhere

    var students: JMap[String, StudentModuleMarkItem] =
      LazyMaps.create { universityID: UniversityID =>
        new StudentModuleMarkItem(universityID)
      }.asJava

    val bindPath: String = s"$sitsModuleCode/$occurrence"
  }

  class StudentModuleMarkItem(val sprCode: SprCode) {
    var acceptChanges: Boolean = true

    val bindPath: String = sprCode
  }

  type Result = (Seq[RecordedAssessmentComponentStudent], Seq[RecordedModuleRegistration])
  type Command = Appliable[Result]
    with OutOfSyncMarksRequest
    with PopulateOnForm
    with ListModuleOccurrencesWithPermission

  def apply(department: Department, academicYear: AcademicYear, currentUser: CurrentUser): Command =
    new OutOfSyncMarksCommandInternal(department, academicYear, currentUser)
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringMarksWorkflowProgressServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringResitServiceComponent
      with AutowiringTransactionalComponent
      with ComposableCommand[Result]
      with OutOfSyncMarksRequest
      with OutOfSyncMarksPopulateOnForm
      with OutOfSyncMarksDescription
      with ListAssessmentComponentsModulesWithPermission
      with ListAssessmentComponentsPermissions

}

trait OutOfSyncMarksRequest {
  var moduleMarks: JMap[String, ModuleOccurrenceItem] =
    LazyMaps.create { path: String =>
      path.split("/", 2) match {
        case Array(sitsModuleCode, occurrence) => new ModuleOccurrenceItem(sitsModuleCode, occurrence)
      }
    }.asJava

  var componentMarks: JMap[String, AssessmentComponentItem] =
    LazyMaps.create { path: String =>
      path.split("/", 4) match {
        case Array(sitsModuleCode, occurrence, assessmentGroup, sequence) => new AssessmentComponentItem(sitsModuleCode, occurrence, assessmentGroup, sequence)
      }
    }.asJava
}

trait OutOfSyncMarksPopulateOnForm extends PopulateOnForm {
  self: ListAssessmentComponentsState
    with OutOfSyncMarksRequest
    with ListModuleOccurrencesWithPermission
    with ListAssessmentComponentsForModulesWithPermission =>

  override def populate(): Unit = {
    pendingComponentMarkChanges
      .flatMap(_._2)
      .flatMap(_.recordedStudent)
      .groupBy { student => (student.moduleCode, student.occurrence, student.assessmentGroup, student.sequence) }
      .foreach { case ((sitsModuleCode, occurrence, assessmentGroup, sequence), students) =>
        val assessmentComponentItem = new AssessmentComponentItem(sitsModuleCode, occurrence, assessmentGroup, sequence)

        students.foreach { student =>
          val studentComponentMarkItem = new StudentComponentMarkItem(student.universityId, student.resitSequence.getOrElse(""))
          assessmentComponentItem.students.put(studentComponentMarkItem.bindPath, studentComponentMarkItem)
        }

        componentMarks.put(assessmentComponentItem.bindPath, assessmentComponentItem)
      }

    pendingModuleMarkChanges
      .flatMap(_._2)
      .flatMap(_.recordedStudent)
      .groupBy { student => (student.sitsModuleCode, student.occurrence) }
      .foreach { case ((sitsModuleCode, occurrence), students) =>
        val moduleOccurrenceItem = new ModuleOccurrenceItem(sitsModuleCode, occurrence)

        students.foreach { student =>
          val studentModuleMarkItem = new StudentModuleMarkItem(student.sprCode)
          moduleOccurrenceItem.students.put(studentModuleMarkItem.bindPath, studentModuleMarkItem)
        }

        moduleMarks.put(moduleOccurrenceItem.bindPath, moduleOccurrenceItem)
      }
  }
}

abstract class OutOfSyncMarksCommandInternal(val department: Department, val academicYear: AcademicYear, val currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ListAssessmentComponentsState
    with ListModuleOccurrencesWithPermission
    with ListAssessmentComponentsForModulesWithPermission {
  self: OutOfSyncMarksRequest
    with AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent
    with ResitServiceComponent
    with MarksWorkflowProgressServiceComponent
    with ListAssessmentComponentsModulesWithPermission
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): (Seq[RecordedAssessmentComponentStudent], Seq[RecordedModuleRegistration]) = transactional() {
    val updatedComponentMarks =
      componentMarks.asScala.view.values
        .flatMap { component =>
          component.students.asScala.view.values
            .filter(_.acceptChanges)
            .flatMap { student =>
              pendingComponentMarkChanges.flatMap(_._2).find(_.recordedStudent.exists { racs =>
                racs.moduleCode == component.sitsModuleCode &&
                racs.occurrence == component.occurrence &&
                racs.assessmentGroup == component.assessmentGroup &&
                racs.sequence == component.sequence &&
                racs.universityId == student.universityID &&
                racs.resitSequence == student.resitSequence.maybeText
              })
            }
        }
        .map { studentMarkRecord =>
          val student = studentMarkRecord.recordedStudent.get // Guarded above
          val upstreamAssessmentGroupMember = studentMarkRecord.upstreamAssessmentGroupMember

          val upstreamMarkState =
            if (upstreamAssessmentGroupMember.agreedMark.isEmpty && upstreamAssessmentGroupMember.agreedGrade.isEmpty) MarkState.UnconfirmedActual
            else MarkState.Agreed

          student.addMark(
            uploader = currentUser.apparentUser,
            mark = upstreamAssessmentGroupMember.firstDefinedMark,
            grade = upstreamAssessmentGroupMember.firstDefinedGrade,
            source = RecordedAssessmentComponentStudentMarkSource.SyncFromSITS,
            markState = student.latestState match {
              // Don't revert ConfirmedActual to UnconfirmedActual
              case Some(MarkState.ConfirmedActual) if upstreamMarkState != MarkState.Agreed => MarkState.ConfirmedActual
              case _ => upstreamMarkState
            },
            comments = "Synchronised mark state from SITS",
          )

          assessmentComponentMarksService.saveOrUpdate(student)

          student
        }

    val updatedModuleMarks =
      moduleMarks.asScala.view.values
        .flatMap { moduleOccurrence =>
          moduleOccurrence.students.asScala.view.values
            .filter(_.acceptChanges)
            .flatMap { student =>
              pendingModuleMarkChanges.flatMap(_._2).find(_.recordedStudent.exists { rmr =>
                rmr.sitsModuleCode == moduleOccurrence.sitsModuleCode &&
                rmr.occurrence == moduleOccurrence.occurrence &&
                rmr.sprCode == student.sprCode
              })
            }
        }
        .map { studentModuleMarkRecord =>
          val student = studentModuleMarkRecord.recordedStudent.get // Guarded above
          val moduleRegistration = studentModuleMarkRecord.moduleRegistration

          val upstreamMarkState =
            if (moduleRegistration.agreedMark.isEmpty && moduleRegistration.agreedGrade.isEmpty) MarkState.UnconfirmedActual
            else MarkState.Agreed

          student.addMark(
            uploader = currentUser.apparentUser,
            mark = moduleRegistration.firstDefinedMark,
            grade = moduleRegistration.firstDefinedGrade,
            result = Option(moduleRegistration.moduleResult),
            source = RecordedModuleMarkSource.SyncFromSITS,
            markState = student.latestState match {
              // Don't revert ConfirmedActual to UnconfirmedActual
              case Some(MarkState.ConfirmedActual) if upstreamMarkState != MarkState.Agreed => MarkState.ConfirmedActual
              case _ => upstreamMarkState
            },
            comments = "Synchronised mark state from SITS",
          )

          moduleRegistrationMarksService.saveOrUpdate(student)

          student
        }

    (updatedComponentMarks.toSeq, updatedModuleMarks.toSeq)
  }
}

trait OutOfSyncMarksDescription extends Describable[Result] {
  self: ListAssessmentComponentsState =>

  override lazy val eventName: String = "OutOfSyncMarks"

  override def describe(d: Description): Unit =
    d.department(department)
     .property("academicYear" -> academicYear.toString)

  override def describeResult(d: Description, result: (Seq[RecordedAssessmentComponentStudent], Seq[RecordedModuleRegistration])): Unit =
    d.properties(
      "acceptedComponentMarkChanges" -> result._1.size,
      "acceptedModuleMarkChanges" -> result._2.size
    )
}
