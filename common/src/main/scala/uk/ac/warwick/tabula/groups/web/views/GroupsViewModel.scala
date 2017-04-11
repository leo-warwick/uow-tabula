package uk.ac.warwick.tabula.groups.web.views

import org.joda.time.DateTime
import uk.ac.warwick.tabula.{AcademicYear, WorkflowStage, WorkflowStages}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups._

import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap

/**
 * A selection of view model classes for passing to the template.
 *
 * See ViewModel for some common components.
 *
 * TODO building the Menu in code means deciding things like the text and icon
 * in Scala instead of Freemarker. Is this bad? Maybe just contain key decisions
 * like whether we are a manager, then the template can decide whether to render
 * those items.
 */
object GroupsViewModel {

	object ViewModules {
		def generate(mapping: Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]], viewerRole: ViewerRole): ViewModules = {
			val moduleItems = for ((module, sets) <- mapping) yield {
				ViewModule(module,
					sets.toSeq map { case (set, groups) =>
						ViewSet(set, ViewGroup.fromGroups(groups.sorted), viewerRole)
					},
					canManageGroups = false
				)
			}

			ViewModules( moduleItems.toSeq.sortBy(_.module.code), canManageDepartment = false )
		}

		def fromOccurrences(occurrences: Seq[SmallGroupEventOccurrence], viewerRole: ViewerRole): ViewModules = {
			ViewModules(
				occurrences.groupBy(_.event).map { case (event, groupedOccurrences) => ViewEvent(event, occurrences) }
					.groupBy(_.event.group).map { case (group, groupedEvents) => ViewGroup(group, groupedEvents.toSeq) }
					.groupBy(_.group.groupSet).map { case (set, groupedGroups) => ViewSet(set, groupedGroups.toSeq, viewerRole) }
					.groupBy(_.set.module).map { case (module, groupedSets) => ViewModule(module, groupedSets.toSeq, canManageGroups = false) }
					.toSeq.sortBy(_.module),
				canManageDepartment = false
			)
		}
	}


	case class ViewModules(
		moduleItems: Seq[ViewModule],
		canManageDepartment: Boolean
	) {
		def hasUnreleasedGroupsets : Boolean = hasUnreleasedGroupsets(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
		def hasUnreleasedGroupsets(academicYear: AcademicYear) : Boolean = moduleItems.exists(_.hasUnreleasedGroupsets(academicYear))
		def hasOpenableGroupsets: Boolean = moduleItems.exists(_.hasOpenableGroupsets)
		def hasCloseableGroupsets: Boolean = moduleItems.exists(_.hasCloseableGroupsets)
	}

	case class ViewModule(
		module: Module,
		setItems: Seq[ViewSet],
		canManageGroups: Boolean
	) {
		def hasUnreleasedGroupsets(academicYear: AcademicYear): Boolean = module.hasUnreleasedGroupSets(academicYear)
		def hasOpenableGroupsets: Boolean = module.groupSets.asScala.exists(s => (!s.openForSignups) && s.allocationMethod == SmallGroupAllocationMethod.StudentSignUp )
		def hasCloseableGroupsets: Boolean = module.groupSets.asScala.exists(s => s.openForSignups && s.allocationMethod == SmallGroupAllocationMethod.StudentSignUp )
	}

	trait ViewSetMethods {
		def set: SmallGroupSet
		def groups: Seq[ViewGroup]
		def viewerRole: ViewerRole

		def viewerIsStudent: Boolean = (viewerRole == StudentAssignedToGroup )|| (viewerRole == StudentNotAssignedToGroup)
		def viewerMustSignUp: Boolean = (viewerRole == StudentNotAssignedToGroup) && isStudentSignUp && set.openForSignups
		def canViewMembers: Boolean = viewerRole == Tutor || set.studentsCanSeeOtherMembers
		def canViewTutors: Boolean = viewerRole == Tutor || set.studentsCanSeeTutorName
		def isStudentSignUp: Boolean = set.allocationMethod == SmallGroupAllocationMethod.StudentSignUp
		def isLinked: Boolean = set.allocationMethod == SmallGroupAllocationMethod.Linked
	}

	case class ViewSet(
		set: SmallGroupSet,
		groups: Seq[ViewGroup],
		viewerRole: ViewerRole
	) extends ViewSetMethods

	case class SetProgress(
		percentage: Int,
		t: String,
		messageCode: String
	)

	case class ViewSetWithProgress(
		set: SmallGroupSet,
		groups: Seq[ViewGroup],
		viewerRole: ViewerRole,
		progress: SetProgress,
		nextStage: Option[WorkflowStage],
		stages: ListMap[String, WorkflowStages.StageProgress]
	) extends ViewSetMethods

	object ViewGroup {
		def fromGroups(groups: Seq[SmallGroup]): Seq[ViewGroup] = groups.map(g => ViewGroup(g, ViewEvent.fromEvents(g.events)))
	}

	case class ViewGroup(group: SmallGroup, events: Seq[ViewEvent])

	object ViewEvent {
		def fromEvents(events: Seq[SmallGroupEvent]): Seq[ViewEvent] = events.map(e => ViewEvent(e, Seq()))
	}

	case class ViewEvent(event: SmallGroupEvent, occurrences: Seq[SmallGroupEventOccurrence])

	sealed trait ViewerRole
	case object StudentAssignedToGroup extends ViewerRole
	case object StudentNotAssignedToGroup extends ViewerRole
	case object Tutor extends ViewerRole

}
