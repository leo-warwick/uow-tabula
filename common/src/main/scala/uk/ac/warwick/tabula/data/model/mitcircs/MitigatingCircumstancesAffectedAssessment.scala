package uk.ac.warwick.tabula.data.model.mitcircs

import java.io.Serializable

import javax.persistence._
import org.hibernate.annotations.{Proxy, Type}
import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.commands.mitcircs.submission.AffectedAssessmentItem
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._

object MitigatingCircumstancesAffectedAssessment {
  val EngagementCriteriaModuleCode = "OE"
  val OtherModuleCode = "O"
}

@Entity
@Proxy
@Access(AccessType.FIELD)
class MitigatingCircumstancesAffectedAssessment extends GeneratedId
  with ToString
  with PermissionsTarget
  with Serializable {

  def this(_submission: MitigatingCircumstancesSubmission, item: AffectedAssessmentItem) = {
    this()
    this.mitigatingCircumstancesSubmission = _submission
    this.moduleCode = item.moduleCode
    this.sequence = item.sequence
    this.module = Option(item.module)
    this.academicYear = item.academicYear
    this.name = item.name
    this.assessmentType = item.assessmentType
    this.deadline = item.deadline
    this.boardRecommendations = Option(item.boardRecommendations).map(_.asScala.toSeq).orNull
    this.extensionDeadline = item.extensionDeadline
  }

  /**
    * Uppercase module code, optionally with CATS. e.g. IN304-15 or IN304.
    *
    * If this has the CATS, it can be mapped to an AssessmentComponent, otherwise it
    * has just been provided by the student.
    */
  @Column(nullable = false)
  var moduleCode: String = _

  /**
    * For linking to an AssessmentComponent
    */
  var sequence: String = _

  /**
    * A link to the Tabula representation of a Module.
    */
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "module_id")
  private var _module: Module = _
  def module: Option[Module] = Option(_module)
  def module_=(module: Option[Module]): Unit = _module = module.orNull

  @ManyToOne(fetch = FetchType.EAGER, optional = true)
  @JoinColumns(value = Array(
    new JoinColumn(name = "moduleCode", referencedColumnName="moduleCode", insertable = false, updatable = false),
    new JoinColumn(name = "sequence", referencedColumnName="sequence", insertable = false, updatable = false)
  ))
  private val _assessmentComponent: AssessmentComponent = null
  def assessmentComponent: Option[AssessmentComponent] = Option(_assessmentComponent)

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(nullable = false)
  var academicYear: AcademicYear = _

  @Column(name = "name")
  private var _name: String = _
  // use the name of the assessment component if this matches one - use the locally held name otherwise
  def name: String = assessmentComponent.map(_.name).getOrElse(_name)
  def name_=(n: String): Unit = _name = n

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
  @Column(name = "assessmentType")
  private var _assessmentType: AssessmentType = _
  def assessmentType: AssessmentType = assessmentComponent.map(_.assessmentType).orElse(Option(_assessmentType)).getOrElse(AssessmentType.Other)
  def assessmentType_=(assessmentType: AssessmentType): Unit = _assessmentType = assessmentType

  @Column(nullable = false)
  var deadline: LocalDate = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "submission_id", insertable = false, updatable = false)
  var mitigatingCircumstancesSubmission: MitigatingCircumstancesSubmission = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitCircsExamBoardRecommendationUserType")
  var boardRecommendations: Seq[AssessmentSpecificRecommendation] = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.mitcircs.MitigatingCircumstancesAcuteOutcomeUserType")
  var acuteOutcome: MitigatingCircumstancesAcuteOutcome = _

  @Column(name = "extensionDeadline")
  private var _extensionDeadline: DateTime = _
  def extensionDeadline: Option[DateTime] = Option(_extensionDeadline)
  def extensionDeadline_=(d: DateTime): Unit = _extensionDeadline = d

  def matches(assignment: Assignment): Boolean = {
    module.contains(assignment.module) && assignment.academicYear == academicYear && assignment.assessmentGroups.asScala.exists { ag =>
      ag.assessmentComponent.sequence == sequence
    }
  }

  override def toStringProps: Seq[(String, Any)] = Seq(
    "id" -> id,
    "moduleCode" -> moduleCode,
    "sequence" -> sequence,
    "academicYear" -> academicYear.toString,
    "name" -> name,
    "assessmentType" -> Option(assessmentType).map(_.code).orNull,
    "deadline" -> deadline,
    "mitigatingCircumstancesSubmission" -> mitigatingCircumstancesSubmission.id,
  )

  override def permissionsParents: LazyList[PermissionsTarget] = LazyList(mitigatingCircumstancesSubmission)

}
