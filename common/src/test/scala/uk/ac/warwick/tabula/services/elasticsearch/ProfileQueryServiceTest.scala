package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import org.joda.time.DateTime
import org.junit.{After, Before}
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.MemberUserType.{Staff, Student}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.services.ProfileService

class ProfileQueryServiceTest extends ElasticsearchTestBase with Mockito {

	override implicit val patienceConfig =
		PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

	val indexName = "profiles"
	val indexType: String = new ProfileIndexType {}.indexType

	private trait Fixture {
		val queryService = new ProfileQueryServiceImpl
		queryService.profileService = mock[ProfileService]
		queryService.client = ProfileQueryServiceTest.this.client
		queryService.indexName = ProfileQueryServiceTest.this.indexName

		implicit val indexable = ProfileIndexService.MemberIndexable
	}

	@Before def setUp(): Unit = {
		new ProfileElasticsearchConfig {
			client.execute {
				create index indexName mappings (mapping(indexType) fields fields) analysis analysers
			}.await.isAcknowledged should be(true)
		}
		blockUntilIndexExists(indexName)
	}

	@After def tearDown(): Unit = {
		client.execute { delete index indexName }
		blockUntilIndexNotExists(indexName)
	}

	@Test def find(): Unit = withFakeTime(dateTime(2000, 6)) { new Fixture {
		val m = new StudentMember
		m.universityId = "0672089"
		m.userId = "cuscav"
		m.firstName = "Mathew"
		m.fullFirstName = "Mathew James"
		m.lastName = "Mannion"
		m.homeDepartment = Fixtures.department("CS", "Computer Science")
		m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0)
		m.userType = Student
		m.inUseFlag = "Active"

		queryService.profileService.getMemberByUniversityId("0672089") returns Some(m)

		// Index the profile
		client.execute { index into indexName / indexType source m.asInstanceOf[Member] id m.id }
		blockUntilCount(1, indexName, indexType)

		// General sanity that this is working before we go into the tests of the query service
		search in indexName / indexType should containResult(m.universityId)
		search in indexName / indexType query queryStringQuery("Mathew") should containResult(m.universityId)
		search in indexName / indexType query queryStringQuery("mat*") should containResult(m.universityId)
		search in indexName / indexType query termQuery("userType", "S") should containResult(m.universityId)

		queryService.find("bob thornton", Seq(m.homeDepartment), Set(), searchAllDepts = false) should be ('empty)
		queryService.find("Mathew", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("mat", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("mann", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("m mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("mathew james mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("mat mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("m m", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("m m", Seq(m.homeDepartment), Set(Student, Staff), searchAllDepts = false).head should be (m)
		queryService.find("m m", Seq(Fixtures.department("OT", "Some other department"), m.homeDepartment), Set(Student, Staff), searchAllDepts = false).head should be (m)
		queryService.find("m m", Seq(Fixtures.department("OT", "Some other department")), Set(Student, Staff), searchAllDepts = false) should be ('empty)
		queryService.find("m m", Seq(m.homeDepartment), Set(Staff), searchAllDepts = false) should be ('empty)
	}}

	@Test def findCopesWithApostrophes(): Unit = withFakeTime(dateTime(2000, 6)) { new Fixture {
		val m = new StudentMember
		m.universityId = "0000001"
		m.userId = "helpme"
		m.firstName = "Johnny"
		m.fullFirstName = "Jonathan"
		m.lastName = "O'Connell"
		m.homeDepartment = Fixtures.department("CS", "Computer Science")
		m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0)
		m.userType = Student
		m.inUseFlag = "Active"

		queryService.profileService.getMemberByUniversityId(m.universityId) returns Some(m)

		// Index the profile
		client.execute { index into indexName / indexType source m.asInstanceOf[Member] id m.id }
		blockUntilCount(1, indexName, indexType)

		queryService.find("bob thornton", Seq(m.homeDepartment), Set(), searchAllDepts = false) should be ('empty)
		queryService.find("joconnell", Seq(m.homeDepartment), Set(), searchAllDepts = false) should be ('empty)
		queryService.find("o'connell", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("connell", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("johnny connell", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("johnny o'connell", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("j o connell", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("j oconnell", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("j o'c", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("j o c", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
	}}

	@Test def asciiFolding(): Unit = withFakeTime(dateTime(2000, 6)) { new Fixture {
		val m = new StudentMember
		m.universityId = "1300623"
		m.userId = "smrlar"
		m.firstName = "Aist\u0117"
		m.fullFirstName = "Aist\u0117"
		m.lastName = "Kiltinavi\u010Di\u016Ba"
		m.homeDepartment = Fixtures.department("CS", "Computer Science")
		m.lastUpdatedDate = new DateTime(2000,1,2,0,0,0)
		m.userType = Student
		m.inUseFlag = "Active"

		queryService.profileService.getMemberByUniversityId(m.universityId) returns Some(m)

		// Index the profile
		client.execute { index into indexName / indexType source m.asInstanceOf[Member] id m.id }
		blockUntilCount(1, indexName, indexType)

		// General sanity that this is working before we go into the tests of the query service
		search in indexName / indexType should containResult(m.universityId)

		queryService.find("bob thornton", Seq(m.homeDepartment), Set(), searchAllDepts = false) should be ('empty)
		queryService.find("Aist\u0117", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("aist", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("aiste", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("a kiltinavi\u010Di\u016Ba", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("aiste kiltinavi\u010Di\u016Ba", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
		queryService.find("aiste kiltinaviciua", Seq(m.homeDepartment), Set(), searchAllDepts = false).head should be (m)
	}}

	@Test def stripTitles(): Unit = new ProfileQuerySanitisation {
		stripTitles("Mathew Mannion") should be ("Mathew Mannion")
		stripTitles("Mr Mathew Mannion") should be ("Mathew Mannion")
		stripTitles("Mr. Mathew Mannion") should be ("Mathew Mannion")
		stripTitles("Prof.Mathew Mannion") should be ("Mathew Mannion")
	}

	@Test def sanitiseQuery(): Unit = new ProfileQuerySanitisation {
		sanitiseQuery("//x/y/") should be ("\\/\\/x\\/y\\/")
		sanitiseQuery("Prof.Mathew Mannion/Mat Mannion") should be ("Mathew Mannion\\/Mat Mannion")
	}

}
