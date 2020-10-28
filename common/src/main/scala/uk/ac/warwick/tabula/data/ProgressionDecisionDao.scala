package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order._
import org.hibernate.criterion.Restrictions._
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.model.{ProgressionDecision, ProgressionDecisionProcessStatus}

trait ProgressionDecisionDao {
  def saveOrUpdate(pd: ProgressionDecision): Unit
  def delete(pd: ProgressionDecision): Unit
  def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision]
  def getAgreedByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision]
  def getAgreedByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision]
  def getByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision]
}

abstract class HibernateProgressionDecisionDao extends ProgressionDecisionDao with HelperRestrictions {
  self: ExtendedSessionComponent =>

  override def saveOrUpdate(pd: ProgressionDecision): Unit = session.saveOrUpdate(pd)

  override def delete(pd: ProgressionDecision): Unit = session.delete(pd)

  private def byAcademicYearsCriteria: ScalaCriteria[ProgressionDecision] = session.newCriteria[ProgressionDecision]
    .addOrder(asc("sprCode"))
    .addOrder(asc("sequence"))

  override def getByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision] =
    safeInSeq(() => {
      byAcademicYearsCriteria
    }, "academicYear", academicYears)

  override def getAgreedByAcademicYears(academicYears: Seq[AcademicYear]): Seq[ProgressionDecision] =
    safeInSeq(() => {
      byAcademicYearsCriteria.add(is("status", ProgressionDecisionProcessStatus.Complete))
    }, "academicYear", academicYears)

  private def byUniversityIdCriteria = session.newCriteria[ProgressionDecision]
    .createAlias("_allStudentCourseDetails", "studentCourseDetails")
    .add(isNull("studentCourseDetails.missingFromImportSince"))
    .addOrder(asc("sprCode"))
    .addOrder(asc("sequence"))

  override def getAgreedByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision] =
    safeInSeq(() => {
      byUniversityIdCriteria.add(is("status", ProgressionDecisionProcessStatus.Complete))
    }, "studentCourseDetails.student.universityId", universityIds).distinct

  override def getByUniversityIds(universityIds: Seq[String]): Seq[ProgressionDecision] =
    safeInSeq(() => {
      byUniversityIdCriteria
    }, "studentCourseDetails.student.universityId", universityIds).distinct
}

@Repository("progressionDecisionDao")
class AutowiringProgressionDecisionDao
  extends HibernateProgressionDecisionDao
    with Daoisms

trait ProgressionDecisionDaoComponent {
  def progressionDecisionDao: ProgressionDecisionDao
}

trait AutowiringProgressionDecisionDaoComponent extends ProgressionDecisionDaoComponent {
  var progressionDecisionDao: ProgressionDecisionDao = Wire[ProgressionDecisionDao]
}
