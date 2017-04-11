package uk.ac.warwick.tabula.services

import org.joda.time.{DateTimeConstants, LocalDateTime, LocalTime}
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula._
import uk.ac.warwick.userlookup.User

class SmallGroupServiceTest extends TestBase with Mockito {
	trait Environment {
		val studentGroupMembershipHelper: UserGroupMembershipHelper[SmallGroup] = smartMock[UserGroupMembershipHelper[SmallGroup]]
		val departmentStudentGroupMembershipHelper: UserGroupMembershipHelper[DepartmentSmallGroup] = smartMock[UserGroupMembershipHelper[DepartmentSmallGroup]]

		val mockUserLookup: UserLookupService = smartMock[UserLookupService]

		val dept = new Department

		val user = new User
		user.setUserId("cusdx")
		user.setWarwickId("0123456")

		val module = new Module("am101", dept)
		val module2: Module = new Module("cs123", dept)

		val student: StudentMember = Fixtures.student(userId = "cusdx", department = dept)

		val groupSet = new SmallGroupSet
		groupSet.module = module
		groupSet.academicYear = new AcademicYear(2013)

		val groupSet2 = new SmallGroupSet
		groupSet2.module = module2
		groupSet2.academicYear = new AcademicYear(2013)

		val group = new SmallGroup
		group.groupSet = groupSet
		group.students.add(user)

		val event1 = new SmallGroupEvent(group)
		event1.day = DayOfWeek.Monday
		event1.startTime = new LocalTime(11, 0)
		event1.endTime = new LocalTime(12, 0)
		event1.weekRanges = Seq(WeekRange(2,3))

		val occurrence1 = new SmallGroupEventOccurrence
		occurrence1.id = "occurrence1"
		occurrence1.event = event1
		occurrence1.week = 2
		val occurrence2 = new SmallGroupEventOccurrence
		occurrence2.id = "occurrence2"
		occurrence2.event = event1
		occurrence2.week = 3

		val eventOccurrencesGroup1: Seq[SmallGroupEventOccurrence] =  Seq(occurrence1) ++ Seq(occurrence2)

		val user1 = new User
		user1.setUserId("cusdy")
		user1.setWarwickId("0123457")
		val user2 = new User
		user2.setUserId("cusdz")
		user2.setWarwickId("0123458")
		val user3 = new User
		user3.setUserId("cusda")
		user3.setWarwickId("0123459")

		group.students.add(user1)
		group.students.add(user2)
		group.students.add(user3)
		group.id = "0001-group1"
		groupSet.groups.add(group)
		groupSet.id =  "0001"

		val group2 = new SmallGroup
		group2.groupSet = groupSet2
		group2.students.add(user)
		group2.id = "0001-group2"
		groupSet2.groups.add(group2)
		groupSet2.id =  "0002"
		val event2 = new SmallGroupEvent(group2)
		event2.day = DayOfWeek.Monday
		event2.startTime = new LocalTime(11, 0)
		event2.endTime = new LocalTime(13, 0)
		event2.weekRanges = Seq(WeekRange(2))

		val eventOccurrence2 = new SmallGroupEventOccurrence
		eventOccurrence2.id = "eventoccurrence2"
		eventOccurrence2.event = event2
		eventOccurrence2.week = 2
		val eventOccurrencesGroup2 =  Seq(eventOccurrence2)
		group.addEvent(event1)

			// user  0123456 is in both groups - group and group2
		group2.addEvent(event2)
		val modreg = new ModuleRegistration(student.mostSignificantCourseDetails.get, module, new JBigDecimal(30), new AcademicYear(2013), "A")
		val modreg2 = new ModuleRegistration(student.mostSignificantCourseDetails.get, module2, new JBigDecimal(30), new AcademicYear(2013), "A")
		val userLookup = new MockUserLookup(true)
		def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
			case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
			case ug: UserGroup => ug.userLookup = userLookup
		}

		val service = new AbstractSmallGroupService
			with AutowiringAssessmentMembershipDaoComponent // don't need this component, so autowiring to null is fine
			with SmallGroupMembershipHelpers
			with UserLookupComponent
			with UserGroupDaoComponent
			with SmallGroupDaoComponent
		  with SecurityServiceComponent
			with TermServiceComponent
			with TermAwareWeekToDateConverterComponent
			with Logging with TaskBenchmarking{
				val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent] = null
				val groupSetManualMembersHelper: UserGroupMembershipHelper[SmallGroupSet] = null
				val departmentGroupSetManualMembersHelper: UserGroupMembershipHelper[DepartmentSmallGroupSet] = null
				val studentGroupHelper: UserGroupMembershipHelper[SmallGroup] = studentGroupMembershipHelper
				val departmentStudentGroupHelper: UserGroupMembershipHelper[DepartmentSmallGroup] = departmentStudentGroupMembershipHelper
				val termService: TermService = smartMock[TermService]

				val membershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]
				val smallGroupDao: SmallGroupDao = smartMock[SmallGroupDao]

				override val weekToDateConverter: WeekToDateConverter = smartMock[WeekToDateConverter]

				smallGroupDao.findByModuleAndYear(module, new AcademicYear(2013)) returns Seq[SmallGroup](group)
				smallGroupDao.findByModuleAndYear(module2, new AcademicYear(2013)) returns Seq[SmallGroup]()
				group.groupSet.membershipService = membershipService
				smallGroupDao.findSmallGroupOccurrencesByGroup(group) returns eventOccurrencesGroup1
				smallGroupDao.findSmallGroupOccurrencesByGroup(group2) returns eventOccurrencesGroup2
				membershipService.determineMembershipUsers(any[Seq[UpstreamAssessmentGroup]], any[Option[UnspecifiedTypeUserGroup]]) returns Seq(user) ++ Seq(user1) ++ Seq(user2) ++ Seq(user3)
				studentGroupHelper.findBy(user) returns Seq(group) ++ Seq(group2)
				studentGroupHelper.findBy(user1) returns Seq(group)
				studentGroupHelper.findBy(user2) returns Seq(group)
				studentGroupHelper.findBy(user3) returns Seq(group)
				departmentStudentGroupHelper.findBy(user) returns Seq()
				departmentStudentGroupHelper.findBy(user1) returns Seq()
				departmentStudentGroupHelper.findBy(user2) returns Seq()
				departmentStudentGroupHelper.findBy(user3) returns Seq()

				weekToDateConverter.toLocalDatetime(2, DayOfWeek.Monday, event1.startTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 15, 11, 0))
				weekToDateConverter.toLocalDatetime(2, DayOfWeek.Monday, event1.endTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 15, 13, 0))
				weekToDateConverter.toLocalDatetime(2, DayOfWeek.Monday, event2.startTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 15, 11, 0))
				weekToDateConverter.toLocalDatetime(2, DayOfWeek.Monday, event2.endTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 15, 12, 0))
				weekToDateConverter.toLocalDatetime(3, DayOfWeek.Monday, event1.startTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 22, 11, 0))
				weekToDateConverter.toLocalDatetime(3, DayOfWeek.Monday, event1.endTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 22, 13, 0))
				weekToDateConverter.toLocalDatetime(3, DayOfWeek.Monday, event2.startTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 22, 11, 0))
				weekToDateConverter.toLocalDatetime(3, DayOfWeek.Monday, event2.endTime , groupSet.academicYear) returns Some(new LocalDateTime(2013, DateTimeConstants.SEPTEMBER, 22, 12, 0))

				val userGroupDao: UserGroupDao = smartMock[UserGroupDao]
			  val securityService: SecurityService = smartMock[SecurityService]
				def userLookup: UserLookupService = mockUserLookup
		}
	}

	@Test
	def findSmallGroupsByMemberCallsMembershipHelper() {
		new Environment {
			val departmentGroup = new DepartmentSmallGroup(new DepartmentSmallGroupSet)
			departmentGroup.linkedGroups.add(group2)

			studentGroupMembershipHelper.findBy(user) returns Seq(group)
			departmentStudentGroupMembershipHelper.findBy(user) returns Seq(departmentGroup)

			service.findSmallGroupsByStudent(user) should be(Seq(group, group2))
			verify(studentGroupMembershipHelper, times(1)).findBy(user)
			verify(departmentStudentGroupMembershipHelper, times(1)).findBy(user)
		}
	}

	@Test
	def testRemoveFromSmallGroups() {
		new Environment {
			mockUserLookup.getUserByUserId("cusdx") returns user

			// try removing the user from the group, but pass in a different module from that associated with the group - this should fail:
			service.removeFromSmallGroups(modreg2)
			group.students.includesUser(user) should be (true)

			// try removing the user from the group, and pass in the module associated with the group
			// - but have the departmental setting set to not allow auto group deregistration - this should fail again::
			dept.autoGroupDeregistration = false
			service.removeFromSmallGroups(modreg)
			group.students.includesUser(user) should be (true)

			// now set the departmental setting back to the default,
			// try removing the user from the group, and pass in the module associated with the group - this should succeed:
			dept.autoGroupDeregistration = true
			service.removeFromSmallGroups(modreg)
			group.students.includesUser(user) should be (false)

			// now check the user is still in group2:
			group2.students.includesUser(user) should be (true)
		}
	}

	@Test
	def testTimetableConflictSmallGroupTimetableClash() {
		new Environment {

			mockUserLookup.getUserByUserId("cusdx") returns user
			mockUserLookup.getUserByUserId("cusdy") returns user1
			mockUserLookup.getUserByUserId("cusdz") returns user2
			mockUserLookup.getUserByUserId("cusda") returns user3
			val clashes: Seq[(SmallGroup, Seq[User])] = service.findPossibleTimetableClashesForGroupSet(group.groupSet)
			clashes.size should be (1)
			val doesUserClashTimetable: Boolean = clashes.exists { case(clashGroup,  users) =>  clashGroup.id ==  group.id  &&  users.exists(groupUser => user.getUserId == groupUser.getUserId) && users.size == 1}
			doesUserClashTimetable should be (true)
		}
	}
}



