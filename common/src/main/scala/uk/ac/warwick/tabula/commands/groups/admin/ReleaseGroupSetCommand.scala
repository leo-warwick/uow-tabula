package uk.ac.warwick.tabula.commands.groups.admin


import uk.ac.warwick.tabula.commands.{Appliable, Notifies, Command, Description}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.data.model.notifications.groups.ReleaseSmallGroupSetsNotification
import uk.ac.warwick.tabula.data.model.{NotificationPriority, Notification}
import scala.collection.JavaConverters._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.Tap._
import uk.ac.warwick.tabula.permissions.Permissions

trait ReleaseSmallGroupSetCommand extends Appliable[Seq[ReleasedSmallGroupSet]] {
	def describeOutcome:Option[String]
}
class ReleaseGroupSetCommandImpl(val groupsToPublish:Seq[SmallGroupSet], private val currentUser: User)
	extends Command[Seq[ReleasedSmallGroupSet]] with Notifies[Seq[ReleasedSmallGroupSet], Seq[SmallGroup]] with ReleaseSmallGroupSetCommand {

  var userLookup:UserLookupService = Wire.auto[UserLookupService]

  var notifyStudents: JBoolean = groupsToPublish match {
    case singleGroup::Nil => !singleGroup.releasedToStudents
    case _ => true
  }

  var notifyTutors: JBoolean = groupsToPublish match {
    case singleGroup::Nil => !singleGroup.releasedToTutors
    case _=> true
  }

	var sendEmail: JBoolean = true

  groupsToPublish.foreach(g => PermissionCheck(Permissions.SmallGroups.Update, g))

  def singleGroupToPublish:SmallGroupSet = {
    groupsToPublish match {
      case h :: Nil => h
      case Nil=>throw new RuntimeException("Attempted to get first group to publish from an empty list")
      case _ => throw new RuntimeException("Attempted to get single group to publish from a list of many")
    }
  }

	def emit(releasedSets: Seq[ReleasedSmallGroupSet]): Seq[ReleaseSmallGroupSetsNotification] = {
		val tutorNotifications =
			for {
				releasedSet <- releasedSets
				if releasedSet.releasedToTutors
				group <- releasedSet.set.groups.asScala
				event <- group.events
				tutor <- event.tutors.users
			} yield {
				val n = Notification.init(new ReleaseSmallGroupSetsNotification(), currentUser, List(group))
					.tap { _.isStudent = false }
				n.recipientUserId = tutor.getUserId
				n.priority = (if (sendEmail) NotificationPriority.Info else NotificationPriority.Trivial)
				n
			}


		val studentNotifications =
			for {
				releasedSet <- releasedSets
				if releasedSet.releasedToStudents
				group <- releasedSet.set.groups.asScala
				student <- group.students.users
			} yield {
				val n = Notification.init(new ReleaseSmallGroupSetsNotification(), currentUser, List(group))
					.tap { _.isStudent = true }
				n.recipientUserId = student.getUserId
				n.priority = (if (sendEmail) NotificationPriority.Info else NotificationPriority.Trivial)
				n
			}

		studentNotifications ++ tutorNotifications
	}

	def describeOutcome():Option[String]={
		groupsToPublish match {
			case singleGroup::Nil=>{
				val tutors = Some("tutors").filter(_ => notifyTutors.booleanValue())
				val students = Some("students").filter(_=>notifyStudents.booleanValue())
				Seq(tutors,students).flatten match {
					case Nil=>None // we've selected to notify neither staff nor students.
					case list=>{
						val updatedUsers = list.mkString(" and ").capitalize
						val moduleName = singleGroup.module.code.toUpperCase
						Some(s"$updatedUsers in <strong>${singleGroup.name} for ${moduleName}</strong> have been notified")
					}
				}
			}
			case _ => None // this function is only used when releasing a single group
		}
	}

	def describe(desc:Description ){
    desc.smallGroupSetCollection(groupsToPublish)
  }

	def applyInternal(): Seq[ReleasedSmallGroupSet] = {
		groupsToPublish.map(groupToPublish => {
			val releaseToStudents = notifyStudents && !groupToPublish.releasedToStudents
			val releaseToTutors = notifyTutors && !groupToPublish.releasedToTutors

			if (releaseToStudents) {
				groupToPublish.releasedToStudents = true
				groupToPublish.emailStudentsOnChange = sendEmail
			}

			if (releaseToTutors) {
				groupToPublish.releasedToTutors = true
				groupToPublish.emailTutorsOnChange = sendEmail
			}

			ReleasedSmallGroupSet(groupToPublish, releaseToStudents, releaseToTutors)
		})
	}
}

case class ReleasedSmallGroupSet(set: SmallGroupSet, releasedToStudents: Boolean, releasedToTutors: Boolean)