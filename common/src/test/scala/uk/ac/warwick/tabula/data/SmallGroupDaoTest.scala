package uk.ac.warwick.tabula.data


import org.joda.time.LocalTime
import org.junit.Before
import uk.ac.warwick.tabula.data.model.{Module, UserGroup}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, MockUserLookup, PersistenceTestBase}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.userlookup.User

class SmallGroupDaoTest extends PersistenceTestBase {

	val smallGroupDao = new SmallGroupDaoImpl
	val moduleDao = new ModuleDaoImpl
	val module: Module = Fixtures.module("kt123","Kinesthetic Teaching")
	val moduleWithNoGroups: Module = Fixtures.module("ab123", "No Groups Here")
	val smallGroupSet: SmallGroupSet = Fixtures.smallGroupSet("Test Small Group Set")
	val smallGroup: SmallGroup = Fixtures.smallGroup("Test Small Group")
	val mockUserLookup = new MockUserLookup
	mockUserLookup.registerUserObjects(new User("cusfal"), new User("cuscao"))

	@Before
	def setup() {
		smallGroupDao.sessionFactory = sessionFactory
		smallGroupDao.userLookup = mockUserLookup
		moduleDao.sessionFactory = sessionFactory
		smallGroupSet.academicYear = new AcademicYear(2013)
		smallGroupSet.format = SmallGroupFormat.Seminar
		smallGroupSet.module = module

		smallGroup.groupSet = smallGroupSet
		smallGroupSet.groups.add(smallGroup)

	}

	@Test def findByModuleAndYear(): Unit = transactional { tx =>
		moduleDao.saveOrUpdate(module)
		session.flush()
		smallGroupDao.saveOrUpdate(smallGroupSet)
		session.flush()
		smallGroupDao.saveOrUpdate(smallGroup)
		session.flush()

		smallGroupDao.findByModuleAndYear(module, AcademicYear(2013)) should be (Seq(smallGroup))
	}

	@Test def smallGroups(): Unit = transactional { tx =>
		moduleDao.saveOrUpdate(module)
		moduleDao.saveOrUpdate(moduleWithNoGroups)
		session.flush()
		smallGroupDao.saveOrUpdate(smallGroupSet)
		session.flush()
		smallGroupDao.saveOrUpdate(smallGroup)
		session.flush()

		smallGroupDao.hasSmallGroups(module) should be(true)
		smallGroupDao.hasSmallGroups(moduleWithNoGroups) should be(false)
	}

	@Test def listSmallGroupEventsForReport(): Unit = transactional { tx =>
		val department = Fixtures.department("its")

		module.adminDepartment = department

		smallGroup.students = UserGroup.ofUsercodes
		smallGroup.students.add(new User("cusfal"))
		smallGroup.students.add(new User("cuscao"))

		val event1 = Fixtures.smallGroupEvent("event1")
		event1.group = smallGroup
		event1.day = DayOfWeek.Monday
		event1.startTime = new LocalTime(12, 30)
		event1.endTime = new LocalTime(12, 45, 16)
		event1.weekRanges = Seq(WeekRange(1,5), WeekRange(10,15))
		event1.tutors.asInstanceOf[UserGroup].userLookup = mockUserLookup
		event1.tutors.add(new User("cusfal"))
		smallGroup.addEvent(event1)

		session.save(department)
		session.save(module)
		session.save(smallGroupSet)
		session.save(smallGroup)
		session.save(event1)

		val departmentSmallGroupSet = Fixtures.departmentSmallGroupSet("Department group set")
		val departmentSmallGroup = Fixtures.departmentSmallGroup("Department group")
		departmentSmallGroup.groupSet = departmentSmallGroupSet
		departmentSmallGroup.students = UserGroup.ofUsercodes
		departmentSmallGroup.students.add(new User("cusfal"))
		val linkedSmallGroupSet = Fixtures.smallGroupSet("Linked Small Group Set")
		linkedSmallGroupSet.academicYear = smallGroupSet.academicYear
		linkedSmallGroupSet.module = module
		linkedSmallGroupSet.format = SmallGroupFormat.Seminar
		val linkedSmallGroup = Fixtures.smallGroup("Linked Small Group")
		linkedSmallGroup.groupSet = linkedSmallGroupSet
		linkedSmallGroupSet.groups.add(linkedSmallGroup)
		val linkedSmallGroupEvent = Fixtures.smallGroupEvent("event2")
		linkedSmallGroupEvent.group = linkedSmallGroup
		linkedSmallGroup.addEvent(linkedSmallGroupEvent)
		linkedSmallGroup.linkedDepartmentSmallGroup = departmentSmallGroup
		linkedSmallGroup.students.add(new User("cusfal"))
		linkedSmallGroup.students.asInstanceOf[UserGroup].userLookup = mockUserLookup

		session.save(departmentSmallGroupSet)
		session.save(departmentSmallGroup)
		session.save(linkedSmallGroupSet)
		session.save(linkedSmallGroup)
		session.save(linkedSmallGroupEvent)

		val data = smallGroupDao.listSmallGroupEventsForReport(department, smallGroupSet.academicYear)
		data.size should be (2)
		val event1Data = data.find(_.eventName.contains("event1")).get
		val event2Data = data.find(_.eventName.contains("event2")).get
		def checkData(event: SmallGroupEvent, data: SmallGroupEventReportData): Unit = {
			data.departmentName should be (event.group.groupSet.module.adminDepartment.name)
			data.eventName should be (Seq(Option(event.group.groupSet.name), Option(event.group.name), Option(event.title)).flatten.mkString(" - "))
			data.moduleTitle should be (event.group.groupSet.module.name)
			data.day should be (Option(event.day).map(_.name).getOrElse(""))
			data.start should be (Option(event.startTime).map(_.toString("HH:mm")).getOrElse(""))
			data.finish should be (Option(event.endTime).map(_.toString("HH:mm")).getOrElse(""))
			data.location should be (Option(event.location).map(_.name).getOrElse(""))
			data.size should be (event.group.students.size)
			data.weeks should be (event.weekRanges.mkString(", "))
			data.staff should be (event.tutors.users.map(u => s"${u.getFullName} (${u.getUserId})").mkString(", "))
		}
		checkData(event1, event1Data)
		checkData(linkedSmallGroupEvent, event2Data)
	}

}
