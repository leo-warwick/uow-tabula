package uk.ac.warwick.tabula.data.model

import javax.persistence._
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import uk.ac.warwick.tabula.services.AssessmentMembershipService

import scala.jdk.CollectionConverters._

/**
  * Represents an upstream assessment component as found in the central
  * University systems. An component is timeless - it doesn't
  * relate to a specific instance of an assignment/exam or even a particular year.
  *
  * This used to be UpstreamAssignment when we were only importing assignment-type
  * components. Now we include other things like exams, so it has been renamed
  * AssessmentComponent in line with what it's called in SITS.
  */
@Entity
@Proxy
@Table(name = "UPSTREAMASSIGNMENT")
class AssessmentComponent extends GeneratedId with PreSaveBehaviour with Serializable {

  @transient var membershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]

  /**
    * Uppercase module code, with CATS. e.g. IN304-15
    */
  var moduleCode: String = _

  /**
    * An OPTIONAL link to a Tabula representation of a Module.
    */
  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "module_id")
  var module: Module = _

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "assessmentComponent")
  var links: JList[AssessmentGroup] = JArrayList()

  /**
    * Assessment group the assignment is in. Is mostly a meaningless
    * character but will map to a corresponding student module registratation
    * to the same group.
    */
  var assessmentGroup: String = _

  /**
    * Identifier for the assignment, unique within a given moduleCode.
    */
  var sequence: String = _

  /**
    * Name as defined upstream.
    */
  var name: String = _

  @Column(name = "IN_USE")
  var inUse: Boolean = _

  /**
    * The type of component. Typical values are A for assignment,
    * E for summer exam. Other values exist.
    */
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AssessmentTypeUserType")
  @Column(nullable = false)
  var assessmentType: AssessmentType = _

  var marksCode: String = _

  var weighting: JInteger = _

  /**
    * Returns moduleCode without CATS. e.g. in304
    */
  def moduleCodeBasic: String = Module.stripCats(moduleCode).getOrElse(throw new IllegalArgumentException(s"$moduleCode did not fit expected module code pattern"))

  /**
    * Returns the CATS as a string if it's present, e.g. 50
    */
  def cats: Option[String] = Module.extractCats(moduleCode)

  def sameKey(other: AssessmentComponent): Boolean = {
    this.sequence == other.sequence && this.moduleCode == other.moduleCode
  }

  def needsUpdatingFrom(other: AssessmentComponent): Boolean =
    this.name != other.name ||
      this.module != other.module ||
      this.assessmentGroup != other.assessmentGroup ||
      this.assessmentType != other.assessmentType ||
      this.inUse != other.inUse ||
      this.marksCode != other.marksCode ||
      this.weighting != other.weighting

  override def preSave(newRecord: Boolean) {
    ensureNotNull("name", name)
    ensureNotNull("moduleCode", moduleCode)
  }

  private def ensureNotNull(name: String, value: Any) {
    if (value == null) throw new IllegalStateException("null " + name + " not allowed")
  }

  def copyFrom(other: AssessmentComponent) {
    moduleCode = other.moduleCode
    assessmentGroup = other.assessmentGroup
    sequence = other.sequence
    inUse = other.inUse
    module = other.module
    name = other.name
    assessmentType = other.assessmentType
    marksCode = other.marksCode
    weighting = other.weighting
  }

  def upstreamAssessmentGroups(year: AcademicYear): Seq[UpstreamAssessmentGroup] = membershipService.getUpstreamAssessmentGroups(this, year)

  def linkedAssignments: Seq[Assignment] = links.asScala.toSeq.collect { case l if l.assignment != null => l.assignment }
}

object AssessmentComponent {
  /** The value we store as the assessment group when no group has been chosen for this student
    * (in ADS it is null)
    *
    * We also use this value for a few other fields, like Occurrence, so the variable
    * name is a bit too specific.
    */
  val NoneAssessmentGroup = "NONE"
}
