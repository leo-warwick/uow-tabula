package uk.ac.warwick.tabula.commands.groups.admin

import java.util.concurrent.TimeoutException

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin.ImportSmallGroupEventsFromExternalSystemCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, AutowiringUserLookupComponent, SmallGroupServiceComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.Try

object ImportSmallGroupEventsFromExternalSystemCommand {

  val Timeout: FiniteDuration = 15.seconds

  val RequiredPermission = Permissions.SmallGroups.Update

  def apply(module: Module, set: SmallGroupSet) =
    new ImportSmallGroupEventsFromExternalSystemCommandInternal(module, set)
      with ComposableCommand[Seq[SmallGroupEvent]]
      with ImportSmallGroupEventsFromExternalSystemPermissions
      with ImportSmallGroupEventsFromExternalSystemDescription
      with CommandSmallGroupEventGenerator
      with AutowiringSmallGroupServiceComponent
      with AutowiringUserLookupComponent
      with AutowiringScientiaTimetableFetchingServiceComponent
      with LookupEventsFromModuleTimetable

  def isValidForYear(academicYear: AcademicYear)(event: TimetableEvent): Boolean =
    event.year == academicYear

  def isImportByDefault(eventType: TimetableEventType): Boolean =
    eventType == TimetableEventType.Practical || eventType == TimetableEventType.Seminar || eventType == TimetableEventType.Other("WRB-ACTIVE")

  class EventToImport {
    def this(event: TimetableEvent) = {
      this()
      timetableEvent = event
    }

    var timetableEvent: TimetableEvent = _
    var group: SmallGroup = _
  }

}

trait ImportSmallGroupEventsFromExternalSystemRequestState {
  var eventsToImport: JList[EventToImport] = JArrayList()
}

trait ImportSmallGroupEventsFromExternalSystemCommandState {
  def module: Module

  def set: SmallGroupSet
}

class ImportSmallGroupEventsFromExternalSystemCommandInternal(val module: Module, val set: SmallGroupSet)
  extends CommandInternal[Seq[SmallGroupEvent]]
    with ImportSmallGroupEventsFromExternalSystemCommandState
    with ImportSmallGroupEventsFromExternalSystemRequestState {
  self: SmallGroupEventGenerator with UserLookupComponent with SmallGroupServiceComponent =>

  override def applyInternal(): Seq[SmallGroupEvent] = transactional() {
    val events = eventsToImport.asScala.toSeq
      .map { e => (e.timetableEvent, Option(e.group)) }
      .filter { case (_, group) => group.nonEmpty }
      .map { case (e, group) =>
        val tutorUsercodes = e.staff.map(_.getUserId)

        createEvent(module, set, group.get, e.weekRanges, e.day, e.startTime, e.endTime, e.location, e.name, tutorUsercodes)
      }

    smallGroupService.saveOrUpdate(set)
    events
  }
}

trait LookupEventsFromModuleTimetable extends PopulateOnForm {
  self: ImportSmallGroupEventsFromExternalSystemCommandState
    with ImportSmallGroupEventsFromExternalSystemRequestState
    with ModuleTimetableFetchingServiceComponent =>

  eventsToImport.clear()
  eventsToImport.addAll(Try {
    Await.result(timetableFetchingService.getTimetableForModule(module.code.toUpperCase, includeStudents = false), ImportSmallGroupEventsFromExternalSystemCommand.Timeout)
      .events
      .filter(ImportSmallGroupEventsFromExternalSystemCommand.isValidForYear(set.academicYear))
      .sorted
      .map(new EventToImport(_))
  }.recover {
    case _: TimeoutException | _: TimetableEmptyException => Nil
  }.get.asJava)

  override def populate(): Unit = {
    if (set.groups.asScala.forall {
      _.events.isEmpty
    }) {
      set.groups.asScala.sorted.zipWithIndex.foreach { case (group, index) =>
        if (eventsToImport.size() > index) {
          eventsToImport.get(index).group = group
        }
      }
    }
  }
}

trait ImportSmallGroupEventsFromExternalSystemPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: ImportSmallGroupEventsFromExternalSystemCommandState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(mandatory(set), mandatory(module))
    p.PermissionCheck(RequiredPermission, set)
  }

}

trait ImportSmallGroupEventsFromExternalSystemDescription extends Describable[Seq[SmallGroupEvent]] {
  self: ImportSmallGroupEventsFromExternalSystemCommandState =>

  override def describe(d: Description): Unit =
    d.smallGroupSet(set)

}
