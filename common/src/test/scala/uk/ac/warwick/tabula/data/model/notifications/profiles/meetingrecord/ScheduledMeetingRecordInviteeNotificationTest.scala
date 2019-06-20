package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.userlookup.User

class ScheduledMeetingRecordInviteeNotificationTest extends TestBase with Mockito {

  // user is an admin that is scheduling a meeting
  @Test def recipient() = withUser("cuslaj", "1170836") {
    val agent = Fixtures.staff(currentUser.universityId, currentUser.userId)

    val staff = Fixtures.staff("9517535", "mctutor")
    val student = Fixtures.student()
    val relationshipType = StudentRelationshipType("tutor", "tutor", "tutor", "tutee")
    val relationship = StudentRelationship(staff, relationshipType, student, DateTime.now)

    // Scheduled by the agent
    val scheduledMeeting = new ScheduledMeetingRecord(agent, Seq(relationship))
    scheduledMeeting.title = "my meeting"
    scheduledMeeting.description = "discuss things"
    scheduledMeeting.meetingDate = DateTime.now
    scheduledMeeting.format = MeetingFormat.FaceToFace

    val notification = Notification.init(new ScheduledMeetingRecordInviteeNotification("created"), currentUser.apparentUser, scheduledMeeting)
    notification.recipients.head.getUserId should be(student.userId)
    // TAB-2489 even if the meeting is scheduled by an admin the tutor should show as the agent in the notification
    notification.content.model("actor").asInstanceOf[User].getWarwickId should be(staff.universityId)

    // if the student creates the meeting, recipient should be staff
    scheduledMeeting.creator = student
    notification.agent = student.asSsoUser
    notification.recipients.head.getUserId should be(staff.userId)
    notification.content.model("actor").asInstanceOf[User].getWarwickId should be(student.universityId)

    // if the staff schedules the meeting, recipient should be student
    scheduledMeeting.creator = staff
    notification.agent = staff.asSsoUser
    notification.recipients.head.getUserId should be(student.userId)
    notification.content.model("actor").asInstanceOf[User].getWarwickId should be(staff.universityId)
  }

  trait TitleFixture {
    val agent: StaffMember = Fixtures.staff("1234567", "tutor")
    agent.firstName = "Tutor"
    agent.lastName = "Name"

    val student: StudentMember = Fixtures.student("7654321", "student")
    student.firstName = "Student"
    student.lastName = "Name"

    val relationshipType = StudentRelationshipType("personalTutor", "tutor", "personal tutor", "personal tutee")

    val relationship: StudentRelationship = StudentRelationship(agent, relationshipType, student, DateTime.now)

    val thirdParty: StaffMember = Fixtures.staff("1122331", "3rdparty")
    thirdParty.firstName = "Third"
    thirdParty.lastName = "Party"

    val meeting = new ScheduledMeetingRecord(Fixtures.staff(), Seq(relationship))
  }

  @Test def titleEditedByTutor() {
    new TitleFixture {
      val notification: ScheduledMeetingRecordInviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification("edited"), agent.asSsoUser, meeting)
      notification.title should be("Meeting with Student Name and Tutor Name edited by Tutor Name")
      notification.titleFor(student.asSsoUser) should be("Meeting with Tutor Name edited by Tutor Name")
      notification.recipients should contain only student.asSsoUser
    }
  }

  @Test def titleEditedByStudent() {
    new TitleFixture {
      val notification: ScheduledMeetingRecordInviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification("edited"), student.asSsoUser, meeting)
      notification.title should be("Meeting with Student Name and Tutor Name edited by Student Name")
      notification.titleFor(agent.asSsoUser) should be("Meeting with Student Name edited by Student Name")
      notification.recipients should contain only agent.asSsoUser
    }
  }

  @Test def titleEditedByThirdParty() {
    new TitleFixture {
      val notification: ScheduledMeetingRecordInviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification("edited"), thirdParty.asSsoUser, meeting)
      notification.title should be("Meeting with Student Name and Tutor Name edited by Third Party")
      notification.titleFor(student.asSsoUser) should be("Meeting with Tutor Name edited by Third Party")
      notification.titleFor(agent.asSsoUser) should be("Meeting with Student Name edited by Third Party")
      notification.recipients should contain only student.asSsoUser
    }
  }

}
