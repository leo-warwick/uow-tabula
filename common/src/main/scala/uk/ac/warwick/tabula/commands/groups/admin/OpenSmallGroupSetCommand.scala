package uk.ac.warwick.tabula.commands.groups.admin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Notification}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSetSelfSignUpState._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupAllocationMethod, SmallGroupSet, SmallGroupSetSelfSignUpState}
import uk.ac.warwick.tabula.data.model.notifications.groups.{OpenSmallGroupSetsOtherSignUpNotification, OpenSmallGroupSetsStudentSignUpNotification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User


object OpenSmallGroupSetCommand {
	def apply(department: Department, setsToUpdate: Seq[SmallGroupSet], user: User, desiredState: SmallGroupSetSelfSignUpState): OpenSmallGroupSet with ComposableCommand[Seq[SmallGroupSet]] with OpenSmallGroupSetPermissions with OpenSmallGroupSetAudit = desiredState match {


		case Open => new OpenSmallGroupSet(department, setsToUpdate, user, desiredState)
											with ComposableCommand[Seq[SmallGroupSet]]
											with OpenSmallGroupSetPermissions
											with OpenSmallGroupSetAudit
											with OpenSmallGroupSetNotifier

		case Closed => new OpenSmallGroupSet(department, setsToUpdate, user, desiredState)
									with ComposableCommand[Seq[SmallGroupSet]]
									with OpenSmallGroupSetPermissions
									with OpenSmallGroupSetAudit
	}

}

trait OpenSmallGroupSetState {
	val department: Department
	val applicableSets: Seq[SmallGroupSet]
	// convenience value for freemarker to use when we're opening a single
	// set rather than a batch.
	def singleSetToOpen: SmallGroupSet = {
		applicableSets match {
			case h :: Nil => h
			case Nil => throw new RuntimeException("Attempted to get first group to open from an empty list")
			case _ => throw new RuntimeException("Attempted to get single group to open from a list of many")
		}
	}
}
class OpenSmallGroupSet(val department: Department, val requestedSets: Seq[SmallGroupSet], val user: User, val setState: SmallGroupSetSelfSignUpState)
	extends CommandInternal[Seq[SmallGroupSet]] with OpenSmallGroupSetState with UserAware {

	 val applicableSets: Seq[SmallGroupSet] = requestedSets.filter(s => s.allocationMethod == SmallGroupAllocationMethod.StudentSignUp && s.openState != setState)

	 def applyInternal(): Seq[SmallGroupSet] = {
		 applicableSets.foreach(s => s.openState = setState)
		 applicableSets
	 }
}

trait OpenSmallGroupSetPermissions extends RequiresPermissionsChecking {
  this: OpenSmallGroupSetState =>

	def permissionsCheck(p: PermissionsChecking) {
		if (applicableSets.isEmpty) p.PermissionCheck(Permissions.SmallGroups.Update, department)
		else applicableSets.foreach(g => p.PermissionCheck(Permissions.SmallGroups.Update, g))
	}
}

trait OpenSmallGroupSetAudit extends Describable[Seq[SmallGroupSet]] {
	this: KnowsEventName with OpenSmallGroupSetState =>
	def describe(d: Description) {
		d.smallGroupSetCollection(applicableSets)
	}
}

trait OpenSmallGroupSetNotifier extends Notifies[Seq[SmallGroupSet], Seq[SmallGroupSet]] {
	this: OpenSmallGroupSetState with UserAware =>

	def emit(sets: Seq[SmallGroupSet]): Seq[Notification[SmallGroupSet, Unit]] = {
		val allMemberships: Seq[(User,SmallGroupSet)] =
			for (set <- sets; member <- set.allStudents) yield (member, set)

		// convert the list of (student, set) pairs into a map of student->sets
		val setsPerUser: Map[User,Seq[SmallGroupSet]] = allMemberships.groupBy(_._1).map { case (k,v) => (k,v.map(_._2))}

		// convert the map into a notification per user
		setsPerUser.map { case (student, studentSets) =>
			val n = {
				if (studentSets.exists(_.allocationMethod == StudentSignUp)) {
					Notification.init(new OpenSmallGroupSetsStudentSignUpNotification, user, studentSets)
				} else {
					Notification.init(new OpenSmallGroupSetsOtherSignUpNotification, user, studentSets)
				}
			}
			n.recipientUserId = student.getUserId
			n
		}.toSeq
	}
}
