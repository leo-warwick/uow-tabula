package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.notifications.groups.OpenSmallGroupSetsStudentSignUpNotification
import uk.ac.warwick.tabula.events.NotificationHandling
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.StudentSignUp
import collection.JavaConverters._

object AllocateSelfToGroupCommand{
	def apply(user:User, groupSet:SmallGroupSet): AllocateSelfToGroupCommand with ComposableCommand[SmallGroupSet] with StudentSignupCommandPermissions with StudentSignUpCommandDescription with AllocateSelfToGroupValidator with AllocateSelfToGroupNotificationCompletion = {
		new AllocateSelfToGroupCommand(user, groupSet)
			with ComposableCommand[SmallGroupSet]
			with StudentSignupCommandPermissions
			with StudentSignUpCommandDescription
		  with AllocateSelfToGroupValidator
			with AllocateSelfToGroupNotificationCompletion
	}
}
object DeallocateSelfFromGroupCommand{
	def apply(user:User, groupSet:SmallGroupSet): DeallocateSelfFromGroupCommand with ComposableCommand[SmallGroupSet] with StudentSignupCommandPermissions with StudentSignUpCommandDescription with DeallocateSelfFromGroupValidator = {
		new DeallocateSelfFromGroupCommand(user, groupSet)
			with ComposableCommand[SmallGroupSet]
			with StudentSignupCommandPermissions
			with StudentSignUpCommandDescription
		  with DeallocateSelfFromGroupValidator
	}
}

trait AllocateSelfToGroupValidator extends SelfValidating {
	this:StudentSignUpCommandState=>
		override def validate(errors: Errors){
			if (group == null) {
				errors.reject("NotEmpty")
			} else {
				if (group.isFull){
					errors.reject("smallGroup.full")
				}
				if (!group.groupSet.openForSignups){
					errors.reject("smallGroup.closed")
				}
				if (!(group.groupSet.allocationMethod == StudentSignUp)){
					errors.reject("smallGroup.notStudentSignUp")
				}
			}
		}
}

trait DeallocateSelfFromGroupValidator extends SelfValidating {
	this:StudentSignUpCommandState=>
	override def validate(errors: Errors){
		if (group == null) {
			errors.reject("NotEmpty")
		} else {
			if (!group.groupSet.openForSignups){
				errors.reject("smallGroup.closed")
			}
			if (!(group.groupSet.allocationMethod == StudentSignUp)){
				errors.reject("smallGroup.notStudentSignUp")
			}
			if (!group.groupSet.allowSelfGroupSwitching){
				errors.reject("smallGroup.noSwitching")
			}
		}
	}

}

trait StudentSignUpCommandState{
	val user: User
	val groupSet: SmallGroupSet
	var group: SmallGroup =_
}
class AllocateSelfToGroupCommand(val user:User, val groupSet:SmallGroupSet) extends CommandInternal[SmallGroupSet] with StudentSignUpCommandState{

	def applyInternal(): SmallGroupSet = {
		group.students.add(user)
		group.groupSet
	}
}
class DeallocateSelfFromGroupCommand(val user:User, val groupSet:SmallGroupSet) extends CommandInternal[SmallGroupSet] with StudentSignUpCommandState{

	def applyInternal(): SmallGroupSet = {
		group.students.remove(user)
		group.groupSet
	}
}


trait StudentSignUpCommandDescription extends Describable[SmallGroupSet]{
	this:StudentSignUpCommandState=>
	def describe(d: Description) {
		d.smallGroup(group)
	}
}

trait StudentSignupCommandPermissions extends RequiresPermissionsChecking{
	this:StudentSignUpCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		// n.b. have to use the groupset here, as this code will be called before the group is bound. Fortunately we know the groupset at construction time
		p.PermissionCheck(Permissions.SmallGroups.AllocateSelf,groupSet)

	}
}

trait AllocateSelfToGroupNotificationCompletion extends CompletesNotifications[SmallGroupSet] {

	self: StudentSignUpCommandState with NotificationHandling =>

	def notificationsToComplete(commandResult: SmallGroupSet): CompletesNotificationsResult = {
		val notifications = notificationService.findActionRequiredNotificationsByEntityAndType[OpenSmallGroupSetsStudentSignUpNotification](groupSet)
		def needsSignUp(set: SmallGroupSet) = {
			set.allStudents.contains(user) && !set.groups.asScala.exists(_.students.includesUser(user))
		}
		val notificationsToClear = notifications.filter(_.isRecipient(user)).filter(n =>
			n.notificationItems.asScala.forall(entityRef =>
				entityRef.entity match {
					case set: SmallGroupSet =>
						!needsSignUp(set)
					case _ =>
						false
				}
			)
		)
		CompletesNotificationsResult(notificationsToClear, user)
	}

}
