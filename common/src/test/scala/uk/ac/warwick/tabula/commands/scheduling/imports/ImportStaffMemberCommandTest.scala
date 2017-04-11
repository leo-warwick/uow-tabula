package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.{DateTimeConstants, LocalDate}
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.tabula.data.model.Gender._
import uk.ac.warwick.tabula.data.model.MemberUserType.Staff
import uk.ac.warwick.tabula.data.model.{Member, StaffMember, StaffProperties}
import uk.ac.warwick.tabula.services.scheduling.{MembershipInformation, MembershipMember}
import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.userlookup.AnonymousUser

// scalastyle:off magic.number
class ImportStaffMemberCommandTest extends TestBase with Mockito {

	trait Environment {
		val mm = MembershipMember(
			universityId 			= "0672089",
			email					= "M.Mannion@warwick.ac.uk",
			title					= "Mr",
			preferredForenames		= "Mathew",
			preferredSurname		= "Mannion",
			dateOfBirth				= new LocalDate(1984, DateTimeConstants.AUGUST, 19),
			usercode				= "cuscav",
			gender					= Male,
			userType				= Staff
		)

		val mac = MembershipInformation(mm)
	}

	// Just a simple test to make sure all the properties that we use BeanWrappers for actually exist, really
	@Test def worksWithNew() {
		new Environment {
			val memberDao: MemberDao = smartMock[MemberDao]
			memberDao.getByUniversityIdStaleOrFresh("0672089") returns None

			val command = new ImportStaffMemberCommand(mac, new AnonymousUser())
			command.memberDao = memberDao

			val member: Member = command.applyInternal()
			member.isInstanceOf[StaffProperties] should be {true}

			member.title should be ("Mr")
			member.universityId should be ("0672089")
			member.userId should be ("cuscav")
			member.email should be ("M.Mannion@warwick.ac.uk")
			member.gender should be (Male)
			member.firstName should be ("Mathew")
			member.lastName should be ("Mannion")
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))
			member.timetableHash should not be null

			verify(memberDao, times(1)).saveOrUpdate(any[Member])
		}
	}

	@Test def worksWithExisting() {
		new Environment {
			val existing = new StaffMember("0672089")
			val existingTimetableHash = "1234"
			existing.timetableHash = existingTimetableHash

			val memberDao: MemberDao = smartMock[MemberDao]
			memberDao.getByUniversityIdStaleOrFresh("0672089") returns Some(existing)

			val command = new ImportStaffMemberCommand(mac, new AnonymousUser())
			command.memberDao = memberDao

			val member: Member = command.applyInternal()
			member.isInstanceOf[StaffProperties] should be {true}

			member.title should be ("Mr")
			member.universityId should be ("0672089")
			member.userId should be ("cuscav")
			member.email should be ("M.Mannion@warwick.ac.uk")
			member.gender should be (Male)
			member.firstName should be ("Mathew")
			member.lastName should be ("Mannion")
			member.dateOfBirth should be (new LocalDate(1984, DateTimeConstants.AUGUST, 19))
			member.timetableHash should be (existingTimetableHash)

			verify(memberDao, times(1)).saveOrUpdate(existing)
		}
	}

}