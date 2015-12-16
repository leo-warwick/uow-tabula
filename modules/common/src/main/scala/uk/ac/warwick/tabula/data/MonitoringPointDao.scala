package uk.ac.warwick.tabula.data

import scala.collection.JavaConverters._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.attendance._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, Route, StudentMember}
import org.hibernate.criterion.{Projections, Order}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.services.TermService
import org.hibernate.FetchMode

trait MonitoringPointDaoComponent {
	val monitoringPointDao: MonitoringPointDao
}

trait AutowiringMonitoringPointDaoComponent extends MonitoringPointDaoComponent {
	val monitoringPointDao = Wire[MonitoringPointDao]
}

trait MonitoringPointDao {
	def getPointById(id: String): Option[MonitoringPoint]
	def getPointTemplateById(id: String): Option[MonitoringPointTemplate]
	def getSetById(id: String): Option[MonitoringPointSet]
	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint], mostSiginificantOnly: Boolean = true): Seq[(StudentMember, MonitoringCheckpoint)]
	def saveOrUpdate(monitoringPoint: MonitoringPoint)
	def delete(monitoringPoint: MonitoringPoint)
	def delete(monitoringPointSet: MonitoringPointSet)
	def saveOrUpdate(monitoringPoint: MonitoringPointTemplate)
	def delete(monitoringPoint: MonitoringPointTemplate)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint)
	def saveOrUpdate(template: MonitoringPointSetTemplate)
	def saveOrUpdate(set: MonitoringPointSet)
	def saveOrUpdate(report: MonitoringPointReport)
	def saveOrUpdate(note: MonitoringPointAttendanceNote)
	def findMonitoringPointSets(route: Route, academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findMonitoringPointSets(route: Route): Seq[MonitoringPointSet]
	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]): Option[MonitoringPointSet]
	def getCheckpoint(monitoringPoint: MonitoringPoint, student: StudentMember) : Option[MonitoringCheckpoint]
	def getCheckpoints(montitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint]
	def listTemplates : Seq[MonitoringPointSetTemplate]
	def getTemplateById(id: String): Option[MonitoringPointSetTemplate]
	def deleteTemplate(template: MonitoringPointSetTemplate)
	def countCheckpointsForPoint(point: MonitoringPoint): Int
	def deleteCheckpoint(checkpoint: MonitoringCheckpoint): Unit
	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet]
	def findPointSetsForStudentsByStudent(students: Seq[StudentMember], academicYear: AcademicYear): Map[StudentMember, MonitoringPointSet]
	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]]
	def studentsByMissedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int,
		startWeek: Int,
		endWeek: Int
	): Seq[(StudentMember, Int)]
	def studentsByUnrecordedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		requiredFromWeek: Int,
		startWeek: Int,
		endWeek: Int,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[(StudentMember, Int)]
	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String]
	def findNonReported(students: Seq[StudentMember], academicYear: AcademicYear, period: String): Seq[StudentMember]
	def findUnreportedReports: Seq[MonitoringPointReport]
	def findReports(students: Seq[StudentMember], year: AcademicYear, period: String): Seq[MonitoringPointReport]
	def hasAnyPointSets(department: Department): Boolean
	def getAttendanceNote(student: StudentMember, monitoringPoint: MonitoringPoint): Option[MonitoringPointAttendanceNote]
	def findAttendanceNotes(students: Seq[StudentMember], points: Seq[MonitoringPoint]): Seq[MonitoringPointAttendanceNote]
}


@Repository
class MonitoringPointDaoImpl extends MonitoringPointDao with Daoisms {

	def getPointById(id: String) =
		session.getById[MonitoringPoint](id)

	def getPointTemplateById(id: String) =
		session.getById[MonitoringPointTemplate](id)

	def getSetById(id: String) =
		session.getById[MonitoringPointSet](id)

	def getCheckpointsByStudent(monitoringPoints: Seq[MonitoringPoint], mostSiginificantOnly: Boolean = true): Seq[(StudentMember, MonitoringCheckpoint)] = {
		if (monitoringPoints.isEmpty) Nil
		else {
			val criteria = session.newCriteria[MonitoringCheckpoint]
				.createAlias("point", "point")
				.add(safeIn("point", monitoringPoints))

			if (mostSiginificantOnly) {
				criteria.setFetchMode("point.pointSet", FetchMode.JOIN)
				criteria.setFetchMode("point.student", FetchMode.JOIN)
				criteria.setFetchMode("point.student.mostSignificantCourseDetails", FetchMode.JOIN)
				criteria.setFetchMode("point.student.mostSignificantCourseDetails.studentCourseYearDetails", FetchMode.JOIN)
			}

			val checkpoints = criteria.seq

			val result = checkpoints
				.map(checkpoint => (checkpoint.student, checkpoint))

			if (mostSiginificantOnly)
				result.filter { case(student, checkpoint) =>
					val pointSet = checkpoint.point.pointSet
					student.mostSignificantCourseDetailsForYear(pointSet.academicYear).exists(scd => {
						pointSet.route == scd.route && scd.freshStudentCourseYearDetails.exists(scyd =>
							scyd.academicYear == pointSet.academicYear && (
								pointSet.year == null || scyd.yearOfStudy == pointSet.year
							)
						)
					})
				}
			else
				result
		}
	}

	def saveOrUpdate(monitoringPoint: MonitoringPoint) = session.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPoint) = session.delete(monitoringPoint)
	def delete(monitoringPointSet: MonitoringPointSet) = session.delete(monitoringPointSet)
	def saveOrUpdate(monitoringPoint: MonitoringPointTemplate) = session.saveOrUpdate(monitoringPoint)
	def delete(monitoringPoint: MonitoringPointTemplate) = session.delete(monitoringPoint)
	def saveOrUpdate(monitoringCheckpoint: MonitoringCheckpoint) = session.saveOrUpdate(monitoringCheckpoint)
	def saveOrUpdate(set: MonitoringPointSet) = session.saveOrUpdate(set)
	def saveOrUpdate(template: MonitoringPointSetTemplate) = session.saveOrUpdate(template)
	def saveOrUpdate(report: MonitoringPointReport) = session.saveOrUpdate(report)
	def saveOrUpdate(note: MonitoringPointAttendanceNote) = session.saveOrUpdate(note)

	def findMonitoringPointSets(route: Route) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.seq

	def findMonitoringPointSets(route: Route, academicYear: AcademicYear) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.add(is("academicYear", academicYear))
			.seq

	def findMonitoringPointSet(route: Route, academicYear: AcademicYear, year: Option[Int]) =
		session.newCriteria[MonitoringPointSet]
			.add(is("route", route))
			.add(is("academicYear", academicYear))
			.add(yearRestriction(year))
			.uniqueResult

	private def yearRestriction(opt: Option[Any]) = opt map { is("year", _) } getOrElse { isNull("year") }

	def getCheckpoint(monitoringPoint: MonitoringPoint, student: StudentMember): Option[MonitoringCheckpoint] = {
		session.newCriteria[MonitoringCheckpoint]
			.add(is("student", student))
			.add(is("point", monitoringPoint))
			.uniqueResult
	}

	def getCheckpoints(monitoringPoint: MonitoringPoint) : Seq[MonitoringCheckpoint] = {
		session.newCriteria[MonitoringCheckpoint]
			.add(is("point", monitoringPoint))
			.seq
	}

	def listTemplates: Seq[MonitoringPointSetTemplate] = {
		session.newCriteria[MonitoringPointSetTemplate]
			.addOrder(Order.asc("position"))
			.list.asScala.toSeq
	}

	def getTemplateById(id: String): Option[MonitoringPointSetTemplate] =
		session.getById[MonitoringPointSetTemplate](id)

	def deleteTemplate(template: MonitoringPointSetTemplate) = session.delete(template)

	def countCheckpointsForPoint(point: MonitoringPoint) =
		session.newCriteria[MonitoringCheckpoint]
			.add(is("point", point))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue()

	def deleteCheckpoint(checkpoint: MonitoringCheckpoint): Unit = {
		session.delete(checkpoint)
	}

	def findPointSetsForStudentsByStudent(students: Seq[StudentMember], academicYear: AcademicYear): Map[StudentMember, MonitoringPointSet] = {
		if (students.isEmpty)
			return Map()

		val partionedUniversityIdsWithIndex = students.map{_.universityId}.grouped(Daoisms.MaxInClauseCount).zipWithIndex.toSeq
		val queryString = """
			select student, mps
			from MonitoringPointSet mps, Route r, StudentCourseDetails scd, StudentCourseYearDetails scyd, StudentMember student
			where r = mps.route
			and scd.route = r.code
			and scyd.studentCourseDetails = scd
			and scd.student = student
			and mps.academicYear = :academicYear
			and scyd.academicYear = mps.academicYear
			and (
			  mps.year = scyd.yearOfStudy
			  or mps.year is null
			) and (
											""" +	partionedUniversityIdsWithIndex.map{
			case (ids, index) => "student.universityId in (:universityIds" + index.toString + ") "
		}.mkString(" or ")	+ ")"

		val query = session.newQuery[Array[java.lang.Object]](queryString)
			.setParameter("academicYear", academicYear)

		partionedUniversityIdsWithIndex.foreach{
			case (ids, index) =>
				query.setParameterList("universityIds" + index.toString, ids)
		}

		query.seq.distinct.map{ objArray =>
			objArray(0).asInstanceOf[StudentMember] -> objArray(1).asInstanceOf[MonitoringPointSet]
		}.toMap
	}

	def findPointSetsForStudents(students: Seq[StudentMember], academicYear: AcademicYear): Seq[MonitoringPointSet] = {
		findPointSetsForStudentsByStudent(students, academicYear).values.toSeq
	}

	def findSimilarPointsForMembers(point: MonitoringPoint, students: Seq[StudentMember]): Map[StudentMember, Seq[MonitoringPoint]] = {
		if (students.isEmpty)
			return Map()

		val partionedUniversityIdsWithIndex = students.map{_.universityId}.grouped(Daoisms.MaxInClauseCount).zipWithIndex.toSeq

		val queryString = """
				select student, mp
				from StudentMember student, StudentCourseDetails scd, StudentCourseYearDetails scyd, Route r, MonitoringPointSet mps, MonitoringPoint mp
				where scd.student = student
				and scd.route = r.code
				and scd = scyd.studentCourseDetails
				and mps.route = r
				and mp.pointSet = mps
				and scyd.academicYear = mps.academicYear
				and (mps.year = scyd.yearOfStudy or mps.year is null)
				and mps.academicYear = :academicYear
				and lower(mp.name) = :name
				and mp.validFromWeek = :validFromWeek
				and mp.requiredFromWeek = :requiredFromWeek
				and (
		""" +	partionedUniversityIdsWithIndex.map{
			case (ids, index) => "student.universityId in (:universityIds" + index.toString + ") "
		}.mkString(" or ")	+ ")"

		val query = session.newQuery[Array[java.lang.Object]](queryString)
			.setParameter("academicYear", point.pointSet.academicYear)
			.setString("name", point.name.toLowerCase)
			.setString("validFromWeek", point.validFromWeek.toString)
			.setString("requiredFromWeek", point.requiredFromWeek.toString)



		partionedUniversityIdsWithIndex.foreach {
			case (ids, index) =>
				query.setParameterList("universityIds" + index.toString, ids)
		}

		val memberPointMap = query.seq.map{objArray =>
			objArray(0).asInstanceOf[StudentMember] -> objArray(1).asInstanceOf[MonitoringPoint]
		}
		memberPointMap.groupBy(_._1).mapValues(_.map(_._2))
	}

	def studentsByMissedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int,
		startWeek: Int = 1,
		endWeek: Int = 52
	): Seq[(StudentMember, Int)] = {
		if (universityIds.isEmpty)
			return Seq()

		val partionedUniversityIdsWithIndex = universityIds.grouped(Daoisms.MaxInClauseCount).zipWithIndex.toSeq

		val queryString = """
			select s.universityId, count(*) as missed from Member s, StudentCourseDetails scd, StudentCourseYearDetails scyd,
			Route r, MonitoringPointSet mps, MonitoringPoint mp, MonitoringCheckpoint mc
			where scd.student = s
			and scyd.studentCourseDetails = scd
			and scd.route = r.code
			and mps.route = r
			and (mps.year is null or mps.year = scyd.yearOfStudy)
			and mp.pointSet = mps
			and mc.point = mp
			and mc.student = s
			and mps.academicYear = :academicYear
			and scyd.academicYear = mps.academicYear
			and mc._state = 'unauthorised'
			and mp.validFromWeek >= :startWeek
			and mp.validFromWeek <= :endWeek
			and (
											""" + partionedUniversityIdsWithIndex.map{
			case (ids, index) => "s.universityId in (:universityIds" + index.toString + ") "
		}.mkString(" or ") + """
			)
			group by s.universityId
		"""

		val query = session.newQuery[Array[java.lang.Object]](queryString)
			.setParameter("academicYear", academicYear)
			.setParameter("startWeek", startWeek)
			.setParameter("endWeek", endWeek)

		partionedUniversityIdsWithIndex.foreach {
			case (ids, index) =>
				query.setParameterList("universityIds" + index.toString, ids)
		}

		studentsByCount(universityIds, academicYear, isAscending, maxResults, startResult, query)
	}

	def studentsByUnrecordedCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		requiredFromWeek: Int = 52,
		startWeek: Int = 1,
		endWeek: Int = 52,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int
	): Seq[(StudentMember, Int)] = {
		if (universityIds.isEmpty)
			return Seq()

		val partionedUniversityIdsWithIndex = universityIds.grouped(Daoisms.MaxInClauseCount).zipWithIndex.toSeq

		val queryString = """
			select s.universityId, count(*) as missed from Member s, StudentCourseDetails scd, StudentCourseYearDetails scyd,
			Route r, MonitoringPointSet mps, MonitoringPoint mp
			where scd.student = s
			and scyd.studentCourseDetails = scd
			and scd.route = r.code
			and mps.route = r
			and (mps.year is null or mps.year = scyd.yearOfStudy)
			and mp.pointSet = mps
			and mps.academicYear = :academicYear
			and scyd.academicYear = mps.academicYear
			and mp.validFromWeek >= :startWeek
			and mp.validFromWeek <= :endWeek
			and mp.requiredFromWeek < :requiredFromWeek
			and mp.id not in (
				select mc.point from MonitoringCheckpoint mc
				where mc.student = s
				and mc.point = mp
			)
			and (
											""" + partionedUniversityIdsWithIndex.map{
			case (ids, index) => "s.universityId in (:universityIds" + index.toString + ") "
		}.mkString(" or ") + """
			)
			group by s.universityId
		"""

		val query = session.newQuery[Array[java.lang.Object]](queryString)
			.setParameter("academicYear", academicYear)
			.setParameter("requiredFromWeek", requiredFromWeek)
			.setParameter("startWeek", startWeek)
			.setParameter("endWeek", endWeek)

		partionedUniversityIdsWithIndex.foreach {
			case (ids, index) =>
				query.setParameterList("universityIds" + index.toString, ids)
		}

		studentsByCount(universityIds, academicYear, isAscending, maxResults, startResult, query)
	}

	private def studentsByCount(
		universityIds: Seq[String],
		academicYear: AcademicYear,
		isAscending: Boolean,
		maxResults: Int,
		startResult: Int,
		query: ScalaQuery[Array[java.lang.Object]]
	) = {
		if (universityIds.isEmpty) Nil
		else {
			val ordering = if (isAscending) Ordering[Int] else Ordering[Int].reverse

			val universityIdCountMap = query.seq.map{objArray =>
				objArray(0).asInstanceOf[String] -> objArray(1).asInstanceOf[Long].toInt
			}.toMap

			val sortedAllUniversityIdCount = universityIds.map{u =>
				u -> universityIdCountMap.getOrElse(u, 0)
			}.sortBy(_._2)(ordering)


			val universityIdsToFetch = sortedAllUniversityIdCount.slice(startResult, startResult + maxResults).map(_._1)

			val students =
				session.newCriteria[StudentMember]
				.add(safeIn("universityId", universityIdsToFetch))
				.seq

			universityIdsToFetch.flatMap{
				u => students.find(_.universityId == u)
			}.map {
				s => s -> sortedAllUniversityIdCount.toMap.getOrElse(s.universityId, 0)
			}
		}
	}

	def findNonReportedTerms(students: Seq[StudentMember], academicYear: AcademicYear): Seq[String] = {
		if (students.isEmpty)
			return Seq()

		val termCounts =
			session.newCriteria[MonitoringPointReport]
			.add(is("academicYear", academicYear))
			.add(safeIn("student.universityId", students.map(_.universityId)))
			.project[Array[java.lang.Object]](
				Projections.projectionList()
					.add(Projections.groupProperty("monitoringPeriod"))
					.add(Projections.count("monitoringPeriod"))
			)
			.seq
			.map { objArray =>
				objArray(0).asInstanceOf[String] -> objArray(1).asInstanceOf[Long].toInt
			}

		TermService.orderedTermNames.diff(termCounts.filter{case(term, count) => count.intValue() == students.size}.map { _._1})

	}

	def findNonReported(students: Seq[StudentMember], academicYear: AcademicYear, period: String): Seq[StudentMember] = {
		if (students.isEmpty)
			return Seq()

		val reportedStudents =
			session.newCriteria[MonitoringPointReport]
			.add(is("academicYear", academicYear))
			.add(is("monitoringPeriod", period))
			.add(safeIn("student.universityId", students.map(_.universityId)))
			.seq
			.map(_.student)

		students.diff(reportedStudents)
	}

	def findUnreportedReports: Seq[MonitoringPointReport] = {
		session.newCriteria[MonitoringPointReport].add(isNull("pushedDate")).seq
	}

	def findReports(students: Seq[StudentMember], academicYear: AcademicYear, period: String): Seq[MonitoringPointReport] = {
		if (students.isEmpty)
			return Seq()

		session.newCriteria[MonitoringPointReport]
			.add(is("academicYear", academicYear))
			.add(is("monitoringPeriod", period))
			.add(safeIn("student.universityId", students.map(_.universityId)))
			.seq
	}

	def hasAnyPointSets(department: Department): Boolean = {
		session.newCriteria[MonitoringPointSet]
			.createAlias("route", "r")
			.add(is("r.adminDepartment", department))
			.project[Number](Projections.rowCount())
			.uniqueResult.get.intValue() > 0
	}

	def getAttendanceNote(student: StudentMember, monitoringPoint: MonitoringPoint): Option[MonitoringPointAttendanceNote] = {
		session.newCriteria[MonitoringPointAttendanceNote]
			.add(is("student", student))
			.add(is("point", monitoringPoint))
			.uniqueResult
	}

	def findAttendanceNotes(students: Seq[StudentMember], points: Seq[MonitoringPoint]): Seq[MonitoringPointAttendanceNote] = {
		if(students.isEmpty || points.isEmpty)
			return Seq()

		session.newCriteria[MonitoringPointAttendanceNote]
			.add(safeIn("student", students))
			.add(safeIn("point", points))
			.seq
	}

}