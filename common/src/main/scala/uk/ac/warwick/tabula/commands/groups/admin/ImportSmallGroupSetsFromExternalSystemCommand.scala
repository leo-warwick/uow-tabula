package uk.ac.warwick.tabula.commands.groups.admin

import java.util.concurrent.TimeoutException

import org.joda.time.LocalTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.ImportSmallGroupSetsFromExternalSystemCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

import scala.jdk.CollectionConverters._
import scala.concurrent.Await
import scala.util.Try

object ImportSmallGroupSetsFromExternalSystemCommand {
  val RequiredPermission = Permissions.SmallGroups.ImportFromExternalSystem

  case class TimetabledSmallGroupEvent(
    module: Module,
    eventType: TimetableEventType,
    events: Seq[TimetableEvent]
  )

  def apply(department: Department, user: CurrentUser) =
    new ImportSmallGroupSetsFromExternalSystemCommandInternal(department, user)
      with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
      with ImportSmallGroupSetsFromExternalSystemPermissions
      with ImportSmallGroupSetsFromExternalSystemValidation
      with ImportSmallGroupSetsFromExternalSystemDescription
      with SetDefaultSelectedValue
      with AutowiringSecurityServiceComponent
      with AutowiringModuleAndDepartmentServiceComponent
      with ComposableCommand[Seq[SmallGroupSet]]
      with CommandSmallGroupSetGenerator
      with CommandSmallGroupEventGenerator
      with AutowiringSmallGroupServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringScientiaTimetableFetchingServiceComponent

  def generateSmallGroupSetNameAndFormat(module: Module, eventType: TimetableEventType): (String, SmallGroupFormat with Product with Serializable) = {
    val format = eventType match {
      case TimetableEventType.Seminar => SmallGroupFormat.Seminar
      case TimetableEventType.Lecture => SmallGroupFormat.Lecture
      case TimetableEventType.Exam => SmallGroupFormat.Exam
      case TimetableEventType.Meeting => SmallGroupFormat.Meeting
      case _ => SmallGroupFormat.Workshop
    }
    val name = "%s %ss".format(module.code.toUpperCase, format)

    (name, format)
  }
}

trait ImportSmallGroupSetsFromExternalSystemCommandState extends CurrentAcademicYear {
  def department: Department

  def user: CurrentUser

  var selected: JList[JBoolean] = LazyLists.createWithFactory { () => true }
}

trait ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState {
  self: ImportSmallGroupSetsFromExternalSystemCommandState with ModuleAndDepartmentServiceComponent with SecurityServiceComponent =>

  lazy val canManageDepartment: Boolean = securityService.can(user, RequiredPermission, department)
  lazy val modulesWithPermission: Set[Module] = moduleAndDepartmentService.modulesWithPermission(user, RequiredPermission, department)

  lazy val modules: Iterable[Module] =
    if (canManageDepartment) department.modules.asScala
    else modulesWithPermission
}

trait LookupEventsFromModuleTimetables {
  self: ModuleTimetableFetchingServiceComponent with ImportSmallGroupSetsFromExternalSystemCommandState with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState =>

  lazy val timetabledEvents: Seq[TimetabledSmallGroupEvent] =
    modules.toSeq.flatMap { module =>
      Try {
        Await.result(timetableFetchingService.getTimetableForModule(module.code.toUpperCase, includeStudents = true), ImportSmallGroupEventsFromExternalSystemCommand.Timeout)
          .events
          .filter(ImportSmallGroupEventsFromExternalSystemCommand.isValidForYear(academicYear))
          .groupBy(_.eventType)
          .toSeq.map { case (eventType, events) =>
          TimetabledSmallGroupEvent(module, eventType, events.sorted)
        }
      }.recover {
        case _: TimeoutException | _: TimetableEmptyException => Nil
      }.get
    }.seq.sortBy { e => (e.module.code, e.eventType.code) }

}

trait SetDefaultSelectedValue extends PopulateOnForm {
  self: ImportSmallGroupSetsFromExternalSystemCommandState with LookupEventsFromModuleTimetables =>

  def populate(): Unit = {
    timetabledEvents.zipWithIndex.foreach { case (TimetabledSmallGroupEvent(module, eventType, _), i) =>
      val (name, _) = generateSmallGroupSetNameAndFormat(module, eventType)

      selected.get(i) // Run a get() first to ensure that the list has been grown
      selected.set(i, !module.groupSets.asScala.exists { set =>
        set.academicYear == academicYear &&
          !set.archived && !set.deleted &&
          set.name == name
      } && ImportSmallGroupEventsFromExternalSystemCommand.isImportByDefault(eventType))
    }
  }
}

class ImportSmallGroupSetsFromExternalSystemCommandInternal(val department: Department, val user: CurrentUser)
  extends CommandInternal[Seq[SmallGroupSet]]
    with ImportSmallGroupSetsFromExternalSystemCommandState
    with LookupEventsFromModuleTimetables {
  self: ModuleTimetableFetchingServiceComponent
    with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
    with SmallGroupServiceComponent
    with SmallGroupSetGenerator
    with SmallGroupEventGenerator
    with UserLookupComponent =>

  def applyInternal(): Seq[SmallGroupSet] = transactional() {
    // For each combination of module & event type, create a small group set with a group for each event
    timetabledEvents.zipWithIndex
      .filter { case (_, i) => selected.get(i) }
      .map { case (TimetabledSmallGroupEvent(module, eventType, events), _) =>
        val (name, format) = generateSmallGroupSetNameAndFormat(module, eventType)

        val set = createSet(module, format, name)

        events.zipWithIndex.foreach { case (e, i) =>
          val group = new SmallGroup(set)
          group.name = s"Group ${i + 1}"
          e.students.foreach(group.students.add)

          smallGroupService.saveOrUpdate(group)

          set.groups.add(group)

          val tutorUsercodes = e.staff.map(_.getUserId)

          createEvent(module, set, group, e.weekRanges, e.day, e.startTime, e.endTime, e.location, e.name, tutorUsercodes)
        }

        smallGroupService.saveOrUpdate(set)
        set
      }
  }
}

trait SmallGroupSetGenerator {
  def createSet(module: Module, format: SmallGroupFormat, name: String): SmallGroupSet
}

trait CommandSmallGroupSetGenerator extends SmallGroupSetGenerator {
  self: ImportSmallGroupSetsFromExternalSystemCommandState =>

  def createSet(module: Module, format: SmallGroupFormat, name: String): SmallGroupSet = {
    val command = ModifySmallGroupSetCommand.create(module)
    command.format = format
    command.name = name
    command.academicYear = academicYear

    command.apply()
  }
}

trait SmallGroupEventGenerator {
  def createEvent(module: Module, set: SmallGroupSet, group: SmallGroup, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], title: String, tutorUsercodes: Seq[String]): SmallGroupEvent
}

trait CommandSmallGroupEventGenerator extends SmallGroupEventGenerator {
  def createEvent(module: Module, set: SmallGroupSet, group: SmallGroup, weeks: Seq[WeekRange], day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, location: Option[Location], title: String, tutorUsercodes: Seq[String]): SmallGroupEvent = {
    val command = ModifySmallGroupEventCommand.create(module, set, group, isImport = true)
    command.weekRanges = weeks
    command.day = day
    command.startTime = startTime
    command.endTime = endTime
    location.collect {
      case NamedLocation(name) => command.location = name

      case MapLocation(name, locationId, _) =>
        command.location = name
        command.locationId = locationId
    }

    command.title = title
    command.tutors.addAll(tutorUsercodes.asJavaCollection)

    command.deliveryMethod = EventDeliveryMethod.Hybrid

    command.apply()
  }
}

trait ImportSmallGroupSetsFromExternalSystemPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ImportSmallGroupSetsFromExternalSystemCommandState with ImportSmallGroupSetsFromExternalSystemPermissionsRestrictedState
    with SecurityServiceComponent =>

  def permissionsCheck(p: PermissionsChecking): Unit = {
    if (canManageDepartment) {
      // This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
      p.PermissionCheck(RequiredPermission, department)
    } else {
      val managedModules = modulesWithPermission.toList

      // This is implied by the above, but it's nice to check anyway. Avoid exception if there are no managed modules
      if (managedModules.nonEmpty) p.PermissionCheckAll(RequiredPermission, managedModules)
      else p.PermissionCheck(RequiredPermission, department)
    }
  }
}

trait ImportSmallGroupSetsFromExternalSystemValidation extends SelfValidating {
  self: ImportSmallGroupSetsFromExternalSystemCommandState =>

  def validate(errors: Errors): Unit = {
    if (academicYear == null) {
      errors.rejectValue("academicYear", "NotEmpty")
    }
  }
}

trait ImportSmallGroupSetsFromExternalSystemDescription extends Describable[Seq[SmallGroupSet]] {
  self: ImportSmallGroupSetsFromExternalSystemCommandState =>

  override def describe(d: Description): Unit =
    d.department(department)
}
