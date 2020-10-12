package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventOccurrence, SmallGroupSet, WeekRange}
import uk.ac.warwick.tabula.data.model.{AssessmentGroup, Department, Module, ScheduledNotification}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._

object CopySmallGroupSetsCommand {
  val RequiredPermission: Permission = Permissions.SmallGroups.Create

  def apply(department: Department, modules: Seq[Module]) =
    new CopySmallGroupSetsCommandInternal(department, modules)
      with ComposableCommand[Seq[SmallGroupSet]]
      with CopySmallGroupSetsPermissions
      with CopySmallGroupSetsDescription
      with CopySmallGroupSetsValidation
      with AutowiringSmallGroupServiceComponent
      with CopySmallGroupSetsScheduledNotifications
      with PopulateCopySmallGroupSetsRequestDefaults {
      override lazy val eventName = "CopySmallGroupSetsFromPrevious"
    }
}

abstract class CopySmallGroupSetsCommandInternal(val department: Department, val modules: Seq[Module])
  extends CommandInternal[Seq[SmallGroupSet]]
    with CopySmallGroupSetsCommandState
    with CopySmallGroupSetsRequestState {
  self: SmallGroupServiceComponent =>

  override def applyInternal(): Seq[SmallGroupSet] = {
    smallGroupSets.asScala.toSeq.filter(_.copy).map { state =>
      val set = state.smallGroupSet

      val copy = set.duplicateTo(
        transient = true,
        academicYear = targetAcademicYear,
        copyGroups = state.copyGroups || set.linked,
        copyEvents = state.copyGroups && state.copyEvents,
        copyMembership = false
      )

      // Reset to be "new"
      copy.archived = false
      copy.deleted = false
      copy.releasedToStudents = false
      copy.releasedToTutors = false
      copy.openForSignups = false
      copy.linkedDepartmentSmallGroupSet = null

      /*
       * TODO we make an assumption here that department small groups have unique names in a year, but
       * that actually isn't enforced.
       */
      Option(set.linkedDepartmentSmallGroupSet).foreach { link =>
        val copiedDepartmentSmallGroupSet =
          smallGroupService.getDepartmentSmallGroupSets(department, targetAcademicYear)
            .find(_.name == link.name)
            .getOrElse {
              link.duplicateTo(transient = true, academicYear = targetAcademicYear, copyMembership = false)
            }

        smallGroupService.saveOrUpdate(copiedDepartmentSmallGroupSet)

        copy.linkedDepartmentSmallGroupSet = copiedDepartmentSmallGroupSet
        copy.groups.asScala.foreach { group =>
          Option(group.linkedDepartmentSmallGroup).foreach { linkGroup =>
            group.linkedDepartmentSmallGroup =
              copiedDepartmentSmallGroupSet.groups.asScala.find {
                _.name == linkGroup.name
              }.orNull
          }
        }
      }

      // Try and guess SITS links for the new year
      set.assessmentGroups.asScala
        // Only where defined in the new year
        .filter(_.toUpstreamAssessmentGroupInfo(targetAcademicYear).nonEmpty)
        .foreach { group =>
          val newGroup = new AssessmentGroup
          newGroup.assessmentComponent = group.assessmentComponent
          newGroup.occurrence = group.occurrence
          newGroup.smallGroupSet = copy
          set.assessmentGroups.add(newGroup)
        }

      smallGroupService.saveOrUpdate(copy)
      copy
    }
  }

}

class CopySmallGroupSetState {
  def this(set: SmallGroupSet) = {
    this()
    smallGroupSet = set
  }

  var smallGroupSet: SmallGroupSet = _
  var copy: Boolean = false
  var copyGroups: Boolean = false
  var copyEvents: Boolean = false
}

trait CopySmallGroupSetsRequestState {
  var targetAcademicYear: AcademicYear = _
  var sourceAcademicYear: AcademicYear = _
  var smallGroupSets: JList[CopySmallGroupSetState] = _
}

trait PopulateCopySmallGroupSetsRequestDefaults extends PopulateOnForm {
  self: CopySmallGroupSetsRequestState with CopySmallGroupSetsCommandState with SmallGroupServiceComponent =>

  targetAcademicYear = AcademicYear.now()
  sourceAcademicYear = targetAcademicYear - 1
  smallGroupSets = LazyLists.create()

  override def populate(): Unit = {
    smallGroupSets.clear()
    modules.foreach { module =>
      smallGroupSets.addAll(
        smallGroupService.getSmallGroupSets(module, sourceAcademicYear).filterNot(_.archived)
          .map { set => new CopySmallGroupSetState(set) }
          .asJava
      )
    }
  }
}

trait CopySmallGroupSetsCommandState {
  def department: Department

  def modules: Seq[Module]
}

trait CopySmallGroupSetsValidation extends SelfValidating {
  self: CopySmallGroupSetsCommandState with CopySmallGroupSetsRequestState =>

  override def validate(errors: Errors): Unit = {
    if (sourceAcademicYear == null) errors.rejectValue("sourceAcademicYear", "NotEmpty")
    else if (targetAcademicYear == null) errors.rejectValue("targetAcademicYear", "NotEmpty")
    else if (sourceAcademicYear == targetAcademicYear) errors.rejectValue("targetAcademicYear", "smallGroupSet.copy.sameYear")
    else {
      lazy val targetAcademicYearWeeks: Seq[WeekRange.Week] =
        targetAcademicYear.weeks.keys.toSeq

      // TAB-3974 Prevent exception when copying an event with a week number that doesn't exist in the target year
      smallGroupSets.asScala.zipWithIndex
        // Only sets where we've set to copy events
        .filter { case (state, _) => state.copy && state.copyGroups && state.copyEvents }
        // Only where there is an event with a week number that doesn't exist in the target year
        .filterNot { case (state, _) =>
        val allWeeks: Seq[WeekRange.Week] =
          state.smallGroupSet.groups.asScala.toSeq
            .flatMap(_.events)
            .flatMap(_.allWeeks)
            .distinct

        allWeeks.forall(targetAcademicYearWeeks.contains)
      }
        .foreach { case (_, index) =>
          errors.rejectValue(s"smallGroupSets[$index].copyEvents", "smallGroupSet.copy.weekNotInTargetYear")
        }
    }
  }
}

trait CopySmallGroupSetsDescription extends Describable[Seq[SmallGroupSet]] {
  self: CopySmallGroupSetsCommandState with CopySmallGroupSetsRequestState =>

  override def describe(d: Description): Unit = d
    .department(department)
    .properties("modules" -> modules.map(_.id))
    .properties("smallGroupSets" -> smallGroupSets.asScala.filter(_.copy).map(_.smallGroupSet.id))
}

trait CopySmallGroupSetsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: CopySmallGroupSetsCommandState =>

  import CopySmallGroupSetsCommand._

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    if (modules.isEmpty) p.PermissionCheck(RequiredPermission, mandatory(department))
    else modules.foreach { module =>
      mustBeLinked(mandatory(module), mandatory(department))
      p.PermissionCheck(Permissions.SmallGroups.Read, module)
      p.PermissionCheck(Permissions.SmallGroups.Create, module)
    }
  }
}

trait CopySmallGroupSetsScheduledNotifications
  extends SchedulesNotifications[Seq[SmallGroupSet], SmallGroupEventOccurrence] with GeneratesNotificationsForSmallGroupEventOccurrence {

  self: SmallGroupServiceComponent =>

  override def transformResult(sets: Seq[SmallGroupSet]): Seq[SmallGroupEventOccurrence] =
  // get all the occurrences (even the ones in invalid weeks) so they can be cleared
    sets.flatMap(_.groups.asScala.flatMap(_.events.flatMap(smallGroupService.getOrCreateSmallGroupEventOccurrences)))

  override def scheduledNotifications(occurrence: SmallGroupEventOccurrence): Seq[ScheduledNotification[SmallGroupEventOccurrence]] = {
    generateNotifications(occurrence)
  }
}
