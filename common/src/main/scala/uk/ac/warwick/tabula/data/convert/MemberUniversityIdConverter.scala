package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.system.TwoWayConverter

class MemberUniversityIdConverter extends TwoWayConverter[String, Member] {

	var service: ProfileService = Wire.auto[ProfileService]

	override def convertRight(universityId: String): Member = {
		if ("me" == universityId) {
			RequestInfo.fromThread.flatMap { info =>
				service.getMemberByUniversityIdStaleOrFresh(info.user.universityId)
			}.orNull
		} else {
			service.getMemberByUniversityIdStaleOrFresh(universityId).orNull
		}
	}
	override def convertLeft(member: Member): String = (Option(member) map {_.universityId}).orNull

}