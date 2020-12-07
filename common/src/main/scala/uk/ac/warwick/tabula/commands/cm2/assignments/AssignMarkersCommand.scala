package uk.ac.warwick.tabula.commands.cm2.assignments

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.CM2MarkingWorkflowService.{Allocations, Marker, Student}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2.docconversion.MarkerAllocationExtractor.{Error, ParsedRow}
import uk.ac.warwick.tabula.services.cm2.docconversion.{AutowiringMarkerAllocationExtractorComponent, MarkerAllocationExtractorComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._


object AssignMarkersCommand {

  def apply(assignment: Assignment) =
    new AssignMarkersCommandInternal(assignment)
      with ComposableCommand[Assignment]
      with AssignMarkersDragAndDropState
      with AssignMarkersValidation
      with AssignMarkersPermissions
      with AssignMarkersDescription
      with AutowiringUserLookupComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringFeedbackServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
}

object AssignMarkersBySpreadsheetCommand {

  def apply(assignment: Assignment) =
    new AssignMarkersCommandInternal(assignment)
      with ComposableCommand[Assignment]
      with AssignMarkersBySpreadsheetState
      with AssignMarkersBySpreadsheetBindListener
      with AssignMarkersValidation
      with AssignMarkersPermissions
      with AssignMarkersDescription
      with AutowiringUserLookupComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringMarkerAllocationExtractorComponent
      with AutowiringFeedbackServiceComponent
      with AutowiringAssessmentMembershipServiceComponent

  val AcceptedFileExtensions = Seq(".xlsx")
}

object AssignMarkersBySmallGroupsCommand {
  def apply(assignment: Assignment, groupMarkerAllocationSequence: Seq[GroupMarkerAllocation]) =
    new AssignMarkersCommandInternal(assignment)
      with ComposableCommand[Assignment]
      with AssignMarkersBySmallGroupsState
      with AssignMarkersValidation
      with AssignMarkersPermissions
      with AssignMarkersDescription
      with AutowiringUserLookupComponent
      with AutowiringCM2MarkingWorkflowServiceComponent
      with AutowiringFeedbackServiceComponent
      with AutowiringAssessmentMembershipServiceComponent {
      def groupMarkerAllocations: Seq[GroupMarkerAllocation] = groupMarkerAllocationSequence
    }
}

abstract class AssignMarkersCommandInternal(val assignment: Assignment) extends CommandInternal[Assignment] {

  this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssignMarkersState with FeedbackServiceComponent =>

  def applyInternal(): Assignment = {
    assignment.cm2MarkingWorkflow.allStages.foreach(stage => {
      val allocations = allocationMap.getOrElse(stage, Map())
      cm2MarkingWorkflowService.allocateMarkersForStage(assignment, stage, allocations)
    })

    // add anonymous marking IDs to each item of feedback - add regardless of setting in case anon marking is chosen later
    feedbackService.addAnonymousIds(assignment.allFeedback)
    assignment
  }
}

trait AssignMarkersBySpreadsheetBindListener extends BindListener {

  this: UserLookupComponent with CM2MarkingWorkflowServiceComponent with MarkerAllocationExtractorComponent
    with AssignMarkersBySpreadsheetState with ValidateConcurrentStages with ValidateSequentialStages with ValidateMarkerChanges=>

  override def onBind(result: BindingResult): Unit = transactional() {

    val fileNames = file.fileNames.map(_.toLowerCase)
    val invalidFiles = fileNames.filter(s => !AssignMarkersBySpreadsheetCommand.AcceptedFileExtensions.exists(s.endsWith))

    if (invalidFiles.nonEmpty) {
      if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), AssignMarkersBySpreadsheetCommand.AcceptedFileExtensions.mkString(", ")), "")
      else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), AssignMarkersBySpreadsheetCommand.AcceptedFileExtensions.mkString(", ")), "")
    }

    if (!result.hasErrors) {
      transactional() {
        result.pushNestedPath("file")
        file.onBind(result)
        result.popNestedPath()

        if (!file.attached.isEmpty) {

          val sheetData = markerAllocationExtractor
            .extractMarkersFromSpreadsheet(file.attached.asScala.head.asByteSource.openStream(), assignment.cm2MarkingWorkflow).map { case (stage, parsedRows) =>
            val studentWithMultipleMarkers: Set[ParsedRow] = parsedRows
              .filter(row => row.marker.nonEmpty && row.student.nonEmpty)
              .groupBy(_.student)
              .filter { case (_, rows) => rows.size > 1 }.values.flatten.toSet
            val newRows = parsedRows.map { r =>
              if (studentWithMultipleMarkers.contains(r)) {
                val newErrors = r.errors :+ Error("Student userCode", code = "markingWorkflow.student.noDupes", Array(stage))
                r.copy(errors = newErrors)
              } else {
                r
              }
            }
            stage -> newRows
          }

          def rowsToAllocations(rows: Seq[ParsedRow]): Allocations = rows
            .filter(_.errors.isEmpty)
            .groupBy(a => a.marker)
            .collect { case (Some(marker), r) => marker -> r.flatMap(_.student).toSet }

          rowsWithErrors = sheetData.flatMap { case (_, rows) => rows }.filter(_.errors.nonEmpty).toSeq

          _allocationMap = sheetData.flatMap { case (key, rows) =>
            if (assignment.cm2MarkingWorkflow.workflowType.rolesShareAllocations) {
              val stages = assignment.cm2MarkingWorkflow.allStages.filter(_.roleName == key)
              stages.map(s => s -> rowsToAllocations(rows))
            } else {
              Seq(MarkingWorkflowStage.fromCode(key, assignment.cm2MarkingWorkflow.workflowType) -> rowsToAllocations(rows))
            }
          }
          validateConcurrentStages(_allocationMap, result)
          validateSequentialStageMarkers(_allocationMap, result)
          validateChangedMarkerAllocations(_allocationMap, result)
          validateUnallocatedMarkerAllocations(_allocationMap, result)
        }
      }
    }
  }
}

trait AssignMarkersPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: AssignMarkersState =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    notDeleted(assignment)
    p.PermissionCheck(Permissions.Assignment.Update, assignment.module)
  }
}

trait ValidateConcurrentStages {
  self: SelfValidating =>

  def validateConcurrentStages(allocationMap: Map[MarkingWorkflowStage, Allocations], errors: Errors): Unit = {
    val noDupesAllowed = allocationMap.view.filterKeys(_.stageAllocation)
    val allocations = noDupesAllowed.values.toSeq

    val markers = allocations.flatMap(_.keys).toSet

    markers.foreach { marker =>
      val students = allocations.flatMap(_.getOrElse(marker, Set()).toSeq)
      val dupes = students.groupBy(identity).collect { case (k, v) if v.size > 1 => k }

      if (dupes.nonEmpty) {
        errors.reject("markingWorkflow.marker.noDupes", Array(marker.getFullName, dupes.map(_.getFullName).mkString(", ")), "")
      }
    }
  }
}

trait ValidateSequentialStages {
  self: SelfValidating with AssignMarkersState =>

  lazy val allocationPairs: Map[MarkingWorkflowStage, Seq[(Marker, Student)]] = allocationMap.map { case (stage, allocations) =>
    stage -> allocations.toSeq.flatMap { case (marker, students) => students.map(marker -> _) }
  }

  def validateSequentialStageMarkers(allocationMap: Map[MarkingWorkflowStage, Allocations], errors: Errors): Unit = {
    val unwiseAllocations: Iterable[(MarkingWorkflowStage, (Marker, Student))] = allocationMap.keys.flatMap { stage =>
      allocationPairs(stage).filter(pair =>
        stage.otherStagesInSequence
          .exists(otherStage => allocationPairs.getOrElse(otherStage, Nil).contains(pair))
      ).map(stage -> _)
    }.toSeq.distinct

    unwiseAllocations.groupBy(_._2).foreach { case ((marker, student), iterable) =>
      val stages = iterable.map(_._1).toSeq.distinct.sortBy(_.order)
      val stageNames = stages.map(_.description).mkString(if (stages.size == 2) " and " else ", ")

      allocationWarnings :+= s"${student.getFullName} is allocated to marker ${marker.getFullName} for ${stages.size} stages: $stageNames"
    }

    if (allocationWarnings.nonEmpty && !allowSameMarkerForSequentialStages) {
      errors.reject("markingWorkflow.marker.warnings")
    }
  }
}

trait ValidateMarkerChanges extends FetchMarkerAllocations {
  self: SelfValidating with AssignMarkersState with UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent=>

  def validateChangedMarkerAllocations(allocationMap: Map[MarkingWorkflowStage, Allocations], errors: Errors): Unit = {
    val changedMarkerAllocationsWithFinalisedFeedback: Iterable[(MarkingWorkflowStage, Marker, Student)] = for {
      (stage, allocations) <- allocationMap
      (newMarker, students) <- allocations
      student <- students
      markerFeedback <- assignment.findFeedback(student.getUserId).toSeq.flatMap(_.allMarkerFeedback)
        .filter(_.stage == stage).filter(_.marker != newMarker).filter(_.finalised)
    } yield {
      val currentMarker = markerFeedback.marker

      (stage, currentMarker, student)
    }

    // check if student  with finalised feedback has been moved to another marker for a particular workflow stage
    changedMarkerAllocationsWithFinalisedFeedback
      .groupBy { case (stage, marker, _) => (stage, marker) }
      .view.mapValues(_.map({ case (_, _, student) => student }))
      .foreach {
        case ((stage, marker), students) =>
          val args: Array[Object] = Array(stage.description.toLowerCase, marker.getFullName, students.map(_.getFullName).mkString(", "))

          errors.reject("markingWorkflow.markers.finalised", args, "")
      }
  }

  def validateUnallocatedMarkerAllocations(allocationMap: Map[MarkingWorkflowStage, Allocations], errors: Errors): Unit = {

    val unallocatedMarkerWithFinalisedFeedback: Iterable[(MarkingWorkflowStage, Marker, Student)] = {

      def isMarkerUnallocated(student: Student, allocations: Map[Marker, Set[Student]]): Boolean = !(allocations.values.flatten.toSet.contains(student))

      def workflowStageAllocations(stageAllocationName: String): Map[MarkingWorkflowStage, Allocations] = {
        val stageAllocations = allocationMap.filter(_._1.allocationName == stageAllocationName)
        if (stageAllocations.isEmpty) {
          Map(MarkingWorkflowStage.fromAllocationName(stageAllocationName, assignment.cm2MarkingWorkflow.workflowType).get -> Map.empty[Marker, Set[Student]])
        } else stageAllocations
      }

      def isMarkerFeedbackFinalised(student: Student, stage: MarkingWorkflowStage): Boolean = {
        assignment.findFeedback(student.getUserId).toSeq.flatMap(_.allMarkerFeedback)
          .filter(_.stage == stage).exists(_.finalised)
      }

      for {
        (stageAllocationName, priorAllocations) <- fetchAllocations(assignment).allocations
        (workflowStage, newAllocations) <- workflowStageAllocations(stageAllocationName)
        (oldMarker, students) <- priorAllocations
        student <- students.toSeq
        if isMarkerFeedbackFinalised(student, workflowStage) && isMarkerUnallocated(student, newAllocations)
      } yield {
        (workflowStage, oldMarker, student)
      }
    }

    // check if student  with finalised feedback has been unallocated marker
    unallocatedMarkerWithFinalisedFeedback
      .groupBy { case (stage, marker, _) => (stage, marker) }
      .view.mapValues(_.map({ case (_, _, student) => student }))
      .foreach {
        case ((stage, marker), students) =>
          val args: Array[Object] = Array(stage.description.toLowerCase, marker.getFullName, students.map(_.getFullName).mkString(", "))
          errors.reject("markingWorkflow.markers.finalised", args, "")
      }
  }

}

trait AssignMarkersSequentialStageValidationState {
  var allowSameMarkerForSequentialStages: Boolean = false

  var allocationWarnings: Seq[String] = Nil
}

trait AssignMarkersValidation extends SelfValidating with ValidateConcurrentStages with ValidateSequentialStages with ValidateMarkerChanges {
  self: AssignMarkersState with UserLookupComponent with CM2MarkingWorkflowServiceComponent with AssessmentMembershipServiceComponent =>

  def validate(errors: Errors): Unit = {
    val currentAllocationMap = allocationMap // just calculate current allocationMap once rather than invoking method a few times
    validateConcurrentStages(currentAllocationMap, errors)
    validateSequentialStageMarkers(currentAllocationMap, errors)
    validateChangedMarkerAllocations(currentAllocationMap, errors)
    validateUnallocatedMarkerAllocations(currentAllocationMap, errors)
  }
}

trait AssignMarkersDescription extends Describable[Assignment] {
  self: AssignMarkersState =>

  override lazy val eventName: String = "AssignMarkers"

  private def printAllocation(allocation: Allocations): String = allocation.map { case (marker, students) =>
    s"${marker.getUserId} -> ${students.map(_.getUserId).toSeq.sorted.mkString(",")}"
  }.mkString("\n")

  def describe(d: Description): Unit = {
    d.assignment(assignment)
      .properties("allocations" -> allocationMap.map {
        case (stage, allocation) => s"${stage.roleName}:\n${printAllocation(allocation)}"
      }.mkString("\n"))
  }
}

trait AssignMarkersState extends AssignMarkersSequentialStageValidationState {
  def assignment: Assignment

  def allocationMap: Map[MarkingWorkflowStage, Allocations]
}

trait AssignMarkersBySpreadsheetState extends AssignMarkersState {
  def allocationMap: Map[MarkingWorkflowStage, Allocations] = _allocationMap

  protected var _allocationMap: Map[MarkingWorkflowStage, Allocations] = Map()
  var rowsWithErrors: Seq[ParsedRow] = Nil
  var file: UploadedFile = new UploadedFile

  allowSameMarkerForSequentialStages = true
}

trait AssignMarkersDragAndDropState extends AssignMarkersState {

  this: UserLookupComponent =>

  // the nasty java mutable bindable map transformed into lovely immutable scala maps with the users resolved
  def allocationMap: Map[MarkingWorkflowStage, Allocations] = {
    allocations.asScala.map { case (stage, jmap) =>
      stage -> jmap.asScala.map { case (marker, students) =>
        userLookup.getUserByUserId(marker) -> students.asScala.map(userLookup.getUserByUserId).toSet
      }.toMap
    }.toMap
  }

  var allocations: JMap[MarkingWorkflowStage, JMap[String, JList[String]]] = LazyMaps.create { _: MarkingWorkflowStage =>
    LazyMaps.create { _: String => JArrayList(): JList[String] }.asJava
  }.asJava
}

trait AssignMarkersBySmallGroupsState extends AssignMarkersState {
  def groupMarkerAllocations: Seq[GroupMarkerAllocation]

  def allocationMap: Map[MarkingWorkflowStage, Allocations] = {
    groupMarkerAllocations.groupBy(_.stages).flatMap { case (stages, allocations) =>
      var map = Map.empty[Marker, Set[Student]]

      allocations.map(alloc => alloc.marker -> alloc.group.students.users.toSet)
        .foreach { case (marker, students) =>
          map += marker -> (map.getOrElse(marker, Set.empty) ++ students)
        }

      stages.asScala.map(stage => stage -> map)
    }
  }
}
