package uk.ac.warwick.tabula.data.model

import enumeratum.{Enum, EnumEntry}
import javax.persistence._
import org.apache.commons.lang3.builder.EqualsBuilder
import org.hibernate.annotations.{BatchSize, Proxy, Type}
import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User

import scala.jdk.CollectionConverters._

/**
  * An assessment group is basically the smallest group of people
  * who would be submitting to the same assignment. It is identified
  * by four pieces of information:
  *
  * - The module it's in
  * - The academic year
  * - The assessment group code (1 or more assignments in the module will share this code)
  * - The occurrence (Splits a module up into multiple occurrences/cohorts per year - 99% of the time
  * there's only one value but sometimes can be 2 or even 26 in one case)
  */
@Entity
@Proxy
class UpstreamAssessmentGroup extends GeneratedId with Serializable {

  // Long-form module code with hyphen and CATS value
  var moduleCode: String = _
  var assessmentGroup: String = _
  var occurrence: String = _
  var sequence: String = _

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = _

  @ManyToOne(fetch = FetchType.EAGER, optional = true)
  @JoinColumns(value = Array(
    new JoinColumn(name = "moduleCode", referencedColumnName="moduleCode", insertable = false, updatable = false),
    new JoinColumn(name = "sequence", referencedColumnName="sequence", insertable = false, updatable = false)
  ))
  private var _assessmentComponent: AssessmentComponent = _
  def assessmentComponent: Option[AssessmentComponent] = Option(_assessmentComponent)
  def assessmentComponent_=(assessmentComponent: AssessmentComponent): Unit = _assessmentComponent = assessmentComponent

  @Column(name = "deadline")
  private var _deadline: LocalDate = _
  def deadline: Option[LocalDate] = Option(_deadline)
  def deadline_=(deadline: Option[LocalDate]): Unit = _deadline = deadline.orNull

  @OneToMany(mappedBy = "upstreamAssessmentGroup", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
  @BatchSize(size = 200)
  var members: JList[UpstreamAssessmentGroupMember] = JArrayList()

  def membersIncludes(user: User): Boolean = members.asScala.map(_.universityId).contains(user.getWarwickId)

  def membersIncludes(universityId: String): Boolean = members.asScala.map(_.universityId).contains(universityId)

  def replaceMembers(universityIds: Seq[(String, Option[String])], assessmentType: UpstreamAssessmentGroupMemberAssessmentType): Unit = {
    members.removeAll(members.asScala.filter(_.assessmentType == assessmentType).asJava)
    members.addAll(universityIds.distinct.map { case (universityId, resitSequence) =>
      new UpstreamAssessmentGroupMember(this, universityId, assessmentType, resitSequence)
    }.asJava)
  }

  def isEquivalentTo(other: UpstreamAssessmentGroup): Boolean =
    new EqualsBuilder()
      .append(moduleCode, other.moduleCode)
      .append(assessmentGroup, other.assessmentGroup)
      .append(occurrence, other.occurrence)
      .append(academicYear, other.academicYear)
      .append(sequence, other.sequence)
      // This intentionally doesn't take into account deadline
      .isEquals

  override def toString: String = "%s %s g:%s o:%s s:%s d:%s".format(moduleCode, academicYear, assessmentGroup, occurrence, sequence, deadline.map(_.toString("yyyy-MM-dd")).orNull)

}

@Entity
@Proxy
class UpstreamAssessmentGroupMember extends GeneratedId with Ordered[UpstreamAssessmentGroupMember]
  with UpstreamAssessmentGroupMemberProperties {

  def this(upstreamAssessmentGroup: UpstreamAssessmentGroup, universityId: String, assessmentType: UpstreamAssessmentGroupMemberAssessmentType, resitSequence: Option[String] = None) = {
    this()
    require(assessmentType == UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment == resitSequence.isEmpty, s"Resit sequence must be empty for original assessments and non-empty for reassessment but was $resitSequence for $assessmentType")
    this.upstreamAssessmentGroup = upstreamAssessmentGroup
    this.universityId = universityId
    this.assessmentType = assessmentType
    this.resitSequence = resitSequence
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  var upstreamAssessmentGroup: UpstreamAssessmentGroup = _

  @Column(name = "universityId")
  var universityId: String = _

  override def compare(that: UpstreamAssessmentGroupMember): Int =
    position.getOrElse(Int.MaxValue) - that.position.getOrElse(Int.MaxValue) match {
      case 0 => Ordering.String.compare(this.universityId, that.universityId)
      case nonZero => nonZero
    }

  // FIXME - just inherits from the components deadline - leaving this here in case we need to do something fancy to support resits
  def deadline: Option[LocalDate] = upstreamAssessmentGroup.deadline

  def isReassessment: Boolean = assessmentType == UpstreamAssessmentGroupMemberAssessmentType.Reassessment

  def isAgreedMark: Boolean = agreedMark.isDefined

  def firstDefinedMark: Option[Int] = agreedMark.orElse(actualMark)

  def isAgreedGrade: Boolean = agreedGrade.isDefined

  def firstDefinedGrade: Option[String] = agreedGrade.orElse(actualGrade)

  def firstOriginalMark: Option[Int] =
    upstreamAssessmentGroup.members.asScala
      .find(uagm => uagm.universityId == universityId && !uagm.isReassessment)
      .flatMap(_.firstDefinedMark)

  def firstOriginalGrade: Option[String] =
    upstreamAssessmentGroup.members.asScala
      .find(uagm => uagm.universityId == universityId && !uagm.isReassessment)
      .flatMap(_.firstDefinedGrade)
}

trait UpstreamAssessmentGroupMemberProperties {
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var position: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var actualMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var actualGrade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var agreedMark: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  var agreedGrade: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroupMemberAssessmentTypeUserType")
  @Column(name = "assessment_type", nullable = false)
  var assessmentType: UpstreamAssessmentGroupMemberAssessmentType = _

  // These are only set for assessmentType == Reassessment
  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  var currentResitAttempt: Option[Int] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  @Column(name = "resit_sequence")
  var resitSequence: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
  @Column(name = "resit_assessment_name")
  var resitAssessmentName: Option[String] = None

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
  @Column(name = "resit_assessment_type")
  private var _resitAssessmentType: AssessmentType = _
  def resitAssessmentType: Option[AssessmentType] = Option(_resitAssessmentType)
  def resitAssessmentType_=(resitAssessmentType: Option[AssessmentType]): Unit = _resitAssessmentType = resitAssessmentType.orNull

  @Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
  @Column(name = "resit_assessment_weighting")
  var resitAssessmentWeighting: Option[Int] = None
}

/** currentMembers are all members excluding PWD)  **/
case class UpstreamAssessmentGroupInfo(upstreamAssessmentGroup: UpstreamAssessmentGroup, currentMembers: Seq[UpstreamAssessmentGroupMember]) {
  lazy val allMembers: Seq[UpstreamAssessmentGroupMember] = upstreamAssessmentGroup.members.asScala.toSeq
  lazy val resitMembers: Seq[UpstreamAssessmentGroupMember] = currentMembers.filter(_.assessmentType == UpstreamAssessmentGroupMemberAssessmentType.Reassessment)
}

sealed abstract class UpstreamAssessmentGroupMemberAssessmentType(val sitsProcess: String) extends EnumEntry
object UpstreamAssessmentGroupMemberAssessmentType extends Enum[UpstreamAssessmentGroupMemberAssessmentType] {
  case object OriginalAssessment extends UpstreamAssessmentGroupMemberAssessmentType("SAS")

  // This covers both a further first sit and a resit, see currentResitAttempt
  case object Reassessment extends UpstreamAssessmentGroupMemberAssessmentType("RAS")

  override def values: IndexedSeq[UpstreamAssessmentGroupMemberAssessmentType] = findValues
}

class UpstreamAssessmentGroupMemberAssessmentTypeUserType extends EnumUserType(UpstreamAssessmentGroupMemberAssessmentType)
