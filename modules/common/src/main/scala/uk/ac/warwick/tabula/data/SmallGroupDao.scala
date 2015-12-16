package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups._
import org.hibernate.criterion.{Projections, Order}
import org.hibernate.criterion.Restrictions._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Department, StudentMember, Module}
import scala.collection.JavaConverters._

trait SmallGroupDaoComponent {
	val smallGroupDao: SmallGroupDao
}

trait AutowiringSmallGroupDaoComponent extends SmallGroupDaoComponent {
	val smallGroupDao = Wire[SmallGroupDao]
}

trait SmallGroupDao {
	def getSmallGroupSetById(id: String): Option[SmallGroupSet]
	def getSmallGroupById(id: String): Option[SmallGroup]
	def getSmallGroupEventById(id: String): Option[SmallGroupEvent]
	def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]
	def getDepartmentSmallGroupSetById(id: String): Option[DepartmentSmallGroupSet]
	def getDepartmentSmallGroupById(id: String): Option[DepartmentSmallGroup]

	def saveOrUpdate(smallGroupSet: SmallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent)
	def saveOrUpdate(occurrence: SmallGroupEventOccurrence)
	def saveOrUpdate(attendance: SmallGroupEventAttendance)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote)
	def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet)
	def saveOrUpdate(smallGroup: DepartmentSmallGroup)

	def findSetsByDepartmentAndYear(department: Department, year: AcademicYear): Seq[SmallGroupSet]
	def findSetsByModuleAndYear(module: Module, year: AcademicYear): Seq[SmallGroupSet]
	def findAllSetsByDepartment(department: Department): Seq[SmallGroupSet]
	def findByModuleAndYear(module: Module, year: AcademicYear): Seq[SmallGroup]

	def getSmallGroupEventOccurrence(event: SmallGroupEvent, week: Int): Option[SmallGroupEventOccurrence]
	def findSmallGroupOccurrencesByGroup(group: SmallGroup): Seq[SmallGroupEventOccurrence]
	def findSmallGroupOccurrencesByEvent(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence]

	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendance]
	def deleteAttendance(attendance: SmallGroupEventAttendance): Unit
	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote]
	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote]

	def findSmallGroupsWithAttendanceRecorded(studentId: String): Seq[SmallGroup]
	def findManuallyAddedAttendance(studentId: String): Seq[SmallGroupEventAttendance]
	def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, modules: Seq[Module]): Seq[SmallGroupEventAttendance]

	def hasSmallGroups(module: Module): Boolean
	def hasSmallGroups(module: Module, academicYear: AcademicYear): Boolean

	def getDepartmentSmallGroupSets(department: Department): Seq[DepartmentSmallGroupSet]
	def getDepartmentSmallGroupSets(department: Department, year: AcademicYear): Seq[DepartmentSmallGroupSet]

	def delete(occurrence: SmallGroupEventOccurrence)
}

@Repository
class SmallGroupDaoImpl extends SmallGroupDao with Daoisms {
	import Order._

	def getSmallGroupSetById(id: String) = session.getById[SmallGroupSet](id)
	def getSmallGroupById(id: String) = session.getById[SmallGroup](id)
	def getSmallGroupEventById(id: String) = session.getById[SmallGroupEvent](id)
	def getSmallGroupEventOccurrenceById(id: String) = session.getById[SmallGroupEventOccurrence](id)
	def getDepartmentSmallGroupSetById(id: String) = session.getById[DepartmentSmallGroupSet](id)
	def getDepartmentSmallGroupById(id: String) = session.getById[DepartmentSmallGroup](id)
	def saveOrUpdate(smallGroupSet: SmallGroupSet) = session.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: SmallGroup) = session.saveOrUpdate(smallGroup)
	def saveOrUpdate(smallGroupEvent: SmallGroupEvent) = session.saveOrUpdate(smallGroupEvent)
	def saveOrUpdate(occurrence: SmallGroupEventOccurrence) = session.saveOrUpdate(occurrence)
	def saveOrUpdate(attendance: SmallGroupEventAttendance) = session.saveOrUpdate(attendance)
	def saveOrUpdate(note: SmallGroupEventAttendanceNote) = session.saveOrUpdate(note)
	def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet) = session.saveOrUpdate(smallGroupSet)
	def saveOrUpdate(smallGroup: DepartmentSmallGroup) = session.saveOrUpdate(smallGroup)

	def getSmallGroupEventOccurrence(event: SmallGroupEvent, week: Int) =
		session.newCriteria[SmallGroupEventOccurrence]
			.add(is("event", event))
			.add(is("week", week))
			.uniqueResult

	def findSetsByDepartmentAndYear(department: Department, year: AcademicYear) =
		session.newCriteria[SmallGroupSet]
			.createAlias("module", "module")
			.add(is("module.adminDepartment", department))
			.add(is("academicYear", year))
			.add(is("deleted", false))
			.addOrder(asc("archived"))
			.addOrder(asc("name"))
			.seq

	def findSetsByModuleAndYear(module: Module, year: AcademicYear) =
		session.newCriteria[SmallGroupSet]
			.add(is("module", module))
			.add(is("academicYear", year))
			.add(is("deleted", false))
			.addOrder(asc("archived"))
			.addOrder(asc("name"))
			.seq

	def findAllSetsByDepartment(department: Department) =
		session.newCriteria[SmallGroupSet]
			.createAlias("module", "module")
			.add(is("module.adminDepartment", department))
			.seq

	def findByModuleAndYear(module: Module, year: AcademicYear) =
		session.newCriteria[SmallGroup]
			.createAlias("groupSet", "set")
			.add(is("set.module", module))
			.add(is("set.academicYear", year))
			.seq

	def findSmallGroupOccurrencesByGroup(group: SmallGroup) =
		session.newCriteria[SmallGroupEventOccurrence]
			.createAlias("event", "event")
			.add(is("event.group", group))
			.addOrder(asc("week"))
			.addOrder(asc("event.day"))
			.seq

	def findSmallGroupOccurrencesByEvent(event: SmallGroupEvent) =
		session.newCriteria[SmallGroupEventOccurrence]
			.add(is("event", event))
			.addOrder(asc("week"))
			.seq

	def getAttendance(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendance] =
		session.newCriteria[SmallGroupEventAttendance]
				.add(is("universityId", studentId))
				.add(is("occurrence", occurrence))
				.uniqueResult

	def deleteAttendance(attendance: SmallGroupEventAttendance): Unit = session.delete(attendance)

	def getAttendanceNote(studentId: String, occurrence: SmallGroupEventOccurrence): Option[SmallGroupEventAttendanceNote] = {
		session.newCriteria[SmallGroupEventAttendanceNote]
			.add(is("student.id", studentId))
			.add(is("occurrence", occurrence))
			.uniqueResult
	}

	def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote] = {
		if(studentIds.isEmpty || occurrences.isEmpty)
			return Seq()

		session.newCriteria[SmallGroupEventAttendanceNote]
			.add(in("student.id", studentIds.asJava))
			.add(in("occurrence", occurrences.asJava))
			.seq
	}

	def findSmallGroupsWithAttendanceRecorded(studentId: String): Seq[SmallGroup] =
		session.newCriteria[SmallGroupEventAttendance]
			.createAlias("occurrence", "occurrence")
			.createAlias("occurrence.event", "event")
			.add(is("universityId", studentId))
			.project[SmallGroup](Projections.distinct(Projections.groupProperty("event.group")))
			.seq

	def findManuallyAddedAttendance(studentId: String): Seq[SmallGroupEventAttendance] =
		session.newCriteria[SmallGroupEventAttendance]
			.add(is("universityId", studentId))
			.add(is("addedManually", true))
			.seq

	def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, modules: Seq[Module]): Seq[SmallGroupEventAttendance] = {
		if (modules.isEmpty) {
			session.newCriteria[SmallGroupEventAttendance]
				.createAlias("occurrence", "occurrence")
				.add(is("universityId", student.universityId))
				.add(ge("occurrence.week", startWeek))
				.add(le("occurrence.week", endWeek))
				.seq
		} else {
			val c = () => {
				session.newCriteria[SmallGroupEventAttendance]
					.createAlias("occurrence", "occurrence")
					.createAlias("occurrence.event", "event")
					.createAlias("event.group", "group")
					.createAlias("group.groupSet", "groupSet")
					.add(is("universityId", student.universityId))
					.add(ge("occurrence.week", startWeek))
					.add(le("occurrence.week", endWeek))
			}
			safeInSeq(c, "groupSet.module", modules)
		}

	}

	def hasSmallGroups(module: Module): Boolean = {
		session.newCriteria[SmallGroupSet]
			.add(is("module", module))
			.add(is("deleted", false))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue() > 0
	}

	def hasSmallGroups(module: Module, academicYear: AcademicYear): Boolean = {
		session.newCriteria[SmallGroupSet]
			.add(is("module", module))
			.add(is("deleted", false))
			.add(is("academicYear", academicYear))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue() > 0
	}

	def getDepartmentSmallGroupSets(department: Department) = {
		session.newCriteria[DepartmentSmallGroupSet]
			.add(is("department", department))
			.add(is("deleted", false))
			.add(is("archived", false))
			.addOrder(asc("name"))
			.seq
	}

	def getDepartmentSmallGroupSets(department: Department, year: AcademicYear) = {
		session.newCriteria[DepartmentSmallGroupSet]
			.add(is("department", department))
			.add(is("academicYear", year))
			.add(is("deleted", false))
			.add(is("archived", false))
			.addOrder(asc("name"))
			.seq
	}

	def delete(occurrence: SmallGroupEventOccurrence) = session.delete(occurrence)
}
