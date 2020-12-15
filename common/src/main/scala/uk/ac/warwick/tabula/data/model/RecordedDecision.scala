package uk.ac.warwick.tabula.data.model

import javax.persistence.{Access, AccessType, Column, Entity}
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.healthchecks.WriteToSits
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.userlookup.User

@Entity
@Proxy
@Access(AccessType.FIELD)
class RecordedDecision extends GeneratedId
  with HibernateVersioned
  with ToEntityReference
  with WriteToSits
  with ToString with Serializable {

  override type Entity = RecordedDecision

  @Column(name = "spr_code", nullable = false)
  var sprCode: String = _

  @Column(nullable = false)
  var sequence: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academic_year", nullable = false)
  var academicYear: AcademicYear = _

  @Column(name = "resit_period", nullable = false)
  var resitPeriod: Boolean = _

  @Column(nullable = false)
  @Type(`type` = "uk.ac.warwick.tabula.data.model.ActualProgressionDecisionUserType")
  var decision: ActualProgressionDecision = _

  var notes: String = _

  var minutes: String = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.SSOUserType")
  @Column(name = "updated_by", nullable = false)
  var updatedBy: User = _

  @Column(name = "updated_date", nullable = false)
  var updatedDate: DateTime = _

  @Column(name = "needs_writing_to_sits_since")
  private var _needsWritingToSitsSince: DateTime = _
  def needsWritingToSitsSince: Option[DateTime] = Option(_needsWritingToSitsSince)
  def needsWritingToSitsSince_=(needsWritingToSitsSince: Option[DateTime]): Unit = _needsWritingToSitsSince = needsWritingToSitsSince.orNull
  def needsWritingToSits: Boolean = needsWritingToSitsSince.nonEmpty

  // empty for any student that's never been written
  @Column(name = "last_written_to_sits")
  private var _lastWrittenToSits: DateTime = _
  def lastWrittenToSits: Option[DateTime] = Option(_lastWrittenToSits)
  def lastWrittenToSits_=(lastWrittenToSits: Option[DateTime]): Unit = _lastWrittenToSits = lastWrittenToSits.orNull

  def markWrittenToSits(): Unit = {
    needsWritingToSitsSince = None
    lastWrittenToSits = Some(DateTime.now)
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "sprCode" -> sprCode,
    "decision" -> decision.pitCode,
  )
}
