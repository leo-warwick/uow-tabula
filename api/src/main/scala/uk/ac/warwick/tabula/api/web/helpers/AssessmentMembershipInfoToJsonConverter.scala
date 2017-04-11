package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.services.{ExcludeType, AssessmentMembershipInfo}

trait AssessmentMembershipInfoToJsonConverter {

	def jsonAssessmentMembershipInfoObject(membershipInfo: AssessmentMembershipInfo, upstreamAssessmentGroups: Seq[UpstreamAssessmentGroup]) = Map(
		"studentMembership" -> Map(
			"total" -> membershipInfo.totalCount,
			"linkedSits" -> membershipInfo.sitsCount,
			"included" -> membershipInfo.usedIncludeCount,
			"excluded" -> membershipInfo.usedExcludeCount,
			"users" -> membershipInfo.items.filterNot(_.itemType == ExcludeType).map { item =>
				Seq(
					item.userId.map { "userId" -> _ },
					item.universityId.map { "universityId" -> _ }
				).flatten.toMap
			}
		),
		"sitsLinks" -> upstreamAssessmentGroups.map { uag => Map(
			"moduleCode" -> uag.moduleCode,
			"assessmentGroup" -> uag.assessmentGroup,
			"occurrence" -> uag.occurrence,
			"sequence" -> uag.sequence
		)}
	)

}
