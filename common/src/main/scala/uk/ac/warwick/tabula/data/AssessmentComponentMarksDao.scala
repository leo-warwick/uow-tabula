package uk.ac.warwick.tabula.data

import org.hibernate.{FetchMode, Session}
import org.hibernate.criterion.Projections._
import org.hibernate.criterion.Restrictions._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.Daoisms._
import uk.ac.warwick.tabula.data.model.{RecordedAssessmentComponentStudent, UpstreamAssessmentGroup, UpstreamAssessmentGroupMember}

trait AssessmentComponentMarksDao {
  def getRecordedStudent(uagm: UpstreamAssessmentGroupMember): Option[RecordedAssessmentComponentStudent]
  def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent]
  def getAllForModulesInYear(moduleCodes: Seq[String], academicYear: AcademicYear): Seq[RecordedAssessmentComponentStudent]
  def allNeedingWritingToSits: Seq[RecordedAssessmentComponentStudent]
  def mostRecentlyWrittenStudentDate: Option[DateTime]
  def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent
}

abstract class AbstractAssessmentComponentMarksDao extends AssessmentComponentMarksDao {
  self: ExtendedSessionComponent
    with HelperRestrictions =>

  override def getRecordedStudent(uagm: UpstreamAssessmentGroupMember): Option[RecordedAssessmentComponentStudent] =
    session.newCriteria[RecordedAssessmentComponentStudent]
      .add(is("moduleCode", uagm.upstreamAssessmentGroup.moduleCode))
      .add(is("assessmentGroup", uagm.upstreamAssessmentGroup.assessmentGroup))
      .add(is("occurrence", uagm.upstreamAssessmentGroup.occurrence))
      .add(is("sequence", uagm.upstreamAssessmentGroup.sequence))
      .add(is("academicYear", uagm.upstreamAssessmentGroup.academicYear))
      .add(is("universityId", uagm.universityId))
      .add(is("assessmentType", uagm.assessmentType))
      .add(if (uagm.resitSequence.isEmpty) isNull("resitSequence") else is("resitSequence", uagm.resitSequence))
      .uniqueResult

  override def getAllRecordedStudents(uag: UpstreamAssessmentGroup): Seq[RecordedAssessmentComponentStudent] =
    session.newCriteria[RecordedAssessmentComponentStudent]
      .setFetchMode("_marks", FetchMode.JOIN)
      .add(is("moduleCode", uag.moduleCode))
      .add(is("assessmentGroup", uag.assessmentGroup))
      .add(is("occurrence", uag.occurrence))
      .add(is("sequence", uag.sequence))
      .add(is("academicYear", uag.academicYear))
      .distinct
      .seq

  override def getAllForModulesInYear(moduleCodes: Seq[String], academicYear: AcademicYear): Seq[RecordedAssessmentComponentStudent] =
    safeInSeq(
      () =>
        session.newCriteria[RecordedAssessmentComponentStudent]
          .setFetchMode("_marks", FetchMode.JOIN)
          .add(is("academicYear", academicYear)),
      "moduleCode",
      moduleCodes
    )

  override def allNeedingWritingToSits: Seq[RecordedAssessmentComponentStudent] =
    session.newCriteria[RecordedAssessmentComponentStudent]
      .add(isNotNull("_needsWritingToSitsSince"))
      .distinct
      .seq

  override def mostRecentlyWrittenStudentDate: Option[DateTime] =
    session.newCriteria[RecordedAssessmentComponentStudent]
      .add(isNotNull("_lastWrittenToSits"))
      .project[DateTime](max("_lastWrittenToSits"))
      .uniqueResult

  override def saveOrUpdate(student: RecordedAssessmentComponentStudent): RecordedAssessmentComponentStudent = {
    session.saveOrUpdate(student)
    student
  }
}

@Repository
class AutowiringAssessmentComponentMarksDao
  extends AbstractAssessmentComponentMarksDao
    with Daoisms

trait AssessmentComponentMarksDaoComponent {
  def assessmentComponentMarksDao: AssessmentComponentMarksDao
}

trait AutowiringAssessmentComponentMarksDaoComponent extends AssessmentComponentMarksDaoComponent {
  var assessmentComponentMarksDao: AssessmentComponentMarksDao = Wire[AssessmentComponentMarksDao]
}
