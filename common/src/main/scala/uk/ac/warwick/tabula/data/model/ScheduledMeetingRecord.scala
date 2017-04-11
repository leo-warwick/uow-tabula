package uk.ac.warwick.tabula.data.model

import javax.persistence._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.timetables.{TimetableEvent, EventOccurrence}

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("scheduled")
class ScheduledMeetingRecord extends AbstractMeetingRecord {

	def this(creator: Member, relationship: StudentRelationship) {
		this()
		this.creator = creator
		this.relationship = relationship
	}

	var missed: Boolean = false

	@Column(name="missed_reason")
	var missedReason: String = _

	def isPendingAction: Boolean = meetingDate.isBeforeNow && !missed

	@transient
	var securityService: SecurityService = Wire[SecurityService]

	def pendingActionBy(user: CurrentUser): Boolean =
		isPendingAction &&
		(user.universityId == creator.universityId ||
		 securityService.can(user, Permissions.Profiles.ScheduledMeetingRecord.Confirm, this))

	def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] = asEventOccurrence(context)

	def universityIdInRelationship(universityId: String): Boolean = {
		def isStudent = universityId == relationship.studentId
		def isAgent = relationship.agentMember.exists(_.universityId == universityId)
		isStudent || isAgent
	}
}