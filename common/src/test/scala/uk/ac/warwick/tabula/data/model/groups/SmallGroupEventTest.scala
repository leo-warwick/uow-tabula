package uk.ac.warwick.tabula.data.model.groups

import org.hibernate.{Session, SessionFactory}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.data.model.{NamedLocation, UserGroup}

class SmallGroupEventTest extends TestBase with Mockito{

	val mockSessionFactory: SessionFactory = smartMock[SessionFactory]
	val mockSession: Session = smartMock[Session]
	mockSessionFactory.getCurrentSession returns mockSession
	mockSessionFactory.openSession() returns mockSession

  @Test
  def duplicate(){
    val source = new SmallGroupEvent
    source.id = "testId"
    source.day = DayOfWeek.Monday
    source.endTime = new LocalTime(13,0,0,0)
    source.group = new SmallGroup
    source.location = NamedLocation("test")
    source.permissionsService = mock[PermissionsService]
    source.startTime = new LocalTime(12,0,0,0)
    source.title= "test title"
    source.tutors = UserGroup.ofUsercodes
		source.tutors.asInstanceOf[UserGroup].sessionFactory = mockSessionFactory
    source.weekRanges = Seq(WeekRange(1))

    val targetGroup = new SmallGroup()
    val clone = source.duplicateTo(targetGroup, transient = true)

    clone.id should be(null) // Don't duplicate IDs
    clone.day should be (source.day)
    clone.endTime should be(source.endTime)
    clone.group should be(targetGroup)
    clone.location should be(source.location)
    clone.permissionsService should be(source.permissionsService)
    clone.startTime should be(source.startTime)
    clone.title should be(source.title)
    clone.tutors should not be source.tutors // just check they're different instances
    clone.weekRanges should be(source.weekRanges)


  }

}
