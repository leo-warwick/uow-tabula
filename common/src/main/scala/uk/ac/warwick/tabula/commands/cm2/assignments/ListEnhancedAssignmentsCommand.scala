package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.{DateTime, LocalDate, Seconds}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.WorkflowStages.StageProgress
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.MarkerWorkflowCache.Json
import uk.ac.warwick.tabula.commands.cm2.assignments.AssignmentInfoFilters.DueDateFilter
import uk.ac.warwick.tabula.commands.cm2.assignments.ListEnhancedAssignmentsCommand._
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.markingworkflow.{FinalStage, MarkingWorkflowStage, MarkingWorkflowType}
import uk.ac.warwick.tabula.data.model.{Assignment, Department, Module}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.cm2._
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, CacheStrategyComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.util.cache._
import uk.ac.warwick.util.collections.Pair

import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object ListEnhancedAssignmentsCommand {

  trait AssignmentInfo {
    def assignment: Assignment
  }

  case class BasicAssignmentInfo(
    assignment: Assignment
  ) extends AssignmentInfo

  case class AssignmentStageCategory(
    category: CM2WorkflowCategory,
    stages: Seq[AssignmentStage]
  )

  case class AssignmentStage(
    stage: CM2WorkflowStage,
    progress: Seq[AssignmentStageProgress]
  )

  case class AssignmentStageProgress(
    progress: StageProgress,
    count: Int
  )

  case class AssignmentNextStage(
    stage: WorkflowStage,
    url: Option[String],
    count: Int
  )

  case class EnhancedAssignmentInfo(
    assignment: Assignment,
    stages: Seq[AssignmentStageCategory],
    nextStages: Seq[AssignmentNextStage]
  ) extends AssignmentInfo

  case class ModuleAssignmentsInfo(
    module: Module,
    assignments: Seq[AssignmentInfo]
  )

  type DepartmentResult = Seq[ModuleAssignmentsInfo]
  type DepartmentCommand = Appliable[DepartmentResult] with ListAssignmentsCommandRequest with ListDepartmentAssignmentsCommandState with ListAssignmentsModulesWithPermission

  type ModuleResult = ModuleAssignmentsInfo
  type ModuleCommand = Appliable[ModuleResult] with ListModuleAssignmentsCommandState

  type AssignmentResult = EnhancedAssignmentInfo
  type AssignmentCommand = Appliable[AssignmentResult] with ListEnhancedAssignmentCommandState

  val AdminPermission: Permission = Permissions.Module.ManageAssignments

  def department(department: Department, academicYear: AcademicYear, user: CurrentUser): DepartmentCommand =
    new ListDepartmentAssignmentsCommandInternal(department, academicYear, user)
      with ListDepartmentAssignmentsPermissions
      with ListAssignmentsModulesWithPermission
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringSecurityServiceComponent
      with ComposableCommand[DepartmentResult]
      with Unaudited with ReadOnly

  def module(module: Module, academicYear: AcademicYear, user: CurrentUser): ModuleCommand =
    new ListModuleAssignmentsCommandInternal(module, academicYear, user)
      with ListModuleAssignmentsPermissions
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringSecurityServiceComponent
      with CachedAssignmentProgress
      with AutowiringAssessmentServiceComponent
      with AutowiringCM2WorkflowProgressServiceComponent
      with AutowiringCacheStrategyComponent
      with ComposableCommand[ModuleResult]
      with Unaudited with ReadOnly

  def moduleSkeleton(module: Module, academicYear: AcademicYear, user: CurrentUser): ModuleCommand =
    new ListModuleSkeletonAssignmentsCommandInternal(module, academicYear, user)
      with ListModuleAssignmentsPermissions
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringSecurityServiceComponent
      with CachedAssignmentProgress
      with AutowiringAssessmentServiceComponent
      with AutowiringCM2WorkflowProgressServiceComponent
      with AutowiringCacheStrategyComponent
      with ComposableCommand[ModuleResult]
      with Unaudited with ReadOnly


  def assignment(assignment: Assignment, academicYear: AcademicYear, user: CurrentUser): AssignmentCommand =
    new ListEnhancedAssignmentCommandInternal(assignment, academicYear, user)
      with ListEnhancedAssignmentPermissions
      with AutowiringModuleAndDepartmentServiceComponent
      with AutowiringSecurityServiceComponent
      with CachedAssignmentProgress
      with AutowiringAssessmentServiceComponent
      with AutowiringCM2WorkflowProgressServiceComponent
      with AutowiringCacheStrategyComponent
      with ComposableCommand[AssignmentResult]
      with Unaudited with ReadOnly

  def sortedModuleAssignments(module: Module, academicYear: AcademicYear): Seq[Assignment] =
    module.assignments.asScala.toSeq
      .filterNot(_.deleted)
      .filter(_.academicYear == academicYear)
      .sortBy { a => (a.openDate, a.name) }

}

trait ListAssignmentsCommandState {
  def academicYear: AcademicYear

  def user: CurrentUser
}

trait ListDepartmentAssignmentsCommandState extends ListAssignmentsCommandState {
  def department: Department
}

trait ListModuleAssignmentsCommandState extends ListAssignmentsCommandState {
  def module: Module
}

trait ListEnhancedAssignmentCommandState extends ListAssignmentsCommandState {
  def assignment: Assignment
}

trait ListAssignmentsCommandRequest {
  self: ListAssignmentsCommandState =>

  var moduleFilters: JList[AssignmentInfoFilters.Module] = JArrayList()
  var workflowTypeFilters: JList[AssignmentInfoFilter] = JArrayList()
  var statusFilters: JList[AssignmentInfoFilter] = JArrayList()
  var dueDateFilter: DueDateFilter = new DueDateFilter
  var showEmptyModules: Boolean = true
}

abstract class ListAssignmentsCommandInternal(val academicYear: AcademicYear, val user: CurrentUser)
  extends ListAssignmentsCommandState with ListAssignmentsCommandRequest {

  protected def moduleInfo(module: Module): ModuleAssignmentsInfo =
    ModuleAssignmentsInfo(
      module,
      sortedModuleAssignments(module, academicYear).map { assignment =>
        BasicAssignmentInfo(assignment)
      }.filter { info =>
        (moduleFilters.asScala.isEmpty || moduleFilters.asScala.exists(_ (info))) &&
          (workflowTypeFilters.asScala.isEmpty || workflowTypeFilters.asScala.exists(_ (info))) &&
          (statusFilters.asScala.isEmpty || statusFilters.asScala.exists(_ (info))) &&
          dueDateFilter(info)
      }
    )
}

class ListDepartmentAssignmentsCommandInternal(val department: Department, academicYear: AcademicYear, user: CurrentUser)
  extends ListAssignmentsCommandInternal(academicYear, user)
    with CommandInternal[DepartmentResult]
    with ListDepartmentAssignmentsCommandState {
  self: ListAssignmentsModulesWithPermission =>

  override def applyInternal(): DepartmentResult =
    allModulesWithPermission.filter { module =>
      moduleFilters.asScala.isEmpty || moduleFilters.asScala.exists(_.module == module)
    }.map(moduleInfo).filter { info => showEmptyModules || info.assignments.nonEmpty }.sortBy(_.module.code)

}

class ListModuleAssignmentsCommandInternal(val module: Module, academicYear: AcademicYear, user: CurrentUser)
  extends ListAssignmentsCommandInternal(academicYear, user)
    with CommandInternal[ModuleResult]
    with ListModuleAssignmentsCommandState {
  self: AssignmentProgress
    with CM2WorkflowProgressServiceComponent =>

  override def applyInternal(): ModuleResult = {
    val info = moduleInfo(module)

    info.copy(assignments = info.assignments.map { a => enhance(a.assignment) })
  }

}

class ListModuleSkeletonAssignmentsCommandInternal(val module: Module, academicYear: AcademicYear, user: CurrentUser)
  extends ListAssignmentsCommandInternal(academicYear, user)
    with CommandInternal[ModuleResult]
    with ListModuleAssignmentsCommandState {
  self: AssignmentProgress
    with CM2WorkflowProgressServiceComponent =>

  override def applyInternal(): ModuleResult = {
    ModuleAssignmentsInfo(
      module,
      sortedModuleAssignments(module, academicYear).map { assignment =>
        BasicAssignmentInfo(assignment)
      })
  }
}

class ListEnhancedAssignmentCommandInternal(val assignment: Assignment, academicYear: AcademicYear, user: CurrentUser)
  extends ListAssignmentsCommandInternal(academicYear, user)
    with CommandInternal[AssignmentResult]
    with ListEnhancedAssignmentCommandState {
  self: AssignmentProgress
    with CM2WorkflowProgressServiceComponent =>

  override def applyInternal(): AssignmentResult = {
    enhance(assignment)
  }

}

trait AssignmentProgress {
  def enhance(assignment: Assignment): EnhancedAssignmentInfo
}

object AssignmentProgressCache {
  type AssignmentId = String
  type Json = String

  final val CacheName: String = "AssignmentProgress"

  /**
   * 7 days in seconds - we can cache this for a reasonably long time because we don't cache across deadline boundaries
   */
  final val CacheExpiryTime: Duration = Duration.ofDays(7)
}

trait AssignmentProgressCache extends TaskBenchmarking {
  self: AssessmentServiceComponent with CM2WorkflowProgressServiceComponent with CacheStrategyComponent =>

  import AssignmentProgressCache._

  private def enhanceUncached(assignment: Assignment): EnhancedAssignmentInfo = benchmarkTask(s"Get progress information for ${assignment.name}") {
    val results = SubmissionAndFeedbackCommand(assignment).apply()

    val allStages = workflowProgressService.getStagesFor(assignment)

    val stages = allStages.flatMap { stage =>
      val name = stage.toString

      val progresses = results.students.flatMap(_.stages.get(name)).filter { p => p.started || p.completed }
      if (progresses.nonEmpty) {
        Seq(AssignmentStage(
          stage,
          progresses.groupBy(identity).view.mapValues(_.size).toSeq.map { case (p, c) => AssignmentStageProgress(p, c) }
        ))
      } else {
        Nil
      }
    }.groupBy(_.stage.category)

    val stagesByCategory = CM2WorkflowCategory.members.map { category =>
      AssignmentStageCategory(category, stages.getOrElse(category, Nil))
    }.filterNot(_.stages.isEmpty)

    val allNextStages =
      results.students.flatMap(_.nextStage).groupBy(identity).view.mapValues(_.size)

    val nextStages =
      allStages.filter(allNextStages.contains).map { stage =>
        AssignmentNextStage(
          stage,
          stage.route(assignment).map(_.url),
          allNextStages(stage)
        )
      }

    EnhancedAssignmentInfo(assignment, stagesByCategory, nextStages)
  }

  private val assignmentProgressCacheEntryFactory = new CacheEntryFactory[AssignmentId, Json] {

    override def create(id: AssignmentId): Json = {
      val assignment = assessmentService.getAssignmentById(id).getOrElse {
        throw new CacheEntryUpdateException(s"Could not find assignment $id")
      }

      Try {
        toJson(enhanceUncached(assignment))
      } match {
        case Success(info) => info
        case Failure(e) => throw new CacheEntryUpdateException(e)
      }
    }

    override def create(ids: JList[AssignmentId]): JMap[AssignmentId, Json] =
      JMap(ids.asScala.toSeq.map(id => (id, create(id))): _*)

    override def isSupportsMultiLookups: Boolean = true

    override def shouldBeCached(info: Json): Boolean = true
  }

  private def toJson(info: EnhancedAssignmentInfo): Json =
    JsonHelper.toJson(Map(
      "assignment" -> info.assignment.id,
      "stages" -> info.stages.map { c =>
        Map(
          "category" -> c.category.code,
          "stages" -> c.stages.map { s =>
            Map(
              "stage" -> s.stage.toString,
              "progress" -> s.progress
            )
          }
        )
      },
      "nextStages" -> info.nextStages.map { ns =>
        Map(
          "stage" -> ns.stage.toString,
          "url" -> ns.url,
          "count" -> ns.count
        )
      }
    ))

  lazy val assignmentProgressCache: Cache[AssignmentId, Json] =
    Caches.builder(CacheName, assignmentProgressCacheEntryFactory, cacheStrategy)
      .expireAfterWrite(CacheExpiryTime)
      .expiryStategy(new TTLCacheExpiryStrategy[AssignmentId, Json] {
        override def getTTL(entry: CacheEntry[AssignmentId, Json]): Pair[Number, TimeUnit] = {
          // Extend the cache time to the next deadline if it's shorter than the default cache expiry
          val seconds: Number = assessmentService.getAssignmentById(entry.getKey) match {
            case Some(assignment) if !assignment.isClosed && Option(assignment.closeDate).nonEmpty =>
              Seconds.secondsBetween(DateTime.now, assignment.closeDate).getSeconds

            case Some(assignment) =>
              val futureExtensionDate = assignment.approvedExtensions.values.flatMap(_.expiryDate).toSeq.sorted.find(_.isAfterNow)

              futureExtensionDate.map[Number] { dt => Seconds.secondsBetween(DateTime.now, dt).getSeconds }
                .getOrElse(CacheExpiryTime.getSeconds)

            case _ => CacheExpiryTime.getSeconds
          }

          Pair.of(seconds, TimeUnit.SECONDS)
        }
      })
      .maximumSize(10000) // Ignored by Memcached, just for Caffeine (testing)
      .build()
}

trait CachedAssignmentProgress extends AssignmentProgress with AssignmentProgressCache {
  self: AssessmentServiceComponent with CM2WorkflowProgressServiceComponent with CacheStrategyComponent =>

  private def fromJson(json: Json): EnhancedAssignmentInfo = {
    val map = JsonHelper.toMap[Any](json)
    val assignment = assessmentService.getAssignmentById(map("assignment").asInstanceOf[String]).get
    EnhancedAssignmentInfo(
      assignment = assignment,
      stages = map("stages").asInstanceOf[Seq[Map[String, Any]]].map { c =>
        AssignmentStageCategory(
          category = CM2WorkflowCategory.members.find(_.code == c("category").asInstanceOf[String]).get,
          stages = c("stages").asInstanceOf[Seq[Map[String, Any]]].map { s =>
            val stage = CM2WorkflowStages.of(s("stage").asInstanceOf[String])

            AssignmentStage(
              stage = stage,
              progress = s("progress").asInstanceOf[Seq[Map[String, Any]]].map { p =>
                val progress = p("progress").asInstanceOf[Map[String, Any]]

                AssignmentStageProgress(
                  progress = StageProgress(
                    stage = stage,
                    started = progress("started").asInstanceOf[Boolean],
                    messageCode = progress("messageCode").asInstanceOf[String],
                    health = WorkflowStageHealth.fromCssClass(progress("health").asInstanceOf[Map[String, Any]]("cssClass").asInstanceOf[String]),
                    completed = progress("completed").asInstanceOf[Boolean],
                    preconditionsMet = progress("preconditionsMet").asInstanceOf[Boolean],
                    skipped = progress.getOrElse("skipped", false).asInstanceOf[Boolean]
                  ),
                  count = p("count").asInstanceOf[Int]
                )
              }
            )
          }
        )
      },
      nextStages = map("nextStages").asInstanceOf[Seq[Map[String, Any]]].map { ns =>
        AssignmentNextStage(
          stage = CM2WorkflowStages.of(ns("stage").asInstanceOf[String]),
          url = ns.get("url").map(_.asInstanceOf[String]),
          count = ns("count").asInstanceOf[Int]
        )
      }
    )
  }

  def enhance(assignment: Assignment): EnhancedAssignmentInfo =
    fromJson(assignmentProgressCache.get(assignment.id))

}

trait ListAssignmentsModulesWithPermission {
  self: ListDepartmentAssignmentsCommandState
    with ModuleAndDepartmentServiceComponent
    with SecurityServiceComponent =>

  lazy val modulesWithPermission: Set[Module] =
    moduleAndDepartmentService.modulesWithPermission(user, AdminPermission, department)

  lazy val allModulesWithPermission: Seq[Module] =
    if (securityService.can(user, AdminPermission, department)) {
      department.modules.asScala.toSeq
    } else {
      modulesWithPermission.toList.sorted
    }

}

trait ListDepartmentAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListDepartmentAssignmentsCommandState
    with ListAssignmentsModulesWithPermission
    with SecurityServiceComponent =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    if (securityService.can(user, AdminPermission, mandatory(department))) {
      // This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
      p.PermissionCheck(AdminPermission, department)
    } else {
      val managedModules = modulesWithPermission.toList

      // This is implied by the above, but it's nice to check anyway. Avoid exception if there are no managed modules
      if (managedModules.nonEmpty) p.PermissionCheckAll(AdminPermission, managedModules)
      else p.PermissionCheck(AdminPermission, department)
    }
}

trait ListModuleAssignmentsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListModuleAssignmentsCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(module))

}

trait ListEnhancedAssignmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ListEnhancedAssignmentCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(AdminPermission, mandatory(assignment))

}

sealed trait AssignmentInfoFilter {
  def description: String

  val getName: String = AssignmentInfoFilters.shortName(getClass)

  def apply(info: AssignmentInfo): Boolean
}

object AssignmentInfoFilters {
  private val ObjectClassPrefix = AssignmentInfoFilters.getClass.getName

  def shortName(clazz: Class[_ <: AssignmentInfoFilter]): String
  = clazz.getName.substring(ObjectClassPrefix.length, clazz.getName.length - 1).replace('$', '.')

  def of(name: String): AssignmentInfoFilter = {
    try {
      // Go through the magical hierarchy
      val clz = Class.forName(ObjectClassPrefix + name.replace('.', '$') + "$")
      clz.getDeclaredField("MODULE$").get(null).asInstanceOf[AssignmentInfoFilter]
    } catch {
      case _: ClassNotFoundException => throw new IllegalArgumentException("AssignmentInfoFilter " + name + " not recognised")
      case _: ClassCastException => throw new IllegalArgumentException("AssignmentInfoFilter " + name + " is not an endpoint of the hierarchy")
    }
  }

  case class Module(module: model.Module) extends AssignmentInfoFilter {
    val description: String = module.code.toUpperCase() + " " + module.name
    override val getName: String = s"Module(${module.code})"

    def apply(info: AssignmentInfo): Boolean = info.assignment.module == module
  }

  def allModuleFilters(modules: Seq[model.Module]): Seq[Module] = modules.map(Module.apply)

  object NoWorkflow extends AssignmentInfoFilter {
    val description: String = "No workflow"

    def apply(info: AssignmentInfo): Boolean = info.assignment.cm2MarkingWorkflow == null
  }

  case class CM2WorkflowType(method: MarkingWorkflowType) extends AssignmentInfoFilter {
    val description: String = method.description
    override val getName: String = method.name

    def apply(info: AssignmentInfo): Boolean = Option(info.assignment.cm2MarkingWorkflow).map(_.workflowType).contains(method)
  }

  def allWorkflowTypeFilters: Seq[AssignmentInfoFilter] = Seq(NoWorkflow) ++ MarkingWorkflowType.values.map(CM2WorkflowType.apply)

  object Status {

    case object Active extends AssignmentInfoFilter {
      val description = "Active"

      def apply(info: AssignmentInfo): Boolean =
        info.assignment.isBetweenDates() || info.assignment.feedbackDeadline.exists(d => !d.isBefore(LocalDate.now))
    }

    case object Inactive extends AssignmentInfoFilter {
      val description = "Inactive"

      def apply(info: AssignmentInfo): Boolean =
        !info.assignment.isBetweenDates() && info.assignment.feedbackDeadline.forall(_.isBefore(LocalDate.now))
    }

    case object NoMarkers extends AssignmentInfoFilter {
      val description = "No markers"

      def apply(info: AssignmentInfo): Boolean =
        Option(info.assignment.cm2MarkingWorkflow).nonEmpty &&
          info.assignment.allFeedback
            .forall { f =>
              f.allMarkerFeedback
                .flatMap(_.marker.getUserId.maybeText)
                .isEmpty
            }
    }

    case object ReleasedToStudents extends AssignmentInfoFilter {
      val description = "Released to students"

      def apply(info: AssignmentInfo): Boolean = info.assignment.hasReleasedFeedback
    }

    case object Closed extends AssignmentInfoFilter {
      val description = "Closed"

      def apply(info: AssignmentInfo): Boolean = info.assignment.isClosed
    }

    case object LateSubmissions extends AssignmentInfoFilter {
      val description = "Late submissions"

      def apply(info: AssignmentInfo): Boolean = info.assignment.lateSubmissionCount > 0
    }

    case object ExtensionsRequested extends AssignmentInfoFilter {
      val description = "Extensions requested"

      def apply(info: AssignmentInfo): Boolean = info.assignment.hasUnapprovedExtensions
    }

    case object NotCheckedForPlagiarism extends AssignmentInfoFilter {
      val description = "Not checked for plagiarism"

      def apply(info: AssignmentInfo): Boolean = !info.assignment.submissions.asScala.exists(_.hasOriginalityReport)
    }

    case object NotReleasedToMarkers extends AssignmentInfoFilter {
      val description = "Not released to markers"

      def apply(info: AssignmentInfo): Boolean = info.assignment.hasWorkflow && info.assignment.allFeedback.flatMap(_.outstandingStages.asScala).isEmpty
    }

    case object BeingMarked extends AssignmentInfoFilter {
      val description = "Being marked"

      def apply(info: AssignmentInfo): Boolean =
        info.assignment.allFeedback
          .flatMap(_.outstandingStages.asScala)
          .collect { case s: MarkingWorkflowStage => s }
          .nonEmpty
    }

    case object MarkingComplete extends AssignmentInfoFilter {
      val description = "Marking complete"

      def apply(info: AssignmentInfo): Boolean = info.assignment.allFeedback
        .map(_.outstandingStages.asScala)
        .forall(stages => stages.nonEmpty && stages.collect { case f: FinalStage => f }.size == stages.size)
    }

    case object LateFeedback extends AssignmentInfoFilter {
      val description = "Late feedback"

      def apply(info: AssignmentInfo): Boolean = info.assignment.hasOutstandingFeedbackDueBefore(LocalDate.now)
    }

    case object FeedbackUnreleased extends AssignmentInfoFilter {
      val description = "Feedback unreleased"

      def apply(info: AssignmentInfo): Boolean = info.assignment.hasUnreleasedFeedback
    }

    val all = Seq(ReleasedToStudents, Closed, Active, Inactive, NoMarkers, LateSubmissions, ExtensionsRequested, NotCheckedForPlagiarism, NotReleasedToMarkers, BeingMarked, MarkingComplete, LateFeedback, FeedbackUnreleased)
  }

  class DueDateFilter extends AssignmentInfoFilter {
    var from: LocalDate = _
    var to: LocalDate = _

    override def description: String = "Between two dates"

    override def apply(info: AssignmentInfo): Boolean =
      (Option(from).isEmpty || (info.assignment.closeDate != null && !info.assignment.closeDate.isBefore(from.toDateTimeAtStartOfDay))) &&
        (Option(to).isEmpty || (info.assignment.closeDate != null && !info.assignment.closeDate.isAfter(to.plusDays(1).toDateTimeAtStartOfDay)))
  }

}
