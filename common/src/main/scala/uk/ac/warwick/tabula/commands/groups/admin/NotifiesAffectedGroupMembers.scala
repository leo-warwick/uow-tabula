package uk.ac.warwick.tabula.commands.groups.admin

import java.util

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.data.model.notifications.groups.{SmallGroupSetChangedNotification, SmallGroupSetChangedStudentNotification, SmallGroupSetChangedTutorNotification}
import uk.ac.warwick.tabula.data.model.{Notification, NotificationPriority}

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.Notifies
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.services.UserLookupComponent

trait SmallGroupSetCommand {
	def set: SmallGroupSet
	def apparentUser: User
}

trait NotifiesAffectedGroupMembers extends Notifies[SmallGroupSet, SmallGroupSet] {
	this: SmallGroupSetCommand with UserLookupComponent =>

	val setBeforeUpdates: SmallGroupSet = set.duplicateTo(transient = false)

	def hasAffectedStudentsGroups(student: User): Boolean = {
		val previousMembership = setBeforeUpdates.groups.asScala.find(_.students.users.contains(student))
		val currentMembership = set.groups.asScala.find(_.students.users.contains(student))
		// notify if the student's group membership has changed, or
		// if the events have changed on the student's group
		(previousMembership != currentMembership) || !previousMembership.get.hasEquivalentEventsTo(currentMembership.get)
	}

	def hasAffectedTutorsEvents(tutor: User): Boolean = {
		// can't use group.hasEquivalentEventsTo, because there might be a change to an event which this user is not a tutor of
		// - so comparisons have to be at the event level rather than the group level
		val previousEvents = setBeforeUpdates.groups.asScala.flatMap(_.events).filter(_.tutors.users.contains(tutor))
		val currentEvents = set.groups.asScala.flatMap(_.events).filter(_.tutors.users.contains(tutor))
		val noEventsRemoved = previousEvents.forall(pe => currentEvents.exists(_.isEquivalentTo(pe)))
		val noEventsAdded = currentEvents.forall(ce => previousEvents.exists(_.isEquivalentTo(ce)))
		if (noEventsRemoved && noEventsAdded) {
			// no events for this tutor have changed (therefore no groups relevant to this tutor can have changed), but the allocations might have
			val previousGroups = setBeforeUpdates.groups.asScala.filter(_.events.exists(_.tutors.users.contains(tutor)))
			val currentGroups = set.groups.asScala.filter(_.events.exists(_.tutors.users.contains(tutor)))
			val allocationsUnchanged = previousGroups.forall(pg => currentGroups.exists(cg => cg == pg && cg.students.hasSameMembersAs(pg.students)))
			!allocationsUnchanged
		} else {
			true
		}
	}

	/**
	 * Just the groups in this set that are applicable to this tutor.
	 */
	def tutorsEvents(set: SmallGroupSet, tutor: User): SmallGroupSet = {
		val clone = set.duplicateTo(transient = false)
		for (clonedGroup <- clone.groups.asScala) {
			clonedGroup.events.filterNot(_.tutors.users.contains(tutor)).foreach(clonedGroup.removeEvent)
		}
		clone.groups = clone.groups.asScala.filterNot(_.events.isEmpty).asJava
		clone
	}

	/**
	 * Just the groups in this set that are applicable to this student.
	 */
	def studentsEvents(set: SmallGroupSet, student: User): util.List[SmallGroup] = {
		set.groups.asScala.filter(_.students.users.contains(student)).asJava
	}

	def createTutorNotification(tutor: User): Option[Notification[SmallGroup, SmallGroupSet]] = {
		val filteredGroupSet = tutorsEvents(set, tutor)
		createNotification(set, filteredGroupSet.groups.asScala, tutor, new SmallGroupSetChangedTutorNotification, set.emailTutorsOnChange)
	}

	def createStudentNotification(student: User): Option[Notification[SmallGroup, SmallGroupSet]] = {
		val filteredGroups = studentsEvents(set, student)
		createNotification(set, filteredGroups.asScala, student, new SmallGroupSetChangedStudentNotification, set.emailStudentsOnChange)
	}

	def createNotification(set: SmallGroupSet, filteredGroups: Seq[SmallGroup], user: User, blankNotification: SmallGroupSetChangedNotification, sendEmail: Boolean): Option[SmallGroupSetChangedNotification] = {
		filteredGroups.toSeq match {
			case Nil => None
			case groups =>
				val n = Notification.init(blankNotification, apparentUser, groups, groups.head.groupSet)
				n.recipientUserId = user.getUserId
				n.oldSmallGroupSizes.value = setBeforeUpdates.groups.asScala.map { group =>
					group.id -> group.students.size.toString
				}.toMap
				if (!sendEmail) n.priority = NotificationPriority.Trivial
				Some(n)
		}
	}

	def emit(set: SmallGroupSet): Seq[Notification[SmallGroup, SmallGroupSet]] = {
		val tutorNotifications = if (set.releasedToTutors) {
			val allEvents = (setBeforeUpdates.groups.asScala ++ set.groups.asScala).flatMap(g => g.events)
			val allTutors = allEvents.flatMap(e => e.tutors.users).distinct
			val affectedTutors = allTutors.filter(hasAffectedTutorsEvents)
			affectedTutors.flatMap(createTutorNotification)
		} else {
			Nil
		}

		val studentNotifications = if (set.releasedToStudents) {
			val allStudents = (setBeforeUpdates.groups.asScala ++ set.groups.asScala).flatMap(g => g.students.users).distinct
			val affectedStudents = allStudents.filter(hasAffectedStudentsGroups)
			affectedStudents.flatMap(createStudentNotification)
		} else {
			Nil
		}
		studentNotifications ++ tutorNotifications
	}

}
