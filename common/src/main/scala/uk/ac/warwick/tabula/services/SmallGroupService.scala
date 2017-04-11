package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.commands.groups.RemoveUserFromSmallGroupCommand
import uk.ac.warwick.tabula.commands.{Appliable, TaskBenchmarking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import org.joda.time.{DateTime, LocalDateTime}
import uk.ac.warwick.tabula.data.Transactions._

import scala.collection.JavaConverters._

trait SmallGroupServiceComponent {
	def smallGroupService: SmallGroupService
}

trait AutowiringSmallGroupServiceComponent extends SmallGroupServiceComponent {
	var smallGroupService: SmallGroupService = Wire[SmallGroupService]
}

trait SmallGroupService {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]
	def getSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int): Option[SmallGroupEventOccurrence]
	def getDepartmentSmallGroupSetById(id: String): Option[DepartmentSmallGroupSet]
	def getDepartmentSmallGroupById(id: String): Option[DepartmentSmallGroup]
	def getOrCreateSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int): Option[SmallGroupEventOccurrence]
	def getOrCreateSmallGroupEventOccurrences(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence]
	def getAllSmallGroupEventOccurrencesForEvent(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence]
	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote)
	def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet)
	def saveOrUpdate(smallGroup: DepartmentSmallGroup)
	def saveOrUpdate(attendance: SmallGroupEventAttendance)
	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent]
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup]
	def removeUserFromGroup(user: User, smallGroup: SmallGroup)
	def removeFromSmallGroups(moduleRegistration: ModuleRegistration)

	def getSmallGroupSets(department: Department, year: AcademicYear): Seq[SmallGroupSet]
	def getSmallGroupSets(module: Module, year: AcademicYear): Seq[SmallGroupSet]
	def getAllSmallGroupSets(department: Department): Seq[SmallGroupSet]
	def countAllSmallGroupSets(year: AcademicYear): Int
	def getAllSmallGroupSets(year: AcademicYear, maxResults: Int, startResult: Int): Seq[SmallGroupSet]

	def findSmallGroupsByStudent(student: User): Seq[SmallGroup]
	def findSmallGroupSetsByMember(user: User): Seq[SmallGroupSet]

	def saveOrUpdateAttendance(studentId: String, event: SmallGroupEvent, weekNumber: Int, state: AttendanceState, user: CurrentUser): SmallGroupEventAttendance
	def deleteAttendance(studentId: String, event: SmallGroupEvent, weekNumber: Int, isPermanent: Boolean = false): Unit
	def findAttendanceByGroup(smallGroup: SmallGroup): Seq[SmallGroupEventOccurrence]
	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote]
	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote]
	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence) : Option[SmallGroupEventAttendance]
	def findSmallGroupsWithAttendanceRecorded(studentId: String): Seq[SmallGroup]
	def findManuallyAddedAttendance(studentId: String): Seq[SmallGroupEventAttendance]
	def findStudentAttendanceInEvents(universityId: String, events: Seq[SmallGroupEvent]): Seq[SmallGroupEventAttendance]

	def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, modules: Seq[Module]): Seq[SmallGroupEventAttendance]

	def findOccurrencesInModulesInWeeks(startWeek: Int, endWeek: Int, modules: Seq[Module], academicYear: AcademicYear): Seq[SmallGroupEventOccurrence]
	def findOccurrencesInWeeks(startWeek: Int, endWeek: Int, academicYear: AcademicYear): Seq[SmallGroupEventOccurrence]

	def hasSmallGroups(module: Module): Boolean
	def hasSmallGroups(module: Module, academicYear: AcademicYear): Boolean

	def getDepartmentSmallGroupSets(department: Department, year: AcademicYear): Seq[DepartmentSmallGroupSet]
	def findDepartmentSmallGroupSetsLinkedToSITSByDepartment(year: AcademicYear): Map[Department, Seq[DepartmentSmallGroupSet]]

	def delete(occurrence: SmallGroupEventOccurrence)

	def findReleasedSmallGroupsByTutor(user: CurrentUser): Seq[SmallGroup]
	def findTodaysEventOccurrences(tutors: Seq[User], modules: Seq[Module], departments: Seq[Department]): Seq[SmallGroupEventOccurrence]
	def findPossibleTimetableClashesForGroupSet(set: SmallGroupSet): Seq[(SmallGroup, Seq[User])]
	def doesTimetableClashesForStudent(smallGroup: SmallGroup, student: User): Boolean

	def listSmallGroupEventsForReport(department: Department, academicYear: AcademicYear): Seq[SmallGroupEventReportData]

	def listMemberDataForAllocation(members: Seq[Member], academicYear: AcademicYear): Map[Member, MemberAllocationData]

	def listDepartmentSetsForMembershipUpdate: Seq[DepartmentSmallGroupSet]
}

abstract class AbstractSmallGroupService extends SmallGroupService {
	self: SmallGroupDaoComponent
		with AssessmentMembershipDaoComponent
		with SmallGroupMembershipHelpers
		with UserLookupComponent
		with UserGroupDaoComponent
		with SecurityServiceComponent
		with TermServiceComponent
		with WeekToDateConverterComponent
		with Logging with TaskBenchmarking =>

	def getSmallGroupSetById(id: String): Option[SmallGroupSet] = smallGroupDao.getSmallGroupSetById(id)
	def getSmallGroupById(id: String): Option[SmallGroup] = smallGroupDao.getSmallGroupById(id)
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent] = smallGroupDao.getSmallGroupEventById(id)
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence] = smallGroupDao.getSmallGroupEventOccurrenceById(id)
	def getSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int): Option[SmallGroupEventOccurrence] = smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber)
	def getDepartmentSmallGroupSetById(id: String): Option[DepartmentSmallGroupSet] = smallGroupDao.getDepartmentSmallGroupSetById(id)
	def getDepartmentSmallGroupById(id: String): Option[DepartmentSmallGroup] = smallGroupDao.getDepartmentSmallGroupById(id)
	def getOrCreateSmallGroupEventOccurrence(event: SmallGroupEvent, weekNumber: Int): Option[SmallGroupEventOccurrence] = {
		event.allWeeks.find(_ == weekNumber).map { _ =>
			smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber).getOrElse {
				val newOccurrence = new SmallGroupEventOccurrence()
				newOccurrence.event = event
				newOccurrence.week = weekNumber
				smallGroupDao.saveOrUpdate(newOccurrence)
				newOccurrence
			}
		}
	}

	def getOrCreateSmallGroupEventOccurrences(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence] =
		event.allWeeks.flatMap(getOrCreateSmallGroupEventOccurrence(event, _))

	def getAllSmallGroupEventOccurrencesForEvent(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence] =
		smallGroupDao.findSmallGroupOccurrencesByEvent(event)

	def saveOrUpdate(smallGroupSet: SmallGroupSet): Unit = smallGroupDao.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup): Unit = smallGroupDao.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent): Unit = smallGroupDao.saveOrUpdate(smallGroupEvent)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote): Unit = smallGroupDao.saveOrUpdate(note)
	def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet): Unit = smallGroupDao.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: DepartmentSmallGroup): Unit = smallGroupDao.saveOrUpdate(smallGroup)
	def saveOrUpdate(attendance: SmallGroupEventAttendance): Unit = smallGroupDao.saveOrUpdate(attendance)

	def getSmallGroupSets(department: Department, year: AcademicYear): Seq[SmallGroupSet] = smallGroupDao.findSetsByDepartmentAndYear(department, year)
	def getSmallGroupSets(module: Module, year: AcademicYear): Seq[SmallGroupSet] = smallGroupDao.findSetsByModuleAndYear(module, year)
	def getAllSmallGroupSets(department: Department): Seq[SmallGroupSet] = smallGroupDao.findAllSetsByDepartment(department)
	def countAllSmallGroupSets(year: AcademicYear): Int = smallGroupDao.countAllSetsByYear(year)
	def getAllSmallGroupSets(year: AcademicYear, maxResults: Int, startResult: Int): Seq[SmallGroupSet] = smallGroupDao.findAllSetsByYear(year, maxResults, startResult)

	def findSmallGroupEventsByTutor(user: User): Seq[SmallGroupEvent] = eventTutorsHelper.findBy(user)
	def findSmallGroupsByTutor(user: User): Seq[SmallGroup] = findSmallGroupEventsByTutor(user)
		.groupBy(_.group).keys.toSeq
		.filterNot { sg => sg.groupSet.deleted || sg.groupSet.archived }

	def findSmallGroupSetsByMember(user:User):Seq[SmallGroupSet] = {
		val autoEnrolled =
			membershipDao.getSITSEnrolledSmallGroupSets(user)
				 .filterNot { _.members.excludesUser(user) }

		val manuallyEnrolled =
			groupSetManualMembersHelper.findBy(user)
				.filterNot { sgs => sgs.deleted || sgs.archived }

		val linked =
			departmentGroupSetManualMembersHelper.findBy(user)
				.filterNot { dsgs => dsgs.deleted || dsgs.archived }
				.flatMap { _.linkedSets.asScala }
				.filterNot { sgs => sgs.deleted || sgs.archived }

		(autoEnrolled ++ manuallyEnrolled ++ linked).distinct
	}

	def findSmallGroupsByStudent(user: User): Seq[SmallGroup] = {
		benchmarkTask("findSmallGroupsByStudent") {
			val groups = benchmarkTask("findByStudentGroupHelper") {
				studentGroupHelper.findBy(user)
					.filterNot { group => group.groupSet.deleted || group.groupSet.archived }
			}
			val linkedGroups = benchmarkTask("findBydepartmentStudentGroupHelper") {
				departmentStudentGroupHelper.findBy(user)
					.filterNot { group => group.groupSet.deleted || group.groupSet.archived }
					.flatMap {
						_.linkedGroups.asScala
					}
					.filterNot { group => group.groupSet.deleted || group.groupSet.archived }
			}
			(groups ++ linkedGroups).distinct
		}
	}

	def deleteAttendance(studentId: String, event: SmallGroupEvent, weekNumber: Int, isPermanent: Boolean = false) {
		for {
			occurrence <- smallGroupDao.getSmallGroupEventOccurrence(event, weekNumber)
			attendance <- smallGroupDao.getAttendance(studentId, occurrence)
		} {
			if (attendance.replacedBy.isEmpty && (!attendance.addedManually || isPermanent)) {
				occurrence.attendance.remove(attendance)
				smallGroupDao.deleteAttendance(attendance)
			} else {
				attendance.state = AttendanceState.NotRecorded // don't unlink
				smallGroupDao.saveOrUpdate(attendance)
			}
		}
	}

	def saveOrUpdateAttendance(
		studentId: String,
		event: SmallGroupEvent,
		weekNumber: Int,
		state: AttendanceState,
		user: CurrentUser
	): SmallGroupEventAttendance = {
		val occurrence = getOrCreateSmallGroupEventOccurrence(event, weekNumber).getOrElse(throw new IllegalArgumentException(
			s"Week number $weekNumber is not valid for event ${event.id}"
		))

		val attendance = smallGroupDao.getAttendance(studentId, occurrence).getOrElse({
			val newAttendance = new SmallGroupEventAttendance
			newAttendance.occurrence = occurrence
			newAttendance.universityId = studentId
			newAttendance
		})

		attendance.state = state

		attendance.updatedBy = user.userId
		attendance.updatedDate = DateTime.now
		smallGroupDao.saveOrUpdate(attendance)
		attendance
	}

	def findAttendanceByGroup(smallGroup: SmallGroup): Seq[SmallGroupEventOccurrence] = {
		// We need to only get back valid occurrences (event related), database does not delete occurrences later once created initially.
		smallGroupDao.findSmallGroupOccurrencesByGroup(smallGroup).filter { groupEventOccurrence => groupEventOccurrence.event.allWeeks.contains(groupEventOccurrence.week) }
	}

	def removeFromSmallGroups(modReg: ModuleRegistration) {
		if (modReg.module.adminDepartment.autoGroupDeregistration) {
			val userId = modReg.studentCourseDetails.student.userId
			val user = userLookup.getUserByUserId(userId)

			for {
			    smallGroup <- smallGroupDao.findByModuleAndYear(modReg.module, modReg.academicYear)
			    if smallGroup.students.includesUser(user)
			} {
				logger.info(s"Removing $userId from small group $smallGroup due to removed registration $modReg")

				// Wrap this in a sub-command so that we can do auditing
				userGroupDao.saveOrUpdate(removeFromGroupCommand(user, smallGroup).apply() match {
					case group: UserGroupCacheManager => group.underlying.asInstanceOf[UserGroup]
					case group: UnspecifiedTypeUserGroup => group.asInstanceOf[UserGroup]
				})
			}
		}
	}

	def removeUserFromGroup(user: User, smallGroup: SmallGroup) {
		userGroupDao.saveOrUpdate(removeFromGroupCommand(user, smallGroup).apply() match {
			case group: UserGroupCacheManager => group.underlying.asInstanceOf[UserGroup]
			case group: UnspecifiedTypeUserGroup => group.asInstanceOf[UserGroup]
		})
	}

	private def removeFromGroupCommand(user: User, smallGroup: SmallGroup): Appliable[UnspecifiedTypeUserGroup] = {
		new RemoveUserFromSmallGroupCommand(user, smallGroup)
	}

	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote] =
		smallGroupDao.getAttendanceNote(studentId, occurrence)

	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote] =
		smallGroupDao.findAttendanceNotes(studentIds, occurrences)

	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence) : Option[SmallGroupEventAttendance] =
		smallGroupDao.getAttendance(studentId, occurrence)

	def findSmallGroupsWithAttendanceRecorded(studentId: String): Seq[SmallGroup] =
		smallGroupDao.findSmallGroupsWithAttendanceRecorded(studentId)

	def findManuallyAddedAttendance(studentId: String): Seq[SmallGroupEventAttendance] =
		smallGroupDao.findManuallyAddedAttendance(studentId)

	def findStudentAttendanceInEvents(universityId: String, events: Seq[SmallGroupEvent]): Seq[SmallGroupEventAttendance] =
		smallGroupDao.findStudentAttendanceInEvents(universityId, events)

	def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, modules: Seq[Module]): Seq[SmallGroupEventAttendance] =
		smallGroupDao.findAttendanceForStudentInModulesInWeeks(student, startWeek, endWeek, modules)

	def findOccurrencesInModulesInWeeks(startWeek: Int, endWeek: Int, modules: Seq[Module], academicYear: AcademicYear): Seq[SmallGroupEventOccurrence] =
		smallGroupDao.findOccurrencesInModulesInWeeks(startWeek, endWeek, modules, academicYear)

	def findOccurrencesInWeeks(startWeek: Int, endWeek: Int, academicYear: AcademicYear): Seq[SmallGroupEventOccurrence] =
		smallGroupDao.findOccurrencesInWeeks(startWeek, endWeek, academicYear)

	def hasSmallGroups(module: Module): Boolean = smallGroupDao.hasSmallGroups(module)
	def hasSmallGroups(module: Module, academicYear: AcademicYear): Boolean = smallGroupDao.hasSmallGroups(module, academicYear)

	def getDepartmentSmallGroupSets(department: Department, year: AcademicYear): Seq[DepartmentSmallGroupSet] = smallGroupDao.getDepartmentSmallGroupSets(department, year)

	def findDepartmentSmallGroupSetsLinkedToSITSByDepartment(year: AcademicYear): Map[Department, Seq[DepartmentSmallGroupSet]] =
		smallGroupDao.findDepartmentSmallGroupSetsLinkedToSITSByDepartment(year)

	def delete(occurrence: SmallGroupEventOccurrence) {
		if (occurrence.attendance.asScala.exists { attendance => attendance.state != AttendanceState.NotRecorded })
			throw new IllegalStateException("Tried to delete event with linked event occurrence")

		smallGroupDao.delete(occurrence)
	}

	def findReleasedSmallGroupsByTutor(user: CurrentUser): Seq[SmallGroup] = 	findSmallGroupsByTutor(user.apparentUser)
		.filter { group =>
			// The set is visible to tutors; OR
			group.groupSet.releasedToTutors ||
				// I have permission to view the membership of the set anyway
				securityService.can(user, Permissions.SmallGroups.ReadMembership, group)
	}

	def findTodaysEventOccurrences(tutors: Seq[User], modules: Seq[Module], departments: Seq[Department]): Seq[SmallGroupEventOccurrence] = {
		val now = DateTime.now
		val today = DayOfWeek.today
		val thisAcademicYear = AcademicYear.findAcademicYearContainingDate(now)
		val thisTermWeek = termService.getTermFromDateIncludingVacations(now).getAcademicWeekNumber(now)
		val allModules = modules ++ departments.flatMap(_.modules.asScala)
		val weeksOccurrencesForModules: Seq[SmallGroupEventOccurrence] = findOccurrencesInModulesInWeeks(thisTermWeek, thisTermWeek, allModules, thisAcademicYear)
			.filter(!_.event.group.groupSet.archived)
		val tutorOccurrences: Seq[SmallGroupEventOccurrence] = tutors.flatMap(findSmallGroupEventsByTutor)
			.filter(e => e.group.groupSet.releasedToTutors && e.group.groupSet.academicYear == thisAcademicYear && !e.group.groupSet.archived)
			.flatMap(event => getOrCreateSmallGroupEventOccurrence(event, thisTermWeek))
		(weeksOccurrencesForModules ++ tutorOccurrences).filter(_.event.day == today)
	}

	def findPossibleTimetableClashesForGroupSet(set: SmallGroupSet): Seq[(SmallGroup, Seq[User])] = {
 		benchmarkTask(s"possibleTimetableClash[Set-${set.id}]") { possibleTimetableClashesForStudents(set, set.allStudents) }
	}

	def doesTimetableClashesForStudent(group: SmallGroup, student: User): Boolean = {
		benchmarkTask(s"studentTimetableClash[Group-${group.id}, Student - ${student.getUserId}]") {
			val possibleClashes = possibleTimetableClashesForStudents(group.groupSet, Seq(student))
			possibleClashes.exists { case(clashGroup, users) => group.id == clashGroup.id && users.exists { user => user.getUserId == student.getUserId } }
		}
	}

	private def possibleTimetableClashesForStudents(set: SmallGroupSet, students: Seq[User]): Seq[(SmallGroup, Seq[User])] = {
		val currentGroupsWithOccurrencesAndDateInfo = benchmarkTask("currentGroupsWithOccurrencesAndDateInfo") {
			set.groups.asScala.map { group =>
				(group, groupOccurrencesWithStartEndDateTimeInfo(group))
			}
		}

		val studentsWithOtherGroupOccurrenceAndDateInfo = benchmarkTask("studentsWithOtherGroupOccurrenceAndDateInfo") {
			//TAB-4425 - Aiming for performance improvement using parallel threads
			val groupsByStudent = benchmarkTask("groupsByStudent") {
				students.par.map(student => student -> transactional(readOnly = true) { findSmallGroupsByStudent(student).filterNot { group => group.groupSet.id == set.id }})
			}.seq.toMap
			val otherGroups = benchmarkTask("otherGroups") { groupsByStudent.values.flatten.toSeq.distinct }
			val otherGroupOccurrencesWithTimes: Map[SmallGroup, Seq[(SmallGroupEventOccurrence, Option[LocalDateTime], Option[LocalDateTime])]] =
				otherGroups.map(group => group -> groupOccurrencesWithStartEndDateTimeInfo(group)).toMap
			groupsByStudent.mapValues(groups => groups.flatMap(otherGroupOccurrencesWithTimes))
		}
		// let us get all clash info only once based on the possibility of students being allocated to various groups of this  groupset. At front end we can do JS magic to filter these students
		// whenever we randomly allocate, shuffle, unallocate students to find real clashes.
		benchmarkTask("possibleTimetableClashesForStudents result") {
			currentGroupsWithOccurrencesAndDateInfo.map { case (group, groupOccurrencesWithdateInfo) =>
				val groupStudentInfoWithPossibleClash = studentsWithOtherGroupOccurrenceAndDateInfo.filter { case (student, otherGroupOccurrencesWithDateInfo) =>
					doesEventOccurrenceOverlap(groupOccurrencesWithdateInfo, otherGroupOccurrencesWithDateInfo)
				}.toSeq
				val groupStudentsWithPossibleClash = groupStudentInfoWithPossibleClash.map { case (student, _) => student }
				(group, groupStudentsWithPossibleClash)
			}
		}
	}

	private def groupOccurrencesWithStartEndDateTimeInfo(group: SmallGroup): Seq[(SmallGroupEventOccurrence, Option[LocalDateTime], Option[LocalDateTime])] = {
		benchmarkTask(s"groupOccurrencesWithStartEndDateTimeInfo[Group-${group.id}]") {
			findAttendanceByGroup(group).filterNot(_.event.isUnscheduled).map { groupOccurrence =>
				val startDateTime = weekToDateConverter.toLocalDatetime(groupOccurrence.week, groupOccurrence.event.day, groupOccurrence.event.startTime, groupOccurrence.event.group.groupSet.academicYear)
				val endDateTime = weekToDateConverter.toLocalDatetime(groupOccurrence.week, groupOccurrence.event.day, groupOccurrence.event.endTime, groupOccurrence.event.group.groupSet.academicYear)
				(groupOccurrence, startDateTime, endDateTime)
			}
		}
	}

	private def doesEventOccurrenceOverlap(
		groupOccurrencesWithdateInfo: Seq[(SmallGroupEventOccurrence, Option[LocalDateTime], Option[LocalDateTime])],
		otherOccurrencesWithdateInfo: Seq[(SmallGroupEventOccurrence, Option[LocalDateTime], Option[LocalDateTime])]
	): Boolean = {
		groupOccurrencesWithdateInfo.exists { case(groupOccurrence, startDateTime1, endDateTime1) =>
			startDateTime1.isDefined && endDateTime1.isDefined && otherOccurrencesWithdateInfo.exists { case(occ, startDateTime2, endDateTime2) =>
				startDateTime2.isDefined && endDateTime2.isDefined && startDateTime1.get.isBefore(endDateTime2.get) && endDateTime1.get.isAfter(startDateTime2.get)
			}
		}
	}

	def listSmallGroupEventsForReport(department: Department, academicYear: AcademicYear): Seq[SmallGroupEventReportData] =
		smallGroupDao.listSmallGroupEventsForReport(department, academicYear)

	def listMemberDataForAllocation(members: Seq[Member], academicYear: AcademicYear): Map[Member, MemberAllocationData] =
		smallGroupDao.listMemberDataForAllocation(members, academicYear)

	def listDepartmentSetsForMembershipUpdate: Seq[DepartmentSmallGroupSet] =
		smallGroupDao.listDepartmentSetsForMembershipUpdate
}

trait SmallGroupMembershipHelpers {
	val eventTutorsHelper: UserGroupMembershipHelper[SmallGroupEvent]
	val studentGroupHelper: UserGroupMembershipHelper[SmallGroup]
	val groupSetManualMembersHelper: UserGroupMembershipHelper[SmallGroupSet]
	val departmentStudentGroupHelper: UserGroupMembershipHelper[DepartmentSmallGroup]
	val departmentGroupSetManualMembersHelper: UserGroupMembershipHelper[DepartmentSmallGroupSet]
	val membershipDao: AssessmentMembershipDao
}

// new up UGMHs which will Wire.auto() their dependencies
trait SmallGroupMembershipHelpersImpl extends SmallGroupMembershipHelpers {
	val eventTutorsHelper = new UserGroupMembershipHelper[SmallGroupEvent]("_tutors")

	// This won't use linked assessment components, only manual membership
	val groupSetManualMembersHelper = new UserGroupMembershipHelper[SmallGroupSet]("_membersGroup")

	val studentGroupHelper = new UserGroupMembershipHelper[SmallGroup]("_studentsGroup")

	// This won't use linked assessment components, only manual membership
	val departmentGroupSetManualMembersHelper = new UserGroupMembershipHelper[DepartmentSmallGroupSet]("_membersGroup")

	val departmentStudentGroupHelper = new UserGroupMembershipHelper[DepartmentSmallGroup]("_studentsGroup")
}

@Service("smallGroupService")
class SmallGroupServiceImpl
	extends AbstractSmallGroupService
		with AutowiringSmallGroupDaoComponent
		with AutowiringAssessmentMembershipDaoComponent
	  with SmallGroupMembershipHelpersImpl
	  with AutowiringUserLookupComponent
		with UserLookupComponent
		with AutowiringUserGroupDaoComponent
		with AutowiringSecurityServiceComponent
		with AutowiringTermServiceComponent
		with TermAwareWeekToDateConverterComponent
		with Logging with TaskBenchmarking
