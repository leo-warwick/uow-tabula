package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{MemberStudentRelationship, _}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString

import scala.collection.JavaConverters._

trait MemberDaoComponent {
	val memberDao: MemberDao
}

trait AutowiringMemberDaoComponent extends MemberDaoComponent {
	val memberDao = Wire[MemberDao]
}

trait MemberDao {
	def saveOrUpdate(member: Member)
	def delete(member: Member)
	def getByUniversityId(universityId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false): Option[Member]
	def getByUniversityIdStaleOrFresh(universityId: String): Option[Member]
	def getAllWithUniversityIds(universityIds: Seq[String]): Seq[Member]
	def getAllWithUniversityIdsStaleOrFresh(universityIds: Seq[String]): Seq[Member]
	def getAllByUserId(userId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly: Boolean = true): Seq[Member]
	def listUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def listUpdatedSince(startDate: DateTime, department: Department, max: Int): Seq[Member]
	def listUpdatedSince(startDate: DateTime): Scrollable[Member]
	def listUpdatedSince(startDate: DateTime, department: Department): Scrollable[Member]
	def countUpdatedSince(startDate: DateTime): Int

	def getStudentsByDepartment(department: Department): Seq[StudentMember]
	def getStaffByDepartment(department: Department): Seq[StaffMember]

	def findUniversityIdsByRestrictions(restrictions: Iterable[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()): Seq[String]
	def findAllStudentDataByRestrictions(restrictions: Iterable[ScalaRestriction], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData]
	def findStudentsByRestrictions(restrictions: Iterable[ScalaRestriction], orders: Iterable[ScalaOrder], maxResults: Int, startResult: Int): Seq[StudentMember]
	def getSCDsByAgentRelationshipAndRestrictions(
		relationshipType: StudentRelationshipType,
		agentId: String,
		restrictions: Seq[ScalaRestriction]
	): Seq[StudentCourseDetails]
	def countStudentsByRestrictions(restrictions: Iterable[ScalaRestriction]): Int

	def getAllModesOfAttendance(department: Department): Seq[ModeOfAttendance]
	def getAllSprStatuses(department: Department): Seq[SitsStatus]

	def getFreshUniversityIds(): Seq[String]
	def stampMissingFromImport(newStaleUniversityIds: Seq[String], importStart: DateTime)
	def getDisability(code: String): Option[Disability]

	def getMemberByTimetableHash(timetableHash: String): Option[Member]
	def setTimetableHash(member: Member, timetableHash: String)

	def cleanFixturesData()
	def evict(member: Member)
	def flush()
	def clear()

}

@Repository
class MemberDaoImpl extends MemberDao with Daoisms with Logging with AttendanceMonitoringStudentDataFetcher {
	import org.hibernate.criterion.Order._
	import org.hibernate.criterion.Projections._
	import org.hibernate.criterion.Restrictions._


	def saveOrUpdate(member: Member) = member match {
		case ignore: RuntimeMember => // shouldn't ever get here, but making sure
		case _ => session.saveOrUpdate(member)
	}

	def delete(member: Member) = member match {
		case ignore: RuntimeMember => // shouldn't ever get here, but making sure
		case _ =>
			session.delete(member)
			// Immediately flush delete
			session.flush()
	}

	def getByUniversityId(universityId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false) = {
		val filterEnabled = Option(session.getEnabledFilter(Member.StudentsOnlyFilter)).isDefined
		try {
			if (disableFilter)
				session.disableFilter(Member.StudentsOnlyFilter)

			val criteria =
				session.newCriteria[Member]
					.add(is("universityId", universityId.safeTrim))

			if (eagerLoad) {
				criteria
					.setFetchMode("studentCourseDetails", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.studentCourseYearDetails", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.moduleRegistrations", FetchMode.JOIN)
					.setFetchMode("homeDepartment", FetchMode.JOIN)
					.setFetchMode("homeDepartment.children", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.studentCourseYearDetails.enrolmentDepartment", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.studentCourseYearDetails.enrolmentDepartment.children", FetchMode.JOIN)
					.uniqueResult.map { m =>
						// This is the worst hack of all time
						m.permissionsParents.force
						m
					}
			} else {
				criteria.uniqueResult
			}
		} finally {
			if (disableFilter && filterEnabled)
				session.enableFilter(Member.StudentsOnlyFilter)
		}
	}

	def getByUniversityIdStaleOrFresh(universityId: String) = {
		val member = sessionWithoutFreshFilters.newCriteria[Member]
			.add(is("universityId", universityId.safeTrim))
			.uniqueResult
		member
	}

	def getFreshUniversityIds() =
			session.newCriteria[StudentMember]
			.project[String](Projections.property("universityId"))
			.seq

	def getAllWithUniversityIds(universityIds: Seq[String]) =
		if (universityIds.isEmpty) Seq.empty
		else safeInSeq(() => { session.newCriteria[Member] }, "universityId", universityIds map { _.safeTrim })

	def getAllWithUniversityIdsStaleOrFresh(universityIds: Seq[String]) = {
		if (universityIds.isEmpty) Seq.empty
		else safeInSeq(() => { sessionWithoutFreshFilters.newCriteria[Member] }, "universityId", universityIds map { _.safeTrim })
	}

	def getAllByUserId(userId: String, disableFilter: Boolean = false, eagerLoad: Boolean = false, activeOnly: Boolean = true) = {
		val filterEnabled = Option(session.getEnabledFilter(Member.StudentsOnlyFilter)).isDefined
		try {
			if (disableFilter)
				session.disableFilter(Member.StudentsOnlyFilter)

			val criteria =
				session.newCriteria[Member]
					.add(is("userId", userId.safeTrim.toLowerCase))
					.addOrder(asc("universityId"))
			if (activeOnly)
				criteria.add(disjunction()
					.add(is("inUseFlag", "Active"))
					.add(like("inUseFlag", "Inactive - Starts %"))
				)


			if (eagerLoad) {
				criteria
					.setFetchMode("studentCourseDetails", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.studentCourseYearDetails", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.moduleRegistrations", FetchMode.JOIN)
					.setFetchMode("homeDepartment", FetchMode.JOIN)
					.setFetchMode("homeDepartment.children", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.studentCourseYearDetails.enrolmentDepartment", FetchMode.JOIN)
					.setFetchMode("studentCourseDetails.studentCourseYearDetails.enrolmentDepartment.children", FetchMode.JOIN)
					.distinct
					.seq.map { m =>
						// This is the worst hack of all time
						m.permissionsParents.force
						m
					}
			} else {
				criteria.seq
			}
		} finally {
			if (disableFilter && filterEnabled)
				session.enableFilter(Member.StudentsOnlyFilter)
		}
	}

	def listUpdatedSince(startDate: DateTime, department: Department, max: Int) = {
		val homeDepartmentMatches = session.newCriteria[Member]
			.add(gt("lastUpdatedDate", startDate))
			.add(is("homeDepartment", department))
			.setMaxResults(max)
			.addOrder(asc("lastUpdatedDate"))
			.list

		val courseMatches = session.newQuery[StudentMember]( """
				select student
        	from
          	StudentCourseDetails scd
          where
            scd.department = :department and
        		scd.student.lastUpdatedDate > :lastUpdated and
            scd.statusOnRoute.code not like 'P%' """)
			.setEntity("department", department)
			.setParameter("lastUpdated", startDate).seq.distinct

		// do not remove; import needed for sorting
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		(homeDepartmentMatches.asScala ++ courseMatches).distinct.sortBy(_.lastUpdatedDate)
	}

	def listUpdatedSince(startDate: DateTime, max: Int) =
		session.newQuery[Member]( """select staffOrStudent from Member staffOrStudent
			where staffOrStudent.lastUpdatedDate > :lastUpdated
			order by lastUpdatedDate asc
		""")
			.setParameter("lastUpdated", startDate)
			.setMaxResults(max).seq.distinct

	def listUpdatedSince(startDate: DateTime) = {
		val scrollableResults = session.newCriteria[Member]
			.add(gt("lastUpdatedDate", startDate))
			.addOrder(asc("lastUpdatedDate"))
			.scroll()
		session.scrollable(scrollableResults)
	}

	def listUpdatedSince(startDate: DateTime, department: Department) = {
		val scrollableResults = session.newCriteria[Member]
			.createAlias("studentCourseDetails", "scd")
			.add(gt("lastUpdatedDate", startDate))
			.add(
				disjunction()
					.add(is("homeDepartment", department))
					.add(
						conjunction()
							.add(is("scd.department", department))
							.add(not(like("scd.statusOnRoute.code", "P%")))
					)
			)
			.addOrder(asc("lastUpdatedDate"))
			.scroll()

		session.scrollable(scrollableResults)
	}

	def countUpdatedSince(startDate: DateTime): Int =
		session.newCriteria[Member]
			.add(gt("lastUpdatedDate", startDate))
			.project[Number](count("universityId")).uniqueResult.get.intValue()


	def getAllCurrentRelationships(student: StudentMember): Seq[StudentRelationship] = {
			session.newCriteria[StudentRelationship]
					.createAlias("studentCourseDetails", "scd")
					.add(is("scd.student", student))
					.add( Restrictions.or(
							Restrictions.isNull("endDate"),
							Restrictions.ge("endDate", new DateTime())
							))
					.seq
	}

	/**
	 * n.b. this will only return students with a direct relationship to a department. For sub-department memberships,
	 * see ProfileService/RelationshipService
	 */
	def getStudentsByDepartment(department: Department): Seq[StudentMember] =
		if (department == null) Nil
		else {

			val s = session.newQuery[StudentMember]("""
			select student
			from
				StudentCourseDetails scd
			where
				scd.department = :department
			and
				scd.mostSignificant = true
			and
				scd.statusOnRoute.code not like 'P%'
			""")
			.setEntity("department", department).seq.distinct
			s
		}

	def getStaffByDepartment(department: Department): Seq[StaffMember] =
		if (department == null) Nil
		else {
			session.newCriteria[StaffMember]
				.add(is("homeDepartment", department))
				.seq
		}

	def findUniversityIdsByRestrictions(restrictions: Iterable[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()): Seq[String] = {
		val idCriteria = session.newCriteria[StudentMember]
		restrictions.foreach { _.apply(idCriteria) }

		if (orders.nonEmpty) {
			orders.foreach { idCriteria.addOrder }
			idCriteria.project[String](property("universityId")).seq.distinct
		} else {
			idCriteria.project[String](distinct(property("universityId"))).seq
		}
	}

	def findAllStudentDataByRestrictions(restrictions: Iterable[ScalaRestriction], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData] = {
		val idCriteria = session.newCriteria[StudentMember]
		restrictions.foreach { _.apply(idCriteria) }

		val universityIds = idCriteria.project[String](property("universityId")).seq

		getAttendanceMonitoringDataForStudents(universityIds, academicYear)
	}

	def findStudentsByRestrictions(
		restrictions: Iterable[ScalaRestriction],
		orders: Iterable[ScalaOrder],
		maxResults: Int,
		startResult: Int
	): Seq[StudentMember] = {
		val universityIds = findUniversityIdsByRestrictions(restrictions)

		if (universityIds.isEmpty)
			return Seq()

		val c = session.newCriteria[StudentMember].add(safeIn("universityId", universityIds))

		// TODO Is there a way of doing multiple safeIn queries with DB-set prders and max results?
		orders.foreach { c.addOrder }

		c.setMaxResults(maxResults).setFirstResult(startResult).distinct.seq
	}

	def getSCDsByAgentRelationshipAndRestrictions(
		relationshipType: StudentRelationshipType,
		agentId: String,
		restrictions: Seq[ScalaRestriction]
	): Seq[StudentCourseDetails] = {
		if (relationshipType == null) Nil
		else {
			val d = DetachedCriteria.forClass(classOf[MemberStudentRelationship])
				.setProjection(Property.forName("studentCourseDetails.scjCode"))
				.add(Restrictions.eq("_agentMember.universityId", agentId))
				.add(Restrictions.eq("relationshipType", relationshipType))
				.add( Restrictions.or(
				Restrictions.isNull("endDate"),
				Restrictions.ge("endDate", new DateTime())
			))

			val c = session.newCriteria[StudentCourseDetails]
			restrictions.foreach { _.apply(c) }
			c.add(Property.forName("scjCode").in(d)).seq
		}
	}

	def countStudentsByRestrictions(restrictions: Iterable[ScalaRestriction]) = {
		val c = session.newCriteria[StudentMember]
		restrictions.foreach { _.apply(c) }

		c.project[Number](countDistinct("universityId")).uniqueResult.get.intValue()
	}

	def getAllModesOfAttendance(department: Department) =
		session.newCriteria[StudentMember]
				.createAlias("mostSignificantCourse", "scd")
				.createAlias("scd.latestStudentCourseYearDetails", "scyd")
				.add(is("scd.department", department))
				.addOrder(desc("moaCount"))
				.project[Array[Any]](
					projectionList()
						.add(groupProperty("scyd.modeOfAttendance"))
						.add(rowCount(), "moaCount")
				)
				.seq.map { array => array(0).asInstanceOf[ModeOfAttendance] }

	def getAllSprStatuses(department: Department) =
		session.newCriteria[StudentMember]
				.createAlias("mostSignificantCourse", "scd")
				.add(is("scd.department", department))
				.addOrder(desc("statusCount"))
				.project[Array[Any]](
					projectionList()
						.add(groupProperty("scd.statusOnRoute"))
						.add(rowCount(), "statusCount")
				)
				.seq.map { array => array(0).asInstanceOf[SitsStatus] }

	def stampMissingFromImport(newStaleUniversityIds: Seq[String], importStart: DateTime) = {
		newStaleUniversityIds.grouped(Daoisms.MaxInClauseCount).foreach { staleIds =>
			val hqlString = """
				update
					Member
				set
					missingFromImportSince = :importStart
				where
					universityId in (:newStaleUniversityIds)
				"""

				session.newQuery(hqlString)
					.setParameter("importStart", importStart)
					.setParameterList("newStaleUniversityIds", staleIds)
					.executeUpdate()
			}
	}

	def getDisability(code: String): Option[Disability] = {
		session.newCriteria[Disability]
			.add(is("code", code))
			.uniqueResult
	}

	def getMemberByTimetableHash(timetableHash: String): Option[Member] = {
		session.newCriteria[Member]
		.add(is("timetableHash", timetableHash))
		.uniqueResult
	}

	def setTimetableHash(member: Member, timetableHash: String) = member match {
		case ignore: RuntimeMember => // shouldn't ever get here, but making sure
		case _ =>
			session.newQuery("update Member set timetableHash = :timetableHash where universityId = :universityId")
				.setParameter("timetableHash", timetableHash)
				.setParameter("universityId", member.universityId)
				.executeUpdate()
	}

	def evict(member: Member) = session.evict(member)
	def flush() = session.flush()
	def clear() = session.clear()

	def cleanFixturesData(): Unit = {
		sessionWithoutFreshFilters.newQuery("delete from StudentCourseYearDetails where studentCourseDetails.scjCode like '3000%'").executeUpdate()
		sessionWithoutFreshFilters.newQuery("delete from ModuleRegistration where studentCourseDetails.scjCode like '3000%'").executeUpdate()
		sessionWithoutFreshFilters.newQuery("delete from MemberStudentRelationship where studentCourseDetails.scjCode like '3000%'").executeUpdate()
		sessionWithoutFreshFilters.newQuery("delete from StudentCourseDetails where scjCode like '3000%'").executeUpdate()
		sessionWithoutFreshFilters.newQuery("delete from StudentMember where universityId like '3000%'").executeUpdate()
	}
}