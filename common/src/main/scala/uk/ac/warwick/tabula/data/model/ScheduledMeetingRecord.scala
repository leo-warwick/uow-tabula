package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.Proxy
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent}

@Entity
@Proxy
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("scheduled")
class ScheduledMeetingRecord extends AbstractMeetingRecord {

  def this(creator: Member, relationships: Seq[StudentRelationship]) = {
    this()
    this.creator = creator
    this.relationships = relationships
  }

  def isPendingAction: Boolean = meetingDate.isBeforeNow && !missed

  @transient
  var securityService: SecurityService = Wire[SecurityService]

  def pendingActionBy(user: CurrentUser): Boolean =
    isPendingAction &&
      (user.universityId == creator.universityId ||
        securityService.can(user, Permissions.Profiles.ScheduledMeetingRecord.Confirm, this))

  def toEventOccurrence(context: TimetableEvent.Context): Option[EventOccurrence] = asEventOccurrence(context)

  def universityIdInRelationship(universityId: String): Boolean = participants.map(_.universityId).contains(universityId)
}
