package uk.ac.warwick.tabula.data.model

import javax.persistence._

import org.apache.commons.lang3.builder.EqualsBuilder
import org.hibernate.annotations.{BatchSize, Type}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._


/**
 * An assessment group is basically the smallest group of people
 * who would be submitting to the same assignment. It is identified
 * by four pieces of information:
 *
 * - The module it's in
 * - The academic year
 * - The assessment group code (1 or more assignments in the module will share this code)
 * - The occurrence (Splits a module up into multiple occurrences/cohorts per year - 99% of the time
 *          there's only one value but sometimes can be 2 or even 26 in one case)
 */
@Entity
class UpstreamAssessmentGroup extends GeneratedId {

	// Long-form module code with hyphen and CATS value
	var moduleCode: String = _
	var assessmentGroup: String = _
	var occurrence: String = _
	var sequence: String = _

	@Basic @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	@Column(nullable = false)
	var academicYear: AcademicYear = _

	@OneToMany(mappedBy = "upstreamAssessmentGroup", fetch = FetchType.LAZY, cascade = Array(CascadeType.ALL), orphanRemoval = true)
	@BatchSize(size = 200)
	var members: JList[UpstreamAssessmentGroupMember] = JArrayList()

	def membersIncludes(user: User): Boolean = members.asScala.map(_.universityId).contains(user.getWarwickId)

	def membersIncludes(universityId: String): Boolean = members.asScala.map(_.universityId).contains(universityId)

	def replaceMembers(universityIds: Seq[String]): Unit = {
		members.clear()
		members.addAll(universityIds.distinct.map(universityId => new UpstreamAssessmentGroupMember(this, universityId)).asJava)
	}

	def isEquivalentTo(other: UpstreamAssessmentGroup): Boolean =
		new EqualsBuilder()
			.append(moduleCode, other.moduleCode)
			.append(assessmentGroup, other.assessmentGroup)
			.append(occurrence, other.occurrence)
			.append(academicYear, other.academicYear)
			.append(sequence, other.sequence)
			.isEquals

	override def toString: String = "%s %s g:%s o:%s s:%s" format (moduleCode, academicYear, assessmentGroup, occurrence, sequence)

}

@Entity
class UpstreamAssessmentGroupMember extends GeneratedId with Ordered[UpstreamAssessmentGroupMember]
	with UpstreamAssessmentGroupMemberProperties {

	def this(upstreamAssessmentGroup: UpstreamAssessmentGroup, universityId: String) = {
		this()
		this.upstreamAssessmentGroup = upstreamAssessmentGroup
		this.universityId = universityId
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_id")
	var upstreamAssessmentGroup: UpstreamAssessmentGroup = _

	@Column(name="universityId")
	var universityId: String = _

	override def compare(that: UpstreamAssessmentGroupMember): Int =
		position.getOrElse(Int.MaxValue) - that.position.getOrElse(Int.MaxValue) match {
			case 0 => Ordering.String.compare(this.universityId, that.universityId)
			case nonZero => nonZero
		}
}

trait UpstreamAssessmentGroupMemberProperties {

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionIntegerUserType")
	var position: Option[Int] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBigDecimalUserType")
	var actualMark: Option[BigDecimal] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var actualGrade: Option[String] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionBigDecimalUserType")
	var agreedMark: Option[BigDecimal] = None

	@Type(`type` = "uk.ac.warwick.tabula.data.model.OptionStringUserType")
	var agreedGrade: Option[String] = None

}