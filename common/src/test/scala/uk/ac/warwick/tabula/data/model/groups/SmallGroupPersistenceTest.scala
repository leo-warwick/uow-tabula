package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.data.model.{MapLocation, NamedLocation}
import uk.ac.warwick.tabula.{AcademicYear, FieldAccessByReflection, PersistenceTestBase}
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import org.springframework.transaction.annotation.Transactional
import org.joda.time.LocalTime

import scala.jdk.CollectionConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.ModuleDaoImpl
import org.junit.Before
import uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethod.Hybrid
import uk.ac.warwick.userlookup.User

class SmallGroupPersistenceTest extends PersistenceTestBase with FieldAccessByReflection {

  val moduleDao = new ModuleDaoImpl
  val service: ModuleAndDepartmentService = new ModuleAndDepartmentService

  @Before
  def setup(): Unit = {
    moduleDao.sessionFactory = sessionFactory
    service.moduleDao = moduleDao
  }

  @Test def nullSettingsAreReplacedWithEmptyMap(): Unit = {
    transactional { ts =>
      val cs108 = service.getModuleByCode("cs108").get
      val set = new SmallGroupSet(cs108)
      set.format = SmallGroupFormat.Lab
      set.name = "test"
      set.setV("settings", null)

      set.getV("settings") == null should be(true)
      session.save(set)

      val id = set.id

      session.flush()
      session.clear()

      session.load(classOf[SmallGroupSet], id) match {
        case loadedSet: SmallGroupSet => (loadedSet.getV("settings") == null) should be(false)
        case _ => fail("Set not found")
      }

    }
  }

  @Transactional
  @Test def itWorks: Unit = {
    // Use data from data.sql
    {
      val cs108 = service.getModuleByCode("cs108").get

      val set1 = new SmallGroupSet(cs108)
      set1.name = "Seminar groups"
      set1.format = SmallGroupFormat.Seminar

      cs108.groupSets.add(set1)
      session.saveOrUpdate(cs108)

      val group1 = new SmallGroup(set1)
      group1.name = "Group 1"

      set1.groups.add(group1)
      session.saveOrUpdate(set1)
      group1.students.add(new User("cuscav"))
      session.saveOrUpdate(group1)

      val event1 = new SmallGroupEvent(group1)
      event1.deliveryMethod = Hybrid
      event1.title = "Event 1"
      event1.location = NamedLocation("A0.01")
      event1.weekRanges = Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)) // Weeks 1, 2, 3, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24
      event1.day = DayOfWeek.Tuesday
      event1.startTime = new LocalTime(15, 0) // 3pm
      event1.endTime = event1.startTime.plusHours(1) // 4pm

      val event2 = new SmallGroupEvent(group1)
      event2.deliveryMethod = Hybrid
      event2.title = "Event 2"
      event2.location = MapLocation("A0.01", "12345")
      event2.weekRanges = Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)) // Weeks 1, 2, 3, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24
      event2.day = DayOfWeek.Thursday
      event2.startTime = new LocalTime(13, 0) // 1pm
      event2.endTime = event2.startTime.plusHours(1) // 2pm

      group1.addEvent(event1)
      group1.addEvent(event2)

      session.saveOrUpdate(group1)

      event1.tutors.knownType.addUserId("cusebr")
      event2.tutors.knownType.addUserId("cusfal")

      session.saveOrUpdate(event1)
      session.saveOrUpdate(event2)

      session.flush()
      session.clear()
    }

    val cs108 = service.getModuleByCode("cs108").get
    val set1 = cs108.groupSets.get(0)
    val group1 = set1.groups.get(0)
    val event1 = group1.events.find(_.title == "Event 1").head
    val event2 = group1.events.find(_.title == "Event 2").head

    // Check a few choice fields to check they're persisted right
    set1.format should be(SmallGroupFormat.Seminar)
    set1.academicYear should be(AcademicYear.now())

    group1.name should be("Group 1")

    event1.day should be(DayOfWeek.Tuesday)
    event1.startTime should be(new LocalTime(15, 0))
    event1.endTime should be(new LocalTime(16, 0))
    event1.weekRanges should be(Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)))

    event2.day should be(DayOfWeek.Thursday)
    event2.startTime should be(new LocalTime(13, 0))
    event2.endTime should be(new LocalTime(14, 0))
    event2.weekRanges should be(Seq(WeekRange(1, 3), WeekRange(6), WeekRange(16, 24)))
  }

}
