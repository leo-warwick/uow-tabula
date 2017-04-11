package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Projections._
import org.hibernate.criterion.Restrictions._
import org.hibernate.criterion.{Order, ProjectionList, Projections, Restrictions}
import org.joda.time.{DateTime, LocalDate}
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.HibernateHelpers._
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.services.TermService

abstract class SchemeMembershipItemType(val value: String)
case object SchemeMembershipStaticType extends SchemeMembershipItemType("static")
case object SchemeMembershipIncludeType extends SchemeMembershipItemType("include")
case object SchemeMembershipExcludeType extends SchemeMembershipItemType("exclude")

/**
 * Item in list of members for displaying in view.
 */
case class SchemeMembershipItem(
	itemType: SchemeMembershipItemType, // static, include or exclude
	firstName: String,
	lastName: String,
	universityId: String,
	userId: String,
	existingSchemes: Seq[AttendanceMonitoringScheme]
) {
	def itemTypeString: String = itemType.value
}

/**
 * Checkpoint without associated StudentMember to save extra query
 */
case class AttendanceMonitoringCheckpointData(
	point: AttendanceMonitoringPoint,
	state: AttendanceState,
	universityId: String
)

trait AttendanceMonitoringDaoComponent {
	val attendanceMonitoringDao: AttendanceMonitoringDao
}

trait AutowiringAttendanceMonitoringDaoComponent extends AttendanceMonitoringDaoComponent {
	val attendanceMonitoringDao: AttendanceMonitoringDao = Wire[AttendanceMonitoringDao]
}

trait AttendanceMonitoringDao {
	def flush(): Unit
	def getSchemeById(id: String): Option[AttendanceMonitoringScheme]
	def getPointById(id: String): Option[AttendanceMonitoringPoint]
	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit
	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit
	def saveOrUpdate(total: AttendanceMonitoringCheckpointTotal): Unit
	def saveOrUpdate(template: AttendanceMonitoringTemplate): Unit
	def saveOrUpdate(templatePoint: AttendanceMonitoringTemplatePoint): Unit
	def saveOrUpdate(note: AttendanceMonitoringNote): Unit
	def saveOrUpdate(report: MonitoringPointReport): Unit
	def delete(scheme: AttendanceMonitoringScheme)
	def delete(point: AttendanceMonitoringPoint)
	def delete(template: AttendanceMonitoringTemplate)
	def delete(templatePoint: AttendanceMonitoringTemplatePoint)
	def getTemplateSchemeById(id: String): Option[AttendanceMonitoringTemplate]
	def getTemplatePointById(id: String): Option[AttendanceMonitoringTemplatePoint]
	def listAllSchemes(department: Department): Seq[AttendanceMonitoringScheme]
	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme]
	def listAllTemplateSchemes: Seq[AttendanceMonitoringTemplate]
	def listTemplateSchemesByStyle(style: AttendanceMonitoringPointStyle): Seq[AttendanceMonitoringTemplate]
	def listSchemesForMembershipUpdate: Seq[AttendanceMonitoringScheme]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def findReports(studentIds: Seq[String], year: AcademicYear, period: String): Seq[MonitoringPointReport]
	def listUnreportedReports: Seq[MonitoringPointReport]
	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem]
	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint]
	def getAllCheckpoints(point: AttendanceMonitoringPoint): Seq[AttendanceMonitoringCheckpoint]
	def getAllCheckpoints(points: Seq[AttendanceMonitoringPoint]): Map[AttendanceMonitoringPoint, Seq[AttendanceMonitoringCheckpoint]]
	def getAllCheckpointData(points: Seq[AttendanceMonitoringPoint]): Seq[AttendanceMonitoringCheckpointData]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]
	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], students: Seq[StudentMember]): Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]]
	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int
	def countCheckpointsForPoints(points: Seq[AttendanceMonitoringPoint]): Map[AttendanceMonitoringPoint, Int]
	def getNonActiveCheckpoints(
		student: StudentMember,
		departmentOption: Option[Department],
		academicYear: AcademicYear,
		activeCheckpoints: Seq[AttendanceMonitoringCheckpoint]
	): Seq[AttendanceMonitoringCheckpoint]
	def hasRecordedCheckpoints(points: Seq[AttendanceMonitoringPoint]): Boolean
	def removeCheckpoints(checkpoints: Seq[AttendanceMonitoringCheckpoint]): Unit
	def saveOrUpdateCheckpoints(checkpoints: Seq[AttendanceMonitoringCheckpoint]): Unit
	def getAllAttendance(studentId: String): Seq[AttendanceMonitoringCheckpoint]
	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote]
	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote]
	def getCheckpointTotal(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear, withFlush: Boolean = false): Option[AttendanceMonitoringCheckpointTotal]
	def getAllCheckpointTotals(department: Department): Seq[AttendanceMonitoringCheckpointTotal]
	def getCheckpointTotals(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringCheckpointTotal]
	def findRelevantPoints(department: Department, academicYear: AcademicYear, endDate: LocalDate): Seq[AttendanceMonitoringPoint]
	def findSchemesLinkedToSITSByDepartment(academicYear: AcademicYear): Map[Department, Seq[AttendanceMonitoringScheme]]
	def getAttendanceMonitoringDataForStudents(universityIds: Seq[String], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData]
	def setCheckpointTotalsForUpdate(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Unit
	def listCheckpointTotalsForUpdate: Seq[AttendanceMonitoringCheckpointTotal]
}

@Repository
class AutowiringAttendanceMonitoringDao extends AttendanceMonitoringDaoImpl with Daoisms

class AttendanceMonitoringDaoImpl extends AttendanceMonitoringDao with AttendanceMonitoringStudentDataFetcher {
	self: ExtendedSessionComponent =>

	def flush(): Unit = session.flush()

	def getSchemeById(id: String): Option[AttendanceMonitoringScheme] =
		getById[AttendanceMonitoringScheme](id)

	def getPointById(id: String): Option[AttendanceMonitoringPoint] =
		getById[AttendanceMonitoringPoint](id)

	def saveOrUpdate(scheme: AttendanceMonitoringScheme): Unit =
		session.saveOrUpdate(scheme)

	def saveOrUpdate(point: AttendanceMonitoringPoint): Unit =
		session.saveOrUpdate(point)

	def saveOrUpdate(total: AttendanceMonitoringCheckpointTotal): Unit =
		session.saveOrUpdate(total)

	def saveOrUpdate(template: AttendanceMonitoringTemplate): Unit =
		session.saveOrUpdate(template)

	def saveOrUpdate(templatePoint: AttendanceMonitoringTemplatePoint): Unit =
		session.saveOrUpdate(templatePoint)

	def saveOrUpdate(note: AttendanceMonitoringNote): Unit =
		session.saveOrUpdate(note)

	def saveOrUpdate(report: MonitoringPointReport): Unit =
		session.saveOrUpdate(report)

	def delete(scheme: AttendanceMonitoringScheme): Unit =
		session.delete(scheme)

	def delete(point: AttendanceMonitoringPoint): Unit =
		session.delete(point)

	def delete(template: AttendanceMonitoringTemplate): Unit =
		session.delete(template)

	def delete(templatePoint: AttendanceMonitoringTemplatePoint): Unit =
		session.delete(templatePoint)

	def getTemplateSchemeById(id: String): Option[AttendanceMonitoringTemplate] =
		getById[AttendanceMonitoringTemplate](id)

	def getTemplatePointById(id: String): Option[AttendanceMonitoringTemplatePoint] =
		getById[AttendanceMonitoringTemplatePoint](id)

	def listAllSchemes(department: Department): Seq[AttendanceMonitoringScheme] = {
		session.newCriteria[AttendanceMonitoringScheme]
			.add(is("department", department))
			.seq
	}

	def listSchemes(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringScheme] = {
		session.newCriteria[AttendanceMonitoringScheme]
			.add(is("academicYear", academicYear))
			.add(is("department", department))
			.seq
	}

	def listAllTemplateSchemes: Seq[AttendanceMonitoringTemplate] = {
		session.newCriteria[AttendanceMonitoringTemplate]
			.addOrder(Order.asc("position"))
			.seq
	}

	def listTemplateSchemesByStyle(style: AttendanceMonitoringPointStyle): Seq[AttendanceMonitoringTemplate] = {
		session.newCriteria[AttendanceMonitoringTemplate]
			.add(is("pointStyle", style))
			.addOrder(Order.asc("position"))
			.seq
	}

	def listSchemesForMembershipUpdate: Seq[AttendanceMonitoringScheme] =
		session.newQuery[AttendanceMonitoringScheme](
			"""
				select scheme from AttendanceMonitoringScheme scheme
				where memberQuery is not null and length(memberQuery) > 0
			"""
			).seq

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] = {
		if (students.isEmpty)
			return Seq()

		val termCounts = {
			safeInSeqWithProjection[MonitoringPointReport, Array[java.lang.Object]](
				() => {
					session.newCriteria[MonitoringPointReport]
						.add(is("academicYear", academicYear))
				},
				Projections.projectionList()
					.add(Projections.groupProperty("monitoringPeriod"))
					.add(Projections.count("monitoringPeriod")),
				"student.universityId",
				students.map(_.universityId)
			).map { objArray =>
				objArray(0).asInstanceOf[String] -> objArray(1).asInstanceOf[Long].toInt
			}
		}

		// the safeInSeq does multiple queries which will mess up the group-by, returning e.g. (Autumn,1) and (Autumn,3)
		// separately. This will merge it back into (Autumn,4)
		val mergedTermCounts = termCounts.groupBy(_._1)
			.mapValues { value => value.map(_._2).sum } // christ.

		val reportedTerms = mergedTermCounts.toSeq
			.filter { case (term, count) => count.intValue() == students.size}
			.map { _._1 }
		TermService.orderedTermNames diff reportedTerms
	}

	def findReports(studentsIds: Seq[String], academicYear: AcademicYear, period: String): Seq[MonitoringPointReport] = {
		if (studentsIds.isEmpty)
			return Seq()

		safeInSeq(() => {
			session.newCriteria[MonitoringPointReport]
				.add(is("academicYear", academicYear))
				.add(is("monitoringPeriod", period))
			},
			"student.universityId",
			studentsIds
		)
	}

	def listUnreportedReports: Seq[MonitoringPointReport] = {
		session.newCriteria[MonitoringPointReport].add(isNull("pushedDate")).seq
	}

	def findSchemeMembershipItems(universityIds: Seq[String], itemType: SchemeMembershipItemType): Seq[SchemeMembershipItem] = {
		if (universityIds.isEmpty)
			return Seq()

		val items = safeInSeqWithProjection[StudentMember, Array[java.lang.Object]](
			() => {
				session.newCriteria[StudentMember]
			},
				Projections.projectionList()
					.add(Projections.property("firstName"))
					.add(Projections.property("lastName"))
					.add(Projections.property("universityId"))
					.add(Projections.property("userId")),
				"universityId",
				universityIds
			).seq.map { objArray =>
			SchemeMembershipItem(
				itemType,
				objArray(0).asInstanceOf[String],
				objArray(1).asInstanceOf[String],
				objArray(2).asInstanceOf[String],
				objArray(3).asInstanceOf[String],
				Seq() // mixed in by the service
			)
		}

		// keep the same order
		universityIds.flatMap(uniId => items.find(_.universityId == uniId))
	}

	def findPoints(
		department: Department,
		academicYear: AcademicYear,
		schemes: Seq[AttendanceMonitoringScheme],
		types: Seq[AttendanceMonitoringPointType],
		styles: Seq[AttendanceMonitoringPointStyle]
	): Seq[AttendanceMonitoringPoint] = {
		val query = session.newCriteria[AttendanceMonitoringPoint]
			.createAlias("scheme", "scheme")
			.add(is("scheme.department", department))
			.add(is("scheme.academicYear", academicYear))

		if (schemes.nonEmpty)
			query.add(safeIn("scheme", schemes))
		if (types.nonEmpty)
			query.add(safeIn("pointType", types))
		if (styles.nonEmpty)
			query.add(safeIn("scheme.pointStyle", styles))

		query.seq
	}

	def getAllCheckpoints(point: AttendanceMonitoringPoint): Seq[AttendanceMonitoringCheckpoint] = {
		session.newCriteria[AttendanceMonitoringCheckpoint]
			.add(is("point", point))
			.seq
	}

	def getAllCheckpointData(points: Seq[AttendanceMonitoringPoint]): Seq[AttendanceMonitoringCheckpointData] = {
		val result = safeInSeqWithProjection[AttendanceMonitoringCheckpoint, Array[java.lang.Object]](
			() => { session.newCriteria[AttendanceMonitoringCheckpoint] },
			Projections.projectionList()
				.add(property("point"))
				.add(property("_state"))
				.add(property("student.universityId")),
			"point",
			points
		)
		result.map(objArray => AttendanceMonitoringCheckpointData(
			objArray(0).asInstanceOf[AttendanceMonitoringPoint],
			objArray(1).asInstanceOf[AttendanceState],
			objArray(2).asInstanceOf[String]
		))
	}

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], student: StudentMember, withFlush: Boolean = false): Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint] = {
		if (withFlush)
			session.flush()

		if (points.isEmpty)
			Map()
		else {
			val checkpoints = safeInSeq(
				() => {
					session.newCriteria[AttendanceMonitoringCheckpoint]
						.add(is("student", student))
				},
				"point",
				points
			)

			checkpoints.map { c => c.point -> c}.toMap
		}
	}

	def getCheckpoints(points: Seq[AttendanceMonitoringPoint], students: Seq[StudentMember]): Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint]] = {
		if (points.isEmpty || students.isEmpty)
			Map()
		else {
			val checkpoints = session.newCriteria[AttendanceMonitoringCheckpoint]
				// TODO Is there a way to do multiple and'd safeIns in multiple queries?
				.add(safeIn("student", students))
				.add(safeIn("point", points))
				.seq

			checkpoints.groupBy(_.student).mapValues(_.groupBy(_.point).mapValues(_.head))
		}
	}

	def countCheckpointsForPoint(point: AttendanceMonitoringPoint): Int =
		session.newCriteria[AttendanceMonitoringCheckpoint]
			.add(is("point", point))
			.count.intValue

	def getNonActiveCheckpoints(
		student: StudentMember,
		departmentOption: Option[Department],
		academicYear: AcademicYear,
		activeCheckpoints: Seq[AttendanceMonitoringCheckpoint]
	): Seq[AttendanceMonitoringCheckpoint] = {
		val c = session.newCriteria[AttendanceMonitoringCheckpoint]
			.createAlias("point", "point")
			.createAlias("point.scheme", "scheme")
			.add(is("student", student))
			.add(is("scheme.academicYear", academicYear))
		if (activeCheckpoints.nonEmpty)
			// TODO Is there a way to do not-in with multiple queries?
			c.add(Restrictions.not(safeIn("id", activeCheckpoints.map(_.id))))
		departmentOption match {
			case Some(department: Department) => c.add(is("scheme.department", department))
			case _ =>
		}
		c.seq.map{c =>
			c.activePoint = false
			c
		}
	}

	def hasRecordedCheckpoints(points: Seq[AttendanceMonitoringPoint]): Boolean = {
		if (points.isEmpty)
			false
		else {
			safeInSeqWithProjection[AttendanceMonitoringCheckpoint, Number](() => { session.newCriteria[AttendanceMonitoringCheckpoint] }, Projections.rowCount(), "point", points).headOption.exists(_.intValue() > 0)
		}
	}

	def removeCheckpoints(checkpoints: Seq[AttendanceMonitoringCheckpoint]): Unit =
		checkpoints.foreach(session.delete)

	def saveOrUpdateCheckpoints(checkpoints: Seq[AttendanceMonitoringCheckpoint]): Unit =
		checkpoints.foreach(session.saveOrUpdate)

	def getAllAttendance(studentId: String): Seq[AttendanceMonitoringCheckpoint] = {
		session.newCriteria[AttendanceMonitoringCheckpoint]
			.createAlias("point", "point") // Don't use the alias, but only return checkpoints with associated points
			.createAlias("point.scheme", "scheme")
			.setFetchMode("point", FetchMode.JOIN) // Eagerly get the point
			.setFetchMode("point.scheme", FetchMode.JOIN) // Eagerly get the scheme
			.add(is("student.universityId", studentId))
			.seq
	}

	def getAttendanceNote(student: StudentMember, point: AttendanceMonitoringPoint): Option[AttendanceMonitoringNote] = {
		session.newCriteria[AttendanceMonitoringNote]
			.add(is("student", student))
			.add(is("point", point))
			.uniqueResult
	}

	def getAttendanceNoteMap(student: StudentMember): Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] = {
		val notes = session.newCriteria[AttendanceMonitoringNote]
			.createAlias("point", "point") // We don't reference the alias, but it means only notes where the point still exists are returned
			.add(is("student", student))
			.seq

		notes.map { n => n.point -> n}.toMap

	}

	def getCheckpointTotal(student: StudentMember, departmentOption: Option[Department], academicYear: AcademicYear, withFlush: Boolean = false): Option[AttendanceMonitoringCheckpointTotal] = {
		if (withFlush)
			// make sure totals are up-to-date
			session.flush()

		departmentOption match {
			case Some(department) => session.newCriteria[AttendanceMonitoringCheckpointTotal]
				.add(is("student", student))
				.add(is("department", department))
				.add(is("academicYear", academicYear))
				.uniqueResult
			case None =>
				val totals = session.newCriteria[AttendanceMonitoringCheckpointTotal]
					.add(is("student", student))
					.add(is("academicYear", academicYear))
					.seq
				if (totals.isEmpty) {
					None
				} else {
					val result = new AttendanceMonitoringCheckpointTotal
					result.student = student
					result.academicYear = academicYear
					result.unrecorded = totals.map(_.unrecorded).sum
					result.authorised = totals.map(_.authorised).sum
					result.unauthorised = totals.map(_.unauthorised).sum
					result.attended = totals.map(_.attended).sum
					result.updatedDate = DateTime.now
					Option(result)
				}
		}

	}

	def getAllCheckpointTotals(department: Department): Seq[AttendanceMonitoringCheckpointTotal] = {
		session.newCriteria[AttendanceMonitoringCheckpointTotal]
			.add(is("department", department))
			.seq
	}

	def getCheckpointTotals(department: Department, academicYear: AcademicYear): Seq[AttendanceMonitoringCheckpointTotal] = {
		session.newCriteria[AttendanceMonitoringCheckpointTotal]
			.add(is("department", department))
			.add(is("academicYear", academicYear))
			.seq
	}

	def findRelevantPoints(department: Department, academicYear: AcademicYear, endDate: LocalDate): Seq[AttendanceMonitoringPoint] =
		session.newCriteria[AttendanceMonitoringPoint]
			.createAlias("scheme", "scheme")
			.add(is("scheme.department", department))
			.add(is("scheme.academicYear", academicYear))
			.add(le("endDate", endDate))
			.seq

	def getAllCheckpoints(relevantPoints: Seq[AttendanceMonitoringPoint]): Map[AttendanceMonitoringPoint, Seq[AttendanceMonitoringCheckpoint]] =
		safeInSeq(() => { session.newCriteria[AttendanceMonitoringCheckpoint] }, "point", relevantPoints)
			.distinct.groupBy(_.point).withDefaultValue(Seq())

	def countCheckpointsForPoints(relevantPoints: Seq[AttendanceMonitoringPoint]): Map[AttendanceMonitoringPoint, Int] =
		safeInSeqWithProjection[AttendanceMonitoringCheckpoint, Array[java.lang.Object]](
			() => { session.newCriteria[AttendanceMonitoringCheckpoint] },
			Projections.projectionList()
				.add(Projections.groupProperty("point"))
				.add(Projections.count("point")),
			"point",
			relevantPoints
		).map { objArray =>
			objArray(0).asInstanceOf[AttendanceMonitoringPoint] -> objArray(1).asInstanceOf[Long].toInt
		}.toMap.withDefaultValue(0)

	def findSchemesLinkedToSITSByDepartment(academicYear: AcademicYear): Map[Department, Seq[AttendanceMonitoringScheme]] = {
		session.newCriteria[AttendanceMonitoringScheme]
			.add(is("academicYear", academicYear))
			.add(isNotNull("memberQuery"))
			.seq
			.groupBy(_.department)
	}

	def setCheckpointTotalsForUpdate(students: Seq[StudentMember], department: Department, academicYear: AcademicYear): Unit = {
		val dbTotals = safeInSeq(() => {
			session.newCriteria[AttendanceMonitoringCheckpointTotal]
				.add(is("department", department))
				.add(is("academicYear", academicYear))
		}, "student", students)
		val totals = students.map(student => dbTotals.find(_.student == student).getOrElse(
			new AttendanceMonitoringCheckpointTotal(student, department, academicYear)
		))
		totals.foreach(total => {
			total.updatedDate = new DateTime(0)
			session.saveOrUpdate(total)
		})
	}

	def listCheckpointTotalsForUpdate: Seq[AttendanceMonitoringCheckpointTotal] = {
		session.newCriteria[AttendanceMonitoringCheckpointTotal]
			.add(lt("updatedDate", DateTime.now.minusHours(6)))
			.addOrder(Order.asc("updatedDate"))
			.setMaxResults(20)
			.setFetchMode("department", FetchMode.JOIN)
			.setFetchMode("student", FetchMode.JOIN)
			.seq
	}
}

case class AttendanceMonitoringStudentData(
	firstName: String,
	lastName: String,
	universityId: String,
	userId: String,
	scdBeginDate: LocalDate,
	scdEndDate: Option[LocalDate],
	routeCode: String,
	routeName: String,
	yearOfStudy: String,
	sprCode: String
) {
	def fullName = s"$firstName $lastName"
}

trait AttendanceMonitoringStudentDataFetcher extends TaskBenchmarking {
	self: SessionComponent =>

	import org.hibernate.criterion.Projections._

	def getAttendanceMonitoringDataForStudents(universityIds: Seq[String], academicYear: AcademicYear): Seq[AttendanceMonitoringStudentData] = {
		def setupProjection(withEndDate: Boolean = false): ProjectionList = {
			val projections =
				Projections.projectionList()
					.add(max("firstName"))
					.add(max("lastName"))
					.add(groupProperty("universityId"))
					.add(max("userId"))
					.add(min("studentCourseDetails.beginDate"))
					.add(max("route.code"))
					.add(max("route.name"))
					.add(max("studentCourseYearDetails.yearOfStudy"))
					.add(max("studentCourseDetails.sprCode"))
			if (withEndDate) {
				projections.add(max("studentCourseDetails.endDate"))
			}
			projections
		}
		def setupCriteria(projection: ProjectionList, withEndDate: Boolean = false) = {
			def criteriaFactory(): ScalaCriteria[StudentMember] = {
				val criteria = session.newCriteria[StudentMember]
					.createAlias("studentCourseDetails", "studentCourseDetails")
					.createAlias("studentCourseDetails.studentCourseYearDetails", "studentCourseYearDetails")
					.createAlias("studentCourseDetails.currentRoute", "route")
					.add(isNull("studentCourseDetails.missingFromImportSince"))
					.add(is("studentCourseYearDetails.academicYear", academicYear))

				if (withEndDate) {
					criteria.add(isNotNull("studentCourseDetails.endDate"))
				} else {
					criteria.add(isNull("studentCourseDetails.endDate"))
				}
			}
			safeInSeqWithProjection[StudentMember, Array[java.lang.Object]](criteriaFactory, projection, "universityId", universityIds)
		}
		// The end date is either null, or if all are not null, the maximum end date, so get the nulls first
		val nullEndDateData = setupCriteria(setupProjection(withEndDate = false)).map {
			case Array(firstName: String, lastName: String, universityId: String, userId: String, scdBeginDate: LocalDate, routeCode: String, routeName: String, yearOfStudy: Integer, sprCode: String) =>
				AttendanceMonitoringStudentData(
					firstName,
					lastName,
					universityId,
					userId,
					scdBeginDate,
					None,
					routeCode,
					routeName,
					yearOfStudy.toString,
					sprCode
				)
		}
		// Then get the not-nulls
		val hasEndDateData = setupCriteria(setupProjection(withEndDate = true), withEndDate = true).map {
			case Array(firstName: String, lastName: String, universityId: String, userId: String, scdBeginDate: LocalDate, routeCode: String, routeName: String, yearOfStudy: Integer, sprCode: String, scdEndDate: LocalDate) =>
				AttendanceMonitoringStudentData(
					firstName,
					lastName,
					universityId,
					userId,
					scdBeginDate,
					Option(scdEndDate),
					routeCode,
					routeName,
					yearOfStudy.toString,
					sprCode
				)
		}
		// Then combine the two, but filter any ended found in the not-ended
		benchmarkTask("Combine data and filter") { nullEndDateData ++ hasEndDateData.filterNot(s => nullEndDateData.exists(_.universityId == s.universityId)) }
	}
}