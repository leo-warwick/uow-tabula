package uk.ac.warwick.tabula.commands.groups.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.util.termdates.Term.TermType
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState

object ModifySmallGroupSetCommand {
	type Command = Appliable[SmallGroupSet] with SelfValidating with ModifySmallGroupSetCommandState
	type CreateCommand = Appliable[SmallGroupSet] with SelfValidating with CreateSmallGroupSetCommandState

	def create(module: Module): CreateCommand =
		new CreateSmallGroupSetCommandInternal(module)
			with ComposableCommand[SmallGroupSet]
			with SetDefaultSmallGroupSetName
			with CreateSmallGroupSetPermissions
			with CreateSmallGroupSetDescription
			with ModifySmallGroupSetValidation
			with ModifySmallGroupSetsScheduledNotifications
			with AutowiringSmallGroupServiceComponent
			with AutowiringAssessmentMembershipServiceComponent
			with GeneratesDefaultWeekRangesWithTermService
			with AutowiringTermServiceComponent

	def edit(module: Module, set: SmallGroupSet): Command =
		new EditSmallGroupSetCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with EditSmallGroupSetPermissions
			with EditSmallGroupSetDescription
			with ModifySmallGroupSetValidation
			with ModifySmallGroupSetsScheduledNotifications
			with AutowiringSmallGroupServiceComponent
}

trait ModifySmallGroupSetCommandState extends CurrentSITSAcademicYear {
	def module: Module
	def existingSet: Option[SmallGroupSet]

	def updatingExistingLink(set: SmallGroupSet): Boolean =
		(allocationMethod == SmallGroupAllocationMethod.Linked
		&& set.allocationMethod == SmallGroupAllocationMethod.Linked
		&& linkedDepartmentSmallGroupSet != set.linkedDepartmentSmallGroupSet)

	def creatingNewLink(set: SmallGroupSet): Boolean =
		(allocationMethod == SmallGroupAllocationMethod.Linked
		&& set.allocationMethod != SmallGroupAllocationMethod.Linked)

	var name: String = _

	var format: SmallGroupFormat = _

	var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual

	var allowSelfGroupSwitching: Boolean = true
	var studentsCanSeeTutorName: Boolean = false
	var studentsCanSeeOtherMembers: Boolean = false

	var collectAttendance: Boolean = true

	var linkedDepartmentSmallGroupSet: DepartmentSmallGroupSet = _
}

trait CreateSmallGroupSetCommandState extends ModifySmallGroupSetCommandState {
	val existingSet = None
}

trait EditSmallGroupSetCommandState extends ModifySmallGroupSetCommandState {
	def set: SmallGroupSet
	def existingSet = Some(set)
}

class CreateSmallGroupSetCommandInternal(val module: Module) extends ModifySmallGroupSetCommandInternal with CreateSmallGroupSetCommandState {
	self: SmallGroupServiceComponent with AssessmentMembershipServiceComponent with GeneratesDefaultWeekRanges =>

	override def applyInternal(): SmallGroupSet = transactional() {
		val set = new SmallGroupSet(module)

		// TAB-2541 By default, new sets should have default week ranges
		set.defaultWeekRanges = defaultWeekRanges(academicYear)

		copyTo(set)

		if (set.allocationMethod == SmallGroupAllocationMethod.Linked) {
			Option(set.linkedDepartmentSmallGroupSet).foreach { linkedSet =>
				linkedSet.groups.asScala.foreach { linkedGroup =>
					val smallGroup = new SmallGroup(set)
					smallGroup.name = linkedGroup.name
					smallGroup.linkedDepartmentSmallGroup = linkedGroup
					set.groups.add(smallGroup)
				}
			}
		} else {
			// TAB-2535 Automatically link to any available upstream groups
			for {
				ua <- assessmentMembershipService.getAssessmentComponents(module)
				uag <- assessmentMembershipService.getUpstreamAssessmentGroups(ua, academicYear)
			} {
				val ag = new AssessmentGroup
				ag.assessmentComponent = ua
				ag.occurrence = uag.occurrence
				ag.smallGroupSet = set
				set.assessmentGroups.add(ag)
			}
		}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait GeneratesDefaultWeekRanges {
	def defaultWeekRanges(year: AcademicYear): Seq[WeekRange]
}

trait GeneratesDefaultWeekRangesWithTermService extends GeneratesDefaultWeekRanges {
	self: TermServiceComponent =>

	def defaultWeekRanges(year: AcademicYear): Seq[WeekRange] = {
		val weeks = termService.getAcademicWeeksForYear(year.dateInTermOne).toMap

		val startingWeekNumbers =
			weeks
				.map { case (weekNumber, dates) =>
					(weekNumber, termService.getTermFromAcademicWeekIncludingVacations(weekNumber, year))
				}
				.filterNot { case (_, term) => term.getTermType == null } // Remove vacations - can't do this above as mins would be wrong
				.groupBy { _._2.getTermType() }
				.map { case (termType, weekNumbersAndTerms) => (termType, weekNumbersAndTerms.keys.min) } // Map to minimum week number

		Seq(
			WeekRange(startingWeekNumbers(TermType.autumn), startingWeekNumbers(TermType.autumn) + 9), // Autumn term
			WeekRange(startingWeekNumbers(TermType.spring), startingWeekNumbers(TermType.spring) + 9), // Spring term
			WeekRange(startingWeekNumbers(TermType.summer), startingWeekNumbers(TermType.summer) + 4) // Summer term - only first 5 weeks
		)
	}
}

trait SetDefaultSmallGroupSetName extends BindListener {
	self: CreateSmallGroupSetCommandState =>

	override def onBind(result: BindingResult) {
		// If we haven't set a name, make one up
		if (!name.hasText) {
			Option(format).foreach { format => name = "%s %ss".format(module.code.toUpperCase, format) }
		}
	}
}

class EditSmallGroupSetCommandInternal(val module: Module, val set: SmallGroupSet) extends ModifySmallGroupSetCommandInternal with EditSmallGroupSetCommandState {
	self: SmallGroupServiceComponent =>

	copyFrom(set)

	override def applyInternal(): SmallGroupSet = transactional() {
		if (updatingExistingLink(set) || creatingNewLink(set)) {
			copyTo(set)

			set.groups.asScala.foreach(_.preDelete())

			set.groups.clear()
			linkedDepartmentSmallGroupSet.groups.asScala.foreach { linkedGroup =>
				val smallGroup = new SmallGroup(set)
				smallGroup.name = linkedGroup.name
				smallGroup.linkedDepartmentSmallGroup = linkedGroup
				set.groups.add(smallGroup)
			}
		} else {
			copyTo(set)
		}

		smallGroupService.saveOrUpdate(set)
		set
	}
}

abstract class ModifySmallGroupSetCommandInternal extends CommandInternal[SmallGroupSet] with ModifySmallGroupSetCommandState {
	def copyFrom(set: SmallGroupSet) {
		name = set.name
		academicYear = set.academicYear
		format = set.format
		allocationMethod = set.allocationMethod
		allowSelfGroupSwitching = set.allowSelfGroupSwitching
		studentsCanSeeTutorName = set.studentsCanSeeTutorName
		studentsCanSeeOtherMembers = set.studentsCanSeeOtherMembers
		collectAttendance = set.collectAttendance
		linkedDepartmentSmallGroupSet = set.linkedDepartmentSmallGroupSet
	}

	def copyTo(set: SmallGroupSet) {
		set.name = name
		set.academicYear = academicYear
		set.format = format
		set.allocationMethod = allocationMethod
		set.collectAttendance = collectAttendance

		set.allowSelfGroupSwitching = allowSelfGroupSwitching
		set.studentsCanSeeOtherMembers = studentsCanSeeOtherMembers
		set.studentsCanSeeTutorName = studentsCanSeeTutorName

		if (allocationMethod == SmallGroupAllocationMethod.Linked) {
			set.linkedDepartmentSmallGroupSet = linkedDepartmentSmallGroupSet
		} else if (set.linkedDepartmentSmallGroupSet != null) {
			// TAB-4032 Unset linked state
			set.linkedDepartmentSmallGroupSet = null
			set.groups.asScala.foreach { _.linkedDepartmentSmallGroup = null }
		}
	}
}

trait ModifySmallGroupSetValidation extends SelfValidating {
	self: ModifySmallGroupSetCommandState with SmallGroupServiceComponent =>

	override def validate(errors: Errors) {
		if (!name.hasText) errors.rejectValue("name", "smallGroupSet.name.NotEmpty")
		else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroupSet.name.Length", Array[Object](200: JInteger), "")

		if (format == null) errors.rejectValue("format", "smallGroupSet.format.NotEmpty")
		if (allocationMethod == null) errors.rejectValue("allocationMethod", "smallGroupSet.allocationMethod.NotEmpty")
		else if (allocationMethod == SmallGroupAllocationMethod.Linked && linkedDepartmentSmallGroupSet == null)
			errors.rejectValue("linkedDepartmentSmallGroupSet", "smallGroupSet.allocationMethod.Linked.NotEmpty")

		existingSet.foreach { set =>
			if (academicYear != set.academicYear) errors.rejectValue("academicYear", "smallGroupSet.academicYear.cantBeChanged")

			if ((updatingExistingLink(set) || creatingNewLink(set)) && hasAttendance(set)
				&& hasAttendance(set)) {
				errors.rejectValue("allocationMethod", "smallGroupEvent.allocationMethod.hasAttendance")
			}

			if (set.releasedToStudents || set.releasedToTutors) {
				if ((set.linked && allocationMethod != SmallGroupAllocationMethod.Linked) || (allocationMethod == SmallGroupAllocationMethod.Linked && !set.linked)) {
					// Can't unlink or link a released set
					errors.rejectValue("allocationMethod", "smallGroupSet.allocationMethod.released")
				}	else if (set.linked && set.linkedDepartmentSmallGroupSet != linkedDepartmentSmallGroupSet) {
					// Can't change the link of a released set
					errors.rejectValue("allocationMethod", "smallGroupSet.allocationMethod.released")
				}
			}
		}
	}

	def hasAttendance(set: SmallGroupSet): Boolean = {
		set.groups.asScala.exists (
			group => group.events.exists { event =>
				smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
					.exists { _.attendance.asScala.exists { attendance =>
					attendance.state != AttendanceState.NotRecorded
				}
			}}
		)
	}

}

trait CreateSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(module))
	}
}

trait CreateSmallGroupSetDescription extends Describable[SmallGroupSet] {
	self: CreateSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.module(module).properties("name" -> name)
	}

	override def describeResult(d: Description, set: SmallGroupSet): Unit =
		d.smallGroupSet(set)
}

trait EditSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupSetDescription extends Describable[SmallGroupSet] {
	self: EditSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}

trait ModifySmallGroupSetsScheduledNotifications
	extends SchedulesNotifications[SmallGroupSet, SmallGroupEventOccurrence] with GeneratesNotificationsForSmallGroupEventOccurrence {

	self: SmallGroupServiceComponent with ModifySmallGroupSetCommandState =>

	override def transformResult(set: SmallGroupSet): Seq[SmallGroupEventOccurrence] =
		// get all the occurrences (even the ones in invalid weeks) so they can be cleared
		set.groups.asScala.flatMap(_.events.flatMap(smallGroupService.getOrCreateSmallGroupEventOccurrences))

	override def scheduledNotifications(occurrence: SmallGroupEventOccurrence): Seq[ScheduledNotification[_]] = {
		if (allocationMethod == SmallGroupAllocationMethod.Linked) generateNotifications(occurrence)
		else Nil // The set commands spawned will hande notification creation for events
	}
}