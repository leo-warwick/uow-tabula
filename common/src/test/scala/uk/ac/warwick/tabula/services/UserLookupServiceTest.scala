package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.{MockCachingLookupService, TestBase, Mockito}
import uk.ac.warwick.tabula.UserFlavour._

class UserLookupServiceTest extends TestBase with Mockito {

	@Test def swappingUserLookup() {
		val lookup1 = mock[UserLookupService]
		val lookup2 = mock[UserLookupService]

		val swappable = new SwappableUserLookupService(lookup1)
		swappable.getUserByUserId("a")
		swappable.delegate = lookup2
		swappable.getUserByUserId("b")

		verify(lookup1, times(1)).getUserByUserId("a")
		verify(lookup2, times(1)).getUserByUserId("b")
	}

	/*
	 * we're testing the UserByWarwickIdCache trait here,
	 * which is implemented by both the UserLookupService and the MockCachingLookupService
	 */
	@Test def caching() {
		val muls = new MockCachingLookupService
		val cache = muls.UserByWarwickIdCache
		var warwickId = ""
		cache.clear()

		// an unverified user should not be cached
		muls.flavour = Unverified
		warwickId = "1234567"
		val unverifiedUser = muls.getUserByWarwickUniId(warwickId)
		unverifiedUser.isVerified should be {false}
		unverifiedUser.isFoundUser should be {false}
		cache.getStatistics.getCacheSize should be (0)

		// a good user should be cached
		muls.flavour = Vanilla
		warwickId = "0123456"
		val existingUser = muls.getUserByWarwickUniId(warwickId)
		existingUser.isVerified should be {true}
		existingUser.isFoundUser should be {true}
		cache.getStatistics.getCacheSize should be (1)
		cache.contains(warwickId) should be {true}

		// an applicant user should NOT be cached (TAB-1734)
		muls.flavour = Applicant
		warwickId = "1819201"
		val applicantUser = muls.getUserByWarwickUniId(warwickId)
		applicantUser.isVerified should be {true}
		applicantUser.isFoundUser should be {false}
		cache.getStatistics.getCacheSize should be (1)
		cache.contains(warwickId) should be {false}

		// an anonymous user should NOT be cached (TAB-1734)
		muls.flavour = Anonymous
		warwickId = "0987654"
		val anonUser = muls.getUserByWarwickUniId(warwickId)
		anonUser.isVerified should be {true}
		anonUser.isFoundUser should be {false}
		cache.getStatistics.getCacheSize should be (1)
		cache.contains(warwickId) should be {false}
	}

	@Test def cachingMultiLookups() {
		val muls = new MockCachingLookupService
		val cache = muls.UserByWarwickIdCache
		var warwickIds = Seq("")
		cache.clear()

		// an unverified user should not be cached
		muls.flavour = Unverified
		warwickIds = Seq("1234567", "1234568")
		val unverifiedUsers = muls.getUsersByWarwickUniIds(warwickIds)
		unverifiedUsers.keys.toSet should be (Set("1234567", "1234568"))
		unverifiedUsers.values.foreach { unverifiedUser =>
			unverifiedUser.isVerified should be {false}
			unverifiedUser.isFoundUser should be {false}
		}
		cache.getStatistics.getCacheSize should be (0)
		warwickIds.foreach { warwickId => cache.contains(warwickId) should be {false} }

		// a good user should be cached
		muls.flavour = Vanilla
		warwickIds = Seq("0123456", "0123457")
		val existingUsers = muls.getUsersByWarwickUniIds(warwickIds)
		existingUsers.values.foreach { existingUser =>
			existingUser.isVerified should be {true}
			existingUser.isFoundUser should be {true}
		}
		cache.getStatistics.getCacheSize should be (2)
		warwickIds.foreach { warwickId => cache.contains(warwickId) should be {true} }

		// an applicant user should NOT be cached (TAB-1734)
		muls.flavour = Applicant
		warwickIds = Seq("1819201", "1819202")
		val applicantUsers = muls.getUsersByWarwickUniIds(warwickIds)
		applicantUsers.values.foreach { applicantUser =>
			applicantUser.isVerified should be {true}
			applicantUser.isFoundUser should be {false}
		}
		cache.getStatistics.getCacheSize should be (2)
		warwickIds.foreach { warwickId => cache.contains(warwickId) should be {false} }

		// an anonymous user should NOT be cached (TAB-1734)
		muls.flavour = Anonymous
		warwickIds = Seq("0987654", "0987655")
		val anonUsers = muls.getUsersByWarwickUniIds(warwickIds)
		anonUsers.values.foreach { anonUser =>
			anonUser.isVerified should be {true}
			anonUser.isFoundUser should be {false}
		}
		cache.getStatistics.getCacheSize should be (2)
		warwickIds.foreach { warwickId => cache.contains(warwickId) should be {false} }
	}
}

