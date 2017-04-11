package uk.ac.warwick.tabula.commands.groups

import org.hibernate.validator.constraints.NotEmpty
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.SearchStudentsInSmallGroupSetCommand._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventOccurrence, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.{Member, Module}
import uk.ac.warwick.tabula.permissions.{CheckablePermission, Permissions}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSmallGroupServiceComponent, ProfileServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.collection.mutable

object SearchStudentsInSmallGroupSetCommand {
	def apply(module: Module, set: SmallGroupSet) =
		new SearchStudentsInSmallGroupSetCommandInternal(module, set)
			with ComposableCommand[Seq[Member]]
			with SearchStudentsInSmallGroupSetPermissions
			with AutowiringProfileServiceComponent
			with AutowiringSmallGroupServiceComponent
			with ReadOnly with Unaudited

	/** The minimum length of the whole query */
	val MinimumQueryLength = 3

	/** The minimum length of at least one term in the query, avoids searches for "m m m" getting through */
	val MinimumTermLength = 2
}

trait SearchStudentsInSmallGroupSetCommandState {
	def module: Module
	def set: SmallGroupSet

	@NotEmpty(message = "{NotEmpty.profiles.searchQuery}")
	var query: String = _

	var excludeEvent: SmallGroupEvent = _
	var excludeWeek: JInteger = _

	def validQuery: Boolean =
		(query.trim().length >= MinimumQueryLength) &&
		query.split( """\s+""").exists(_.length >= MinimumTermLength)
}

class SearchStudentsInSmallGroupSetCommandInternal(val module: Module, val set: SmallGroupSet) extends CommandInternal[Seq[Member]] with SearchStudentsInSmallGroupSetCommandState {
	self: ProfileServiceComponent with SmallGroupServiceComponent =>

	lazy val excludedEventOccurrence: Option[SmallGroupEventOccurrence] =
		transactional() {
			if (Option(excludeEvent).isDefined && Option(excludeWeek).isDefined) {
				smallGroupService.getOrCreateSmallGroupEventOccurrence(excludeEvent, excludeWeek)
			} else {
				None
			}
		}

	def allUniversityIdsInSet: mutable.Buffer[String] = {
		// Include the university IDs of any users in any SmallGroupSet within this module for the relevant academic year
		module.groupSets.asScala.filter(_.academicYear == set.academicYear).flatMap { set =>
			set.members.knownType.members ++ set.groups.asScala.flatMap { group =>
				group.students.users.map(_.getWarwickId) ++
					smallGroupService.findAttendanceByGroup(group).flatMap(_.attendance.asScala.toSeq.map(_.universityId))
			}
		}.distinct
	}

	def members: Seq[Member] = {
		val allUniversityIds = allUniversityIdsInSet
		val excludedUniversityIds = excludedEventOccurrence.map { occurrence =>
			occurrence.event.group.students.users.map { _.getWarwickId } ++ occurrence.attendance.asScala.toSeq.map { _.universityId }
		}.getOrElse(Nil)

		profileService.getAllMembersWithUniversityIds(allUniversityIds diff excludedUniversityIds)
	}

	override def applyInternal(): Seq[Member] = {
		if (validQuery) {
			val terms = query.split("""\s+""").map { _.trim().toLowerCase }
			members.filter { member =>
				terms.forall { term =>
					member.fullName.fold(false) { _.toLowerCase.contains(term) }
				}
			}.sortBy { member => (member.lastName, member.firstName, member.universityId) }
		} else Nil
	}

}

trait SearchStudentsInSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: SearchStudentsInSmallGroupSetCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(mandatory(set), mandatory(module))
		p.PermissionCheckAny(
			Seq(CheckablePermission(Permissions.SmallGroupEvents.ViewRegister, mandatory(set))) ++
				mandatory(set).groups.asScala.map{group => CheckablePermission(Permissions.SmallGroupEvents.ViewRegister, group)}
		)
	}
}