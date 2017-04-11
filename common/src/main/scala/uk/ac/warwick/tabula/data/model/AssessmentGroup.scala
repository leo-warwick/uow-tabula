package uk.ac.warwick.tabula.data.model

import javax.persistence._

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, ToString}
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.services.AssessmentMembershipService

/**
 * This entity is basically a many-to-many mapper between
 * assignment/exam/smallgroupset and assessmentcomponent, so
 * that they can link to multiple assessmentcomponents.
 *
 * It is not directly related to UpstreamAssessmentGroup
 * as the name might suggest - it is a confusing name.
 */
@Entity
class AssessmentGroup extends GeneratedId {

	@transient var membershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]

	/*
	Either assignment, smallGroupSet _or_ exam will be non-null
	depending on which type of entity we're linking an
	AssessmentComponent to...
	 */

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignment_id")
	var assignment: Assignment = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_set_id")
	var smallGroupSet: SmallGroupSet = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "exam_id")
	var exam: Exam = _

	def parent: Option[GeneratedId] =
		Seq(Option(assignment), Option(smallGroupSet), Option(exam)).flatten.headOption

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "upstream_id")
	var assessmentComponent: AssessmentComponent = _

	var occurrence: String = _

	def toUpstreamAssessmentGroup(academicYear: AcademicYear): Option[UpstreamAssessmentGroup] = {
		if (academicYear == null || assessmentComponent == null || occurrence == null) {
			None
		} else {
			val template = new UpstreamAssessmentGroup
			template.academicYear = academicYear
			template.assessmentGroup = assessmentComponent.assessmentGroup
			template.moduleCode = assessmentComponent.moduleCode
			template.sequence = assessmentComponent.sequence
			template.occurrence = occurrence
			membershipService.getUpstreamAssessmentGroup(template)
		}
	}

	override def toString: String = {
		if (parent.isDefined && assessmentComponent != null && occurrence != null) {

			val entityInfo =
				if (assignment != null) Seq("assignment" -> assignment.id)
				else if (smallGroupSet != null) Seq("smallGroupSet" -> smallGroupSet.id)
				else Seq("exam" -> exam.id)

			val props = entityInfo ++ Seq(
				"assessmentComponent" -> assessmentComponent.id,
				"occurrence" -> occurrence,
				"sequence" -> assessmentComponent.sequence
			)

			ToString.forObject(this, props : _*)

		} else {
			"AssessmentGroup(invalid, contains nulls)"
		}
	}
}