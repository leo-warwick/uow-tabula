package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StaffMember
import uk.ac.warwick.tabula.Mockito

class MemberUniversityIdConverterTest extends TestBase with Mockito {

	val converter = new MemberUniversityIdConverter
	var service: ProfileService = mock[ProfileService]
	converter.service = service

	@Test def validInput {
		val member = new StaffMember
		member.universityId = "0672089"

		service.getMemberByUniversityIdStaleOrFresh("0672089") returns (Some(member))

		converter.convertRight("0672089") should be (member)
	}

	@Test def invalidInput {
		service.getMemberByUniversityIdStaleOrFresh("20X6") returns (None)

		converter.convertRight("20X6") should be (null)
	}

	@Test def formatting {
		val member = new StaffMember
		member.universityId = "0672089"

		converter.convertLeft(member) should be ("0672089")
		converter.convertLeft(null) should be (null)
	}

}