package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Projections._
import org.hibernate.criterion.Restrictions._
import org.hibernate.sql.JoinType
import org.joda.time.LocalTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.EventDeliveryMethod._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.userlookup.User

trait SmallGroupDaoComponent {
  val smallGroupDao: SmallGroupDao
}

trait AutowiringSmallGroupDaoComponent extends SmallGroupDaoComponent {
  val smallGroupDao: SmallGroupDao = Wire[SmallGroupDao]
}

case class SmallGroupEventReportData(
  departmentName: String,
  eventName: String,
  moduleTitle: String,
  day: String,
  start: String,
  finish: String,
  location: String,
  size: Int,
  weeks: String,
  staff: String
)

case class MemberAllocationData(
  routeCode: String,
  routeName: String,
  yearOfStudy: Int
)

trait SmallGroupDao {
  def getSmallGroupSetById(id: String): Option[SmallGroupSet]

  def getSmallGroupSetsByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[SmallGroupSet]

  def getSmallGroupById(id: String): Option[SmallGroup]

  def getSmallGroupEventById(id: String): Option[SmallGroupEvent]

  def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence]

  def getDepartmentSmallGroupSetById(id: String): Option[DepartmentSmallGroupSet]

  def getDepartmentSmallGroupById(id: String): Option[DepartmentSmallGroup]

  def saveOrUpdate(smallGroupSet: SmallGroupSet): Unit

  def saveOrUpdate(smallGroup: SmallGroup): Unit

  def saveOrUpdate(smallGroupEvent: SmallGroupEvent): Unit

  def saveOrUpdate(occurrence: SmallGroupEventOccurrence): Unit

  def saveOrUpdate(attendance: SmallGroupEventAttendance): Unit

  def saveOrUpdate(note: SmallGroupEventAttendanceNote): Unit

  def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet): Unit

  def saveOrUpdate(smallGroup: DepartmentSmallGroup): Unit

  def findSetsByDepartmentAndYear(department: Department, year: AcademicYear): Seq[SmallGroupSet]

  def findSetsByModuleAndYear(module: Module, year: AcademicYear): Seq[SmallGroupSet]

  def findAllSetsByDepartment(department: Department): Seq[SmallGroupSet]

  def countAllSetsByYear(year: AcademicYear): Int

  def findAllSetsByYear(year: AcademicYear, maxResults: Int, startResult: Int): Seq[SmallGroupSet]

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

  def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, academicYear: AcademicYear, modules: Seq[Module]): Seq[SmallGroupEventAttendance]

  def findAllAttendanceForStudentInAcademicYear(student: StudentMember, academicYear: AcademicYear): Seq[SmallGroupEventAttendance]

  def findOccurrencesInModulesInWeeks(startWeek: Int, endWeek: Int, modules: Seq[Module], academicYear: AcademicYear): Seq[SmallGroupEventOccurrence]

  def findOccurrencesInWeeks(startWeek: Int, endWeek: Int, academicYear: AcademicYear): Seq[SmallGroupEventOccurrence]

  def findStudentAttendanceInEvents(universityId: String, events: Seq[SmallGroupEvent]): Seq[SmallGroupEventAttendance]

  def hasSmallGroups(module: Module): Boolean

  def hasSmallGroups(module: Module, academicYear: AcademicYear): Boolean

  def getDepartmentSmallGroupSets(department: Department, year: AcademicYear): Seq[DepartmentSmallGroupSet]

  def findDepartmentSmallGroupSetsLinkedToSITSByDepartment(year: AcademicYear): Map[Department, Seq[DepartmentSmallGroupSet]]

  def findSmallGroupSetsLinkedToSITSByDepartment(year: AcademicYear): Map[Department, Seq[SmallGroupSet]]

  def delete(occurrence: SmallGroupEventOccurrence): Unit

  def findAttendedSmallGroupEvents(studentId: String): Seq[SmallGroupEventAttendance]

  def listSmallGroupEventsForReport(department: Department, academicYear: AcademicYear): Seq[SmallGroupEventReportData]

  def listMemberDataForAllocation(members: Seq[Member], academicYear: AcademicYear): Map[Member, MemberAllocationData]

  def listDepartmentSetsForMembershipUpdate: Seq[DepartmentSmallGroupSet]

  def listSetsForMembershipUpdate: Seq[SmallGroupSet]

  def listSmallGroupsWithoutLocation(academicYear: AcademicYear, department: Option[Department]): Seq[SmallGroupEvent]

  def listSmallGroupsWithoutOnlineLocation(academicYear: AcademicYear, department: Option[Department]): Seq[SmallGroupEvent]

  def findSmallGroupsByNameOrModule(query: FindSmallGroupQuery): Seq[SmallGroup]

  def findAttendanceForStudentsInWeeks(students: Seq[StudentMember], academicYear: AcademicYear, startWeek: Int, endWeek: Int): Seq[SmallGroupEventAttendance]
}

case class FindSmallGroupQuery(
  terms: Seq[String],
  modules: Seq[String],
  academicYear: AcademicYear,
  department: Option[String]
)

@Repository
class SmallGroupDaoImpl extends SmallGroupDao
  with Daoisms with TaskBenchmarking with AutowiringUserLookupComponent {

  val MaxGroupsByName = 15

  def getSmallGroupSetById(id: String): Option[SmallGroupSet] = getById[SmallGroupSet](id)

  def getSmallGroupSetsByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[SmallGroupSet] =
    session.newCriteria[SmallGroupSet]
      .add(is("module", module))
      .add(is("academicYear", year))
      .add(is("name", name))
      .add(is("deleted", false))
      .seq

  def getSmallGroupById(id: String): Option[SmallGroup] = getById[SmallGroup](id)

  def getSmallGroupEventById(id: String): Option[SmallGroupEvent] = getById[SmallGroupEvent](id)

  def getSmallGroupEventOccurrenceById(id: String): Option[SmallGroupEventOccurrence] = getById[SmallGroupEventOccurrence](id)

  def getDepartmentSmallGroupSetById(id: String): Option[DepartmentSmallGroupSet] = getById[DepartmentSmallGroupSet](id)

  def getDepartmentSmallGroupById(id: String): Option[DepartmentSmallGroup] = getById[DepartmentSmallGroup](id)

  def saveOrUpdate(smallGroupSet: SmallGroupSet): Unit = session.saveOrUpdate(smallGroupSet)

  def saveOrUpdate(smallGroup: SmallGroup): Unit = session.saveOrUpdate(smallGroup)

  def saveOrUpdate(smallGroupEvent: SmallGroupEvent): Unit = {
    require(
      smallGroupEvent.deliveryMethod == Hybrid ||
      (smallGroupEvent.deliveryMethod == OnlineOnly && smallGroupEvent.location == null) ||
      (smallGroupEvent.deliveryMethod == FaceToFaceOnly && smallGroupEvent.onlineDeliveryUrl == null && smallGroupEvent.onlinePlatform.isEmpty)
    )
    session.saveOrUpdate(smallGroupEvent)
  }

  def saveOrUpdate(occurrence: SmallGroupEventOccurrence): Unit = session.saveOrUpdate(occurrence)

  def saveOrUpdate(attendance: SmallGroupEventAttendance): Unit = session.saveOrUpdate(attendance)

  def saveOrUpdate(note: SmallGroupEventAttendanceNote): Unit = session.saveOrUpdate(note)

  def saveOrUpdate(smallGroupSet: DepartmentSmallGroupSet): Unit = session.saveOrUpdate(smallGroupSet)

  def saveOrUpdate(smallGroup: DepartmentSmallGroup): Unit = session.saveOrUpdate(smallGroup)

  def getSmallGroupEventOccurrence(event: SmallGroupEvent, week: Int): Option[SmallGroupEventOccurrence] =
    session.newCriteria[SmallGroupEventOccurrence]
      .add(is("event", event))
      .add(is("week", week))
      .uniqueResult

  def findSetsByDepartmentAndYear(department: Department, year: AcademicYear): Seq[SmallGroupSet] =
    session.newCriteria[SmallGroupSet]
      .createAlias("module", "module")
      .add(is("module.adminDepartment", department))
      .add(is("academicYear", year))
      .add(is("deleted", false))
      .addOrder(asc("archived"))
      .addOrder(asc("name"))
      .seq

  def findSetsByModuleAndYear(module: Module, year: AcademicYear): Seq[SmallGroupSet] =
    session.newCriteria[SmallGroupSet]
      .add(is("module", module))
      .add(is("academicYear", year))
      .add(is("deleted", false))
      .addOrder(asc("archived"))
      .addOrder(asc("name"))
      .seq

  def findAllSetsByDepartment(department: Department): Seq[SmallGroupSet] =
    session.newCriteria[SmallGroupSet]
      .createAlias("module", "module")
      .add(is("module.adminDepartment", department))
      .add(is("deleted", false))
      .addOrder(asc("archived"))
      .addOrder(asc("name"))
      .seq

  def countAllSetsByYear(year: AcademicYear): Int =
    session.newCriteria[SmallGroupSet]
      .add(is("academicYear", year))
      .count.intValue

  def findAllSetsByYear(year: AcademicYear, maxResults: Int, startResult: Int): Seq[SmallGroupSet] = {
    val allIds =
      session.newCriteria[SmallGroupSet]
        .createAlias("module", "module")
        .createAlias("module.adminDepartment", "department")
        .add(is("academicYear", year))
        .addOrder(asc("department.code"))
        .addOrder(asc("module.code"))
        .addOrder(asc("name"))
        .setMaxResults(maxResults)
        .setFirstResult(startResult)
        .project[Array[Any]](
        projectionList()
          .add(distinct(id()))
          .add(property("department.code"))
          .add(property("module.code"))
          .add(property("name"))
      )
        .seq.map(_ (0).asInstanceOf[String])

    safeInSeq(() => {
      session.newCriteria[SmallGroupSet]
        .createAlias("module", "module")
        .createAlias("module.adminDepartment", "department")
        .addOrder(asc("department.code"))
        .addOrder(asc("module.code"))
        .addOrder(asc("name"))
        .setFetchMode("groups", FetchMode.JOIN)
        .setFetchMode("groups.events", FetchMode.JOIN)
        .setFetchMode("module", FetchMode.JOIN)
        .setFetchMode("department", FetchMode.JOIN)
        .distinct
    }, "id", allIds).sortBy { s => (s.module.adminDepartment.code, s.module.code, s.name) }
  }

  def findByModuleAndYear(module: Module, year: AcademicYear): Seq[SmallGroup] =
    session.newCriteria[SmallGroup]
      .createAlias("groupSet", "set")
      .add(is("set.module", module))
      .add(is("set.academicYear", year))
      .seq

  def findSmallGroupOccurrencesByGroup(group: SmallGroup): Seq[SmallGroupEventOccurrence] =
    session.newCriteria[SmallGroupEventOccurrence]
      .createAlias("event", "event")
      .add(is("event.group", group))
      .addOrder(asc("week"))
      .addOrder(asc("event.day"))
      .seq

  def findSmallGroupOccurrencesByEvent(event: SmallGroupEvent): Seq[SmallGroupEventOccurrence] =
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

  def findAttendanceNotes(studentIds: Seq[String], occurrences: Seq[SmallGroupEventOccurrence]): Seq[SmallGroupEventAttendanceNote] =
    if (studentIds.isEmpty || occurrences.isEmpty)
      Nil
    else
      session.newCriteria[SmallGroupEventAttendanceNote]
        .add(safeIn("student.id", studentIds))
        .add(safeIn("occurrence", occurrences))
        .seq

  def findSmallGroupsWithAttendanceRecorded(studentId: String): Seq[SmallGroup] =
    session.newCriteria[SmallGroupEventAttendance]
      .createAlias("occurrence", "occurrence")
      .createAlias("occurrence.event", "event")
      .add(is("universityId", studentId))
      .project[SmallGroup](distinct(groupProperty("event.group")))
      .seq

  def findManuallyAddedAttendance(studentId: String): Seq[SmallGroupEventAttendance] =
    session.newCriteria[SmallGroupEventAttendance]
      .add(is("universityId", studentId))
      .add(is("addedManually", true))
      .seq

  def findAttendanceForStudentsInWeeks(students: Seq[StudentMember], academicYear: AcademicYear, startWeek: Int, endWeek: Int): Seq[SmallGroupEventAttendance] = {
    session.newCriteria[SmallGroupEventAttendance]
      .createAlias("occurrence", "occurrence")
      .createAlias("occurrence.event", "event")
      .createAlias("event.group", "group")
      .createAlias("group.groupSet", "groupSet")
      .add(safeIn("universityId", students.map(_.universityId)))
      .add(is("groupSet.academicYear", academicYear))
      .add(ge("occurrence.week", startWeek))
      .add(le("occurrence.week", endWeek))
      .seq
  }

  def findAttendanceForStudentInModulesInWeeks(student: StudentMember, startWeek: Int, endWeek: Int, academicYear: AcademicYear, modules: Seq[Module]): Seq[SmallGroupEventAttendance] = {
    if (modules.isEmpty) {
      session.newCriteria[SmallGroupEventAttendance]
        .createAlias("occurrence", "occurrence")
        .createAlias("occurrence.event", "event")
        .createAlias("event.group", "group")
        .createAlias("group.groupSet", "groupSet")
        .add(is("universityId", student.universityId))
        .add(is("groupSet.academicYear", academicYear))
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
          .add(is("groupSet.academicYear", academicYear))
          .add(ge("occurrence.week", startWeek))
          .add(le("occurrence.week", endWeek))
      }
      safeInSeq(c, "groupSet.module", modules)
    }
  }

  override def findAllAttendanceForStudentInAcademicYear(student: StudentMember, academicYear: AcademicYear): Seq[SmallGroupEventAttendance] =
    session.newCriteria[SmallGroupEventAttendance]
      .createAlias("occurrence", "occurrence")
      .createAlias("occurrence.event", "event")
      .createAlias("event.group", "group")
      .createAlias("group.groupSet", "groupSet")
      .add(is("universityId", student.universityId))
      .add(is("groupSet.academicYear", academicYear))
      .seq

  def findOccurrencesInModulesInWeeks(startWeek: Int, endWeek: Int, modules: Seq[Module], academicYear: AcademicYear): Seq[SmallGroupEventOccurrence] = {
    if (modules.isEmpty) {
      Seq()
    } else {
      val c = () => {
        session.newCriteria[SmallGroupEventOccurrence]
          .createAlias("event", "event")
          .createAlias("event.group", "group")
          .createAlias("group.groupSet", "groupSet")
          .add(ge("week", startWeek))
          .add(le("week", endWeek))
          .add(is("groupSet.academicYear", academicYear))
          .setFetchMode("event", FetchMode.JOIN)
      }
      val occurrences = safeInSeq(c, "groupSet.module", modules)
      // Filter the occurrences in case any invalid ones still exist
      occurrences.filter(o => o.event.allWeeks.contains(o.week))
    }
  }

  def findOccurrencesInWeeks(startWeek: Int, endWeek: Int, academicYear: AcademicYear): Seq[SmallGroupEventOccurrence] = {
    session.newCriteria[SmallGroupEventOccurrence]
      .createAlias("event", "event")
      .createAlias("event.group", "group")
      .createAlias("group.groupSet", "groupSet")
      .add(ge("week", startWeek))
      .add(le("week", endWeek))
      .add(is("groupSet.academicYear", academicYear))
      .seq
  }

  def findStudentAttendanceInEvents(universityId: String, events: Seq[SmallGroupEvent]): Seq[SmallGroupEventAttendance] = {
    safeInSeq(() => {
      session.newCriteria[SmallGroupEventAttendance]
        .createAlias("occurrence", "occurrence")
        .add(is("universityId", universityId))
    }, "occurrence.event", events)
  }


  def hasSmallGroups(module: Module): Boolean = {
    session.newCriteria[SmallGroupSet]
      .add(is("module", module))
      .add(is("deleted", false))
      .count.intValue > 0
  }

  def hasSmallGroups(module: Module, academicYear: AcademicYear): Boolean = {
    session.newCriteria[SmallGroupSet]
      .add(is("module", module))
      .add(is("deleted", false))
      .add(is("academicYear", academicYear))
      .count.intValue > 0
  }

  def getDepartmentSmallGroupSets(department: Department, year: AcademicYear): Seq[DepartmentSmallGroupSet] = {
    session.newCriteria[DepartmentSmallGroupSet]
      .add(is("department", department))
      .add(is("academicYear", year))
      .add(is("deleted", false))
      .add(is("archived", false))
      .addOrder(asc("name"))
      .seq
  }

  def findDepartmentSmallGroupSetsLinkedToSITSByDepartment(year: AcademicYear): Map[Department, Seq[DepartmentSmallGroupSet]] = {
    session.newCriteria[DepartmentSmallGroupSet]
      .add(is("academicYear", year))
      .add(is("deleted", false))
      .add(is("archived", false))
      .add(isNotNull("memberQuery"))
      .addOrder(asc("name"))
      .seq
      .groupBy(_.department)
  }

  def findSmallGroupSetsLinkedToSITSByDepartment(year: AcademicYear): Map[Department, Seq[SmallGroupSet]] = {
    session.newCriteria[SmallGroupSet]
      .add(is("academicYear", year))
      .add(is("deleted", false))
      .add(is("archived", false))
      .add(isNotNull("memberQuery"))
      .addOrder(asc("name"))
      .seq
      .groupBy(_.module.adminDepartment)
  }

  def delete(occurrence: SmallGroupEventOccurrence): Unit = session.delete(occurrence)

  def findAttendedSmallGroupEvents(studentId: String): Seq[SmallGroupEventAttendance] =
    session.newCriteria[SmallGroupEventAttendance]
      .add(is("universityId", studentId))
      .add(is("state", AttendanceState.Attended))
      .seq

  def listSmallGroupEventsForReport(department: Department, academicYear: AcademicYear): Seq[SmallGroupEventReportData] = {
    // First get all the IDs
    val eventIDs = benchmarkTask("eventIDs") {
      session.newCriteria[SmallGroupEvent]
        .createAlias("group", "group")
        .createAlias("group.groupSet", "groupSet")
        .createAlias("groupSet.module", "module")
        .add(is("module.adminDepartment", department))
        .add(is("groupSet.academicYear", academicYear))
        .project[Array[java.lang.Object]](projectionList()
        .add(property("id"))
        .add(property("group.id"))
      ).seq
        .map(array => (array(0).asInstanceOf[String], array(1).asInstanceOf[String]))
    }

    // Now get the non-linked group sizes
    val nonLinkedGroupSizes: Map[String, Int] = benchmarkTask("nonLinkedGroupSizes") {
      safeInSeqWithProjection[SmallGroup, Array[java.lang.Object]](
        () => {
          session.newCriteria[SmallGroup]
            .createAlias("_studentsGroup", "usergroup")
            .createAlias("usergroup.includeUsers", "users")
            .add(isNull("linkedDepartmentSmallGroup"))
        },
        projectionList()
          .add(groupProperty("id"))
          .add(count("id")),
        "id",
        eventIDs.map(_._2)
      ).seq
        .map(objArray => (objArray(0).asInstanceOf[String], objArray(1).asInstanceOf[Long].toInt)).toMap
    }

    // Now get the linked group sizes
    val linkedGroupSizes: Map[String, Int] = benchmarkTask("linkedGroupSizes") {
      safeInSeqWithProjection[SmallGroup, Array[java.lang.Object]](
        () => {
          session.newCriteria[SmallGroup]
            .createAlias("linkedDepartmentSmallGroup", "linkedGroup")
            .createAlias("linkedGroup._studentsGroup", "usergroup")
            .createAlias("usergroup.includeUsers", "users")
        },
        projectionList()
          .add(groupProperty("id"))
          .add(count("id")),
        "id",
        eventIDs.map(_._2)
      ).seq
        .map(objArray => (objArray(0).asInstanceOf[String], objArray(1).asInstanceOf[Long].toInt)).toMap
    }

    // Now get the tutors for each event
    val tutors: Map[String, Seq[User]] = benchmarkTask("tutors") {
      safeInSeqWithProjection[SmallGroupEvent, Array[java.lang.Object]](
        () => {
          session.newCriteria[SmallGroupEvent]
            .createAlias("_tutors", "usergroup")
            .createAlias("usergroup.includeUsers", "users")
        },
        projectionList()
          .add(property("id"))
          .add(property("users.elements")),
        "id",
        eventIDs.map(_._1)
      ).map(objArray => (objArray(0).asInstanceOf[String], userLookup.getUserByUserId(objArray(1).asInstanceOf[String])))
       .groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    }

    // Now get the data and mix in the group sizes and tutors
    safeInSeqWithProjection[SmallGroupEvent, Array[java.lang.Object]](
      () => {
        session.newCriteria[SmallGroupEvent]
          .createAlias("group", "group")
          .createAlias("group.groupSet", "groupSet")
          .createAlias("groupSet.module", "module")
          .createAlias("group.linkedDepartmentSmallGroup", "linkedGroup", JoinType.LEFT_OUTER_JOIN)
          .add(is("module.adminDepartment", department))
          .add(is("groupSet.academicYear", academicYear))
      },
      projectionList()
        .add(property("id"))
        .add(property("group.id"))
        .add(property("groupSet.name"))
        .add(property("group._name"))
        .add(property("linkedGroup.name"))
        .add(property("title"))
        .add(property("module.name"))
        .add(property("day"))
        .add(property("startTime"))
        .add(property("endTime"))
        .add(property("location"))
        .add(property("weekRanges"))
      ,
      "id",
      eventIDs.map(_._1)
    ).seq.map(objArray => SmallGroupEventReportData(
      departmentName = department.name,
      eventName = Seq(
        Option(objArray(2).asInstanceOf[String]),
        Seq(Option(objArray(4).asInstanceOf[String]), Option(objArray(3).asInstanceOf[String])).flatten.headOption,
        Option(objArray(5).asInstanceOf[String])
      ).flatten.mkString(" - "),
      moduleTitle = objArray(6).asInstanceOf[String],
      day = Option(objArray(7)).map(_.asInstanceOf[DayOfWeek].getName).getOrElse(""),
      start = Option(objArray(8)).map(_.asInstanceOf[LocalTime].toString("HH:mm")).getOrElse(""),
      finish = Option(objArray(9)).map(_.asInstanceOf[LocalTime].toString("HH:mm")).getOrElse(""),
      location = Option(objArray(10)).map(_.asInstanceOf[Location].name).getOrElse(""),
      size = linkedGroupSizes.getOrElse(
        objArray(1).asInstanceOf[String],
        nonLinkedGroupSizes.getOrElse(
          objArray(1).asInstanceOf[String],
          0
        )
      ),
      weeks = Option(objArray(11)).map(_.asInstanceOf[Seq[WeekRange]].mkString(", ")).getOrElse(""),
      staff = tutors.getOrElse(objArray(0).asInstanceOf[String], Seq()).map(u => s"${u.getFullName} (${u.getUserId})").mkString(", ")
    ))

  }

  def listMemberDataForAllocation(members: Seq[Member], academicYear: AcademicYear): Map[Member, MemberAllocationData] = {
    val data = safeInSeqWithProjection[Member, Array[java.lang.Object]](
      () => {
        session.newCriteria[Member]
          .createAlias("mostSignificantCourse", "course")
          .createAlias("course.currentRoute", "route")
          .createAlias("course.studentCourseYearDetails", "studentCourseYearDetails")
          .add(is("studentCourseYearDetails.academicYear", academicYear))
      },
      projectionList()
        .add(property("universityId"))
        .add(property("route.code"))
        .add(property("route.name"))
        .add(property("studentCourseYearDetails.yearOfStudy"))
      ,
      "universityId",
      members.map(_.universityId)
    ).seq.map(objArray => objArray(0).asInstanceOf[String] -> MemberAllocationData(
      routeCode = objArray(1).asInstanceOf[String],
      routeName = objArray(2).asInstanceOf[String],
      yearOfStudy = objArray(3).asInstanceOf[Int]
    )).toMap
    members.map(member => member -> data.getOrElse(member.universityId, MemberAllocationData("", "", 0))).toMap
  }

  def listDepartmentSetsForMembershipUpdate: Seq[DepartmentSmallGroupSet] =
    session.newQuery[DepartmentSmallGroupSet](
      """
					from DepartmentSmallGroupSet
					where memberQuery is not null and length(memberQuery) > 0
			"""
    ).seq

  def listSetsForMembershipUpdate: Seq[SmallGroupSet] =
    session.newQuery[SmallGroupSet](
      """
					from SmallGroupSet
					where memberQuery is not null and length(memberQuery) > 0
			"""
    ).seq

  override def listSmallGroupsWithoutLocation(academicYear: AcademicYear, department: Option[Department]): Seq[SmallGroupEvent] = {
    val departmentCondition = department.map(_ => s"and s.module.adminDepartment = :department").getOrElse("")

    val query = session.newQuery[Array[java.lang.Object]](
      s"""
					from SmallGroupEvent e
					join e.group as g
					join g.groupSet as s
					where s.academicYear = :academicYear and location not like '%|%' and deliveryMethod != 'OnlineOnly'
		 			and s.deleted = false
		 			$departmentCondition
			""")
      .setParameter("academicYear", academicYear)

    department.foreach(d => query.setEntity("department", d))

    query.seq.map(objArray => objArray(0).asInstanceOf[SmallGroupEvent])
  }

  override def listSmallGroupsWithoutOnlineLocation(academicYear: AcademicYear, department: Option[Department]): Seq[SmallGroupEvent] = {
    val departmentCondition = department.map(_ => s"and s.module.adminDepartment = :department").getOrElse("")

    val query = session.newQuery[Array[java.lang.Object]](
      s"""
					from SmallGroupEvent e
					join e.group as g
					join g.groupSet as s
					where s.academicYear = :academicYear and (onlineDeliveryUrl is null OR onlineDeliveryUrl = '') and deliveryMethod != 'FaceToFaceOnly'
		 			and s.deleted = false
		 			$departmentCondition
			""")
      .setParameter("academicYear", academicYear)

    department.foreach(d => query.setEntity("department", d))

    query.seq.map(objArray => objArray(0).asInstanceOf[SmallGroupEvent])
  }

  def findSmallGroupsByNameOrModule(query: FindSmallGroupQuery): Seq[SmallGroup] = {
    val indexedTerms = query.terms.zipWithIndex

    val termConditions = indexedTerms.zipWithIndex.map { case (_, i) => s"""(
			(
			 lower(g._name) like :ot$i or
			 lower(g._name) like :fw$i or
       lower(g._name) like :lw$i
			 )
			or
			(
			 lower(g.groupSet.name) like :ot$i or
			 lower(g.groupSet.name) like :fw$i or
			 lower(g.groupSet.name) like :lw$i
			)
		)"""
    }.mkString(" and ")

    val termCondition = if (termConditions.nonEmpty) s" and ($termConditions)" else ""
    val departmentCondition = if (query.department.isDefined) " and g.groupSet.module.adminDepartment.code = :department" else ""
    val moduleCondition = if (query.modules.nonEmpty) " and g.groupSet.module.code in (:modules)" else ""

    val hql =
      s"""
			from SmallGroup g
			where g.groupSet.deleted = false and g.groupSet.academicYear = :academicYear
			$termCondition
			$departmentCondition
	 		$moduleCondition
		"""

    val scalaQuery = session.newQuery[SmallGroup](hql)
      .setParameter("academicYear", query.academicYear)
      .setMaxResults(MaxGroupsByName)

    indexedTerms.foreach { case (t, i) => scalaQuery.setString(s"fw$i", t.toLowerCase + " %") }
    indexedTerms.foreach { case (t, i) => scalaQuery.setString(s"ot$i", "% " + t.toLowerCase + " %") }
    indexedTerms.foreach { case (t, i) => scalaQuery.setString(s"lw$i", "% " + t.toLowerCase) }
    if (query.modules.nonEmpty) scalaQuery.setParameterList("modules", query.modules.map(_.toLowerCase))
    query.department.foreach(d => scalaQuery.setParameter("department", d))

    scalaQuery.seq
  }

}
