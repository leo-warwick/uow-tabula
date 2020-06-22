package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Projections._
import org.hibernate.criterion._
import org.hibernate.transform.Transformers
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

import scala.reflect.classTag

trait StudentCourseYearDetailsDao {
  def saveOrUpdate(studentCourseYearDetails: StudentCourseYearDetails): Unit

  def delete(studentCourseYearDetails: StudentCourseYearDetails): Unit

  def getStudentCourseYearDetails(id: String): Option[StudentCourseYearDetails]

  def getBySceKey(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]

  def getBySceKeyStaleOrFresh(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]

  def getFreshIds: Seq[String]

  def getIdsStaleSince(from: DateTime): Seq[String]

  def getFreshKeys: Seq[StudentCourseYearKey]

  def getIdFromKey(key: StudentCourseYearKey): Option[String]

  def convertKeysToIds(keys: Seq[StudentCourseYearKey]): Seq[String]

  def stampMissingFromImport(newStaleScydIds: Seq[String], importStart: DateTime): Unit

  def unstampPresentInImport(notStaleScjCodes: Seq[String]): Unit

  def findByCourseRoutesYear(
    academicYear: AcademicYear,
    courses: Seq[Course],
    courseOccurrences: Seq[String],
    routes: Seq[Route],
    yearOfStudy: Int,
    includeTempWithdrawn: Boolean,
    resitOnly: Boolean,
    eagerLoad: Boolean = false,
    disableFreshFilter: Boolean = false,
    includePermWithdrawn: Boolean = true
  ): Seq[StudentCourseYearDetails]

  def findByCourseRoutesLevel(
    academicYear: AcademicYear,
    courses: Seq[Course],
    courseOccurrences: Seq[String],
    routes: Seq[Route],
    levelCode: String,
    includeTempWithdrawn: Boolean,
    resitOnly: Boolean,
    eagerLoad: Boolean = false,
    disableFreshFilter: Boolean = false,
    includePermWithdrawn: Boolean = true
  ): Seq[StudentCourseYearDetails]

  def findByScjCodeAndAcademicYear(items: Seq[(String, AcademicYear)]): Map[(String, AcademicYear), StudentCourseYearDetails]

  def findByUniversityIdAndAcademicYear(items: Seq[(String, AcademicYear)]): Map[(String, AcademicYear), StudentCourseYearDetails]

  def listForYearMarkExport: Seq[StudentCourseYearDetails]
}

@Repository
class StudentCourseYearDetailsDaoImpl extends StudentCourseYearDetailsDao with Daoisms {

  import Restrictions._

  def saveOrUpdate(studentCourseYearDetails: StudentCourseYearDetails): Unit = {
    session.saveOrUpdate(studentCourseYearDetails)
  }

  def delete(studentCourseYearDetails: StudentCourseYearDetails): Unit = {
    session.delete(studentCourseYearDetails)
    session.flush()
  }

  def getStudentCourseYearDetails(id: String): Option[StudentCourseYearDetails] = getById[StudentCourseYearDetails](id)

  def getBySceKey(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails] =
    session.newCriteria[StudentCourseYearDetails]
      .add(is("studentCourseDetails", studentCourseDetails))
      .add(is("missingFromImportSince", null))
      .add(is("sceSequenceNumber", seq))
      .uniqueResult

  def getBySceKeyStaleOrFresh(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails] = {
    sessionWithoutFreshFilters.newCriteria[StudentCourseYearDetails]
      .add(is("studentCourseDetails", studentCourseDetails))
      .add(is("sceSequenceNumber", seq))
      .uniqueResult
  }

  def getFreshKeys: Seq[StudentCourseYearKey] = {
    session.newCriteria[StudentCourseYearDetails]
      .add(is("missingFromImportSince", null))
      .project[Array[Any]](
      projectionList()
        .add(property("studentCourseDetails.scjCode"), "scjCode")
        .add(property("sceSequenceNumber"), "sceSequenceNumber")
    )
      .setResultTransformer(Transformers.aliasToBean(classOf[StudentCourseYearKey]))
      .seq
  }

  def getFreshIds: Seq[String] = {
    session.newCriteria[StudentCourseYearDetails]
      .add(isNull("missingFromImportSince"))
      .project[String](Projections.property("id"))
      .seq
  }

  def getIdsStaleSince(from: DateTime): Seq[String] = {
    session.newCriteria[StudentCourseYearDetails]
      .add(ge("missingFromImportSince", from))
      .project[String](Projections.property("id"))
      .seq
  }

  // TODO - put these two methods in a service
  def convertKeysToIds(keys: Seq[StudentCourseYearKey]): Seq[String] =
    keys.flatMap {
      key => getIdFromKey(key)
    }

  def getIdFromKey(key: StudentCourseYearKey): Option[String] = {
    sessionWithoutFreshFilters.newCriteria[StudentCourseYearDetails]
      .add(is("studentCourseDetails.scjCode", key.scjCode))
      .add(is("sceSequenceNumber", key.sceSequenceNumber))
      .project[String](Projections.property("id"))
      .uniqueResult
  }

  def stampMissingFromImport(newStaleScydIds: Seq[String], importStart: DateTime): Unit = {
    newStaleScydIds.grouped(Daoisms.MaxInClauseCount).foreach { newStaleIds =>
      val sqlString =
        """ update StudentCourseYearDetails set missingFromImportSince = :importStart
          					|where id in (:newStaleScydIds) """.stripMargin

      session.newUpdateQuery(sqlString)
        .setParameter("importStart", importStart)
        .setParameterList("newStaleScydIds", newStaleIds)
        .executeUpdate()
    }
  }

  def unstampPresentInImport(notStaleScydIds: Seq[String]): Unit = {
    notStaleScydIds.grouped(Daoisms.MaxInClauseCount).foreach { ids =>
      val hqlString =
        """update StudentCourseYearDetails set missingFromImportSince = null
          					|where id in (:ids)""".stripMargin

      sessionWithoutFreshFilters.newUpdateQuery(hqlString)
        .setParameterList("ids", ids)
        .executeUpdate()
    }
  }

  private def sessionDisablingFilters(disableFreshFilter: Boolean) =
    if (disableFreshFilter) {
      sessionWithoutFreshFilters
    } else {
      session
    }

  private def findByCourseRoutesCriteria(
    academicYear: AcademicYear,
    courses: Seq[Course],
    courseOccurrences: Seq[String],
    routes: Seq[Route],
    includeTempWithdrawn: Boolean,
    resitOnly: Boolean,
    disableFreshFilter: Boolean,
    extraCriteria: Seq[Criterion] = Nil
  ): ScalaCriteria[StudentCourseYearDetails] = {
    val c = sessionDisablingFilters(disableFreshFilter).newCriteria[StudentCourseYearDetails]
      .createAlias("studentCourseDetails", "scd")
      .add(isNull("missingFromImportSince"))
      .add(is("academicYear", academicYear))
      .add(is("enrolledOrCompleted", true))
      .add(safeIn("scd.course", courses))

    if (!includeTempWithdrawn) {
      c.add(not(like("scd.statusOnRoute.code", "T%")))
    }

    if (courseOccurrences.nonEmpty) {
      c.add(safeIn("blockOccurrence", courseOccurrences))
    }

    if (routes.nonEmpty) {
      c.add(disjunction(
        safeIn("route", routes),
        conjunction(
          isNull("route"),
          safeIn("scd.currentRoute", routes)
        )
      ))
    }

    if (resitOnly) {
      val resitQuery = DetachedCriteria.forClass(classTag[UpstreamAssessmentGroupMember].runtimeClass, "uagm")
        .createAlias("upstreamAssessmentGroup", "uag")
        .add(is("uag.academicYear", academicYear))
        .add(is("assessmentType", UpstreamAssessmentGroupMemberAssessmentType.Reassessment))
        .add(eqProperty("uagm.universityId", "scd.student.universityId"))
      c.add(Subqueries.exists(resitQuery.setProjection(property("uagm.universityId"))))
    }

    extraCriteria.foreach(c.add)

    c
  }

  private def findByCourseRoutesWithPermWithdrawnOption(
    academicYear: AcademicYear,
    courses: Seq[Course],
    courseOccurrences: Seq[String],
    routes: Seq[Route],
    includeTempWithdrawn: Boolean,
    resitOnly: Boolean,
    eagerLoad: Boolean = false,
    disableFreshFilter: Boolean = false,
    includePermWithdrawn: Boolean = true,
    extraCriteria: Seq[Criterion] = Nil
  ): Seq[StudentCourseYearDetails] = {
    val criteria = findByCourseRoutesCriteria(academicYear, courses, courseOccurrences, routes, includeTempWithdrawn, resitOnly, disableFreshFilter, extraCriteria)

    val result: Seq[StudentCourseYearDetails] =
      if (eagerLoad) {
        // Where we're going, we don't need roads
        type UniversityId = String
        type StudentCourseYearDetailsId = String

        val studentsMap: Map[UniversityId, StudentCourseYearDetailsId] =
          criteria
            .project[Array[Any]](
            projectionList()
              .add(property("scd.student.universityId"), "universityId")
              .add(property("id"), "id")
          )
            .seq
            .map { case Array(universityId: UniversityId, scydId: StudentCourseYearDetailsId) => (universityId, scydId) }
            .toMap

        // Eagerly load the students and relationships for use later
        sessionDisablingFilters(disableFreshFilter).newCriteria[StudentMember]
          .add(safeIn("universityId", studentsMap.keys.toSeq))
          .setFetchMode("studentCourseDetails", FetchMode.JOIN)
          .setFetchMode("studentCourseDetails._moduleRegistrations", FetchMode.JOIN)
          .setFetchMode("studentCourseDetails._moduleRegistrations.module", FetchMode.JOIN)
          .setFetchMode("studentCourseDetails.currentRoute", FetchMode.JOIN)
          .setFetchMode("studentCourseDetails.studentCourseYearDetails", FetchMode.JOIN)
          .setFetchMode("studentCourseDetails.studentCourseYearDetails.route", FetchMode.JOIN)
          .distinct
          .seq
          .map { student =>
            student.freshOrStaleStudentCourseDetails.flatMap(_.freshOrStaleStudentCourseYearDetails)
              .find(_.id == studentsMap(student.universityId))
              .get
          }
      } else {
        criteria.seq
      }

    if (!includePermWithdrawn) {
      result.filterNot(_.studentCourseDetails.permanentlyWithdrawn)
    } else {
      result
    }
  }

  def findByCourseRoutesYear(
    academicYear: AcademicYear,
    courses: Seq[Course],
    courseOccurrences: Seq[String],
    routes: Seq[Route],
    yearOfStudy: Int,
    includeTempWithdrawn: Boolean,
    resitOnly: Boolean,
    eagerLoad: Boolean = false,
    disableFreshFilter: Boolean = false,
    includePermWithdrawn: Boolean = true
  ): Seq[StudentCourseYearDetails] = {
    findByCourseRoutesWithPermWithdrawnOption(academicYear, courses, courseOccurrences, routes, includeTempWithdrawn, resitOnly, eagerLoad, disableFreshFilter, includePermWithdrawn, extraCriteria = Seq(is("yearOfStudy", yearOfStudy)))
  }

  def findByCourseRoutesLevel(
    academicYear: AcademicYear,
    courses: Seq[Course],
    courseOccurrences: Seq[String],
    routes: Seq[Route],
    levelCode: String,
    includeTempWithdrawn: Boolean,
    resitOnly: Boolean,
    eagerLoad: Boolean = false,
    disableFreshFilter: Boolean = false,
    includePermWithdrawn: Boolean = true
  ): Seq[StudentCourseYearDetails] = {
    findByCourseRoutesWithPermWithdrawnOption(academicYear, courses, courseOccurrences, routes, includeTempWithdrawn, resitOnly, eagerLoad, disableFreshFilter, includePermWithdrawn, extraCriteria = Seq(is("studyLevel", levelCode)))
  }

  def findByScjCodeAndAcademicYear(items: Seq[(String, AcademicYear)]): Map[(String, AcademicYear), StudentCourseYearDetails] = {
    // Get all the SCYDs for the given SCJ codes
    val scyds = safeInSeq(() => {
      session.newCriteria[StudentCourseYearDetails]
        .createAlias("studentCourseDetails", "scd")
        .setFetchMode("studentCourseDetails", FetchMode.JOIN)
    }, "scd.scjCode", items.map(_._1))
    // Group by SCJ code-academic year pairs
    scyds.groupBy(scyd => (scyd.studentCourseDetails.scjCode, scyd.academicYear))
      .view
      // Only use the pairs that were passed in
      .filterKeys(items.contains)
      // Sort take the last SCYD; SCYDs are sorted so the most relevant is last
      .mapValues(_.sorted.lastOption)
      .filter(_._2.isDefined).mapValues(_.get)
      .toMap
  }

  def findByUniversityIdAndAcademicYear(items: Seq[(String, AcademicYear)]): Map[(String, AcademicYear), StudentCourseYearDetails] = {
    // Get all the SCYDs for the given Uni IDs
    val scyds = safeInSeq(() => {
      session.newCriteria[StudentCourseYearDetails]
        .createAlias("studentCourseDetails", "scd")
        .createAlias("scd.student", "student")
        .setFetchMode("studentCourseDetails", FetchMode.JOIN)
        .setFetchMode("studentCourseDetails.student", FetchMode.JOIN)
    }, "student.universityId", items.map(_._1))
    // Group by Uni ID-academic year pairs
    scyds.groupBy(scyd => (scyd.studentCourseDetails.student.universityId, scyd.academicYear)).view
      // Only use the pairs that were passed in
      .filterKeys(items.contains)
      // Sort take the last SCYD; SCYDs are sorted so the most relevant is last
      .mapValues(_.sorted.lastOption)
      .filter(_._2.isDefined).mapValues(_.get).toMap
  }

  def listForYearMarkExport: Seq[StudentCourseYearDetails] = {
    session.newCriteria[StudentCourseYearDetails]
      .add(isNull("agreedMarkUploadedDate"))
      .add(isNotNull("agreedMark"))
      .seq
  }
}

trait StudentCourseYearDetailsDaoComponent {
  def studentCourseYearDetailsDao: StudentCourseYearDetailsDao
}

trait AutowiringStudentCourseYearDetailsDaoComponent extends StudentCourseYearDetailsDaoComponent {
  var studentCourseYearDetailsDao: StudentCourseYearDetailsDao = Wire[StudentCourseYearDetailsDao]
}
