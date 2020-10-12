package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import org.joda.time.DateTime
import org.junit.{After, Before}
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.MemberUserType.{Staff, Student}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.services.ProfileService

class ProfileQueryServiceTest extends ElasticsearchTestBase with Mockito {

  val index: Index = Index("profiles")

  private trait Fixture {
    val queryService = new ProfileQueryServiceImpl
    queryService.profileService = mock[ProfileService]
    queryService.client = ProfileQueryServiceTest.this.client
    queryService.indexName = ProfileQueryServiceTest.this.index.name

    implicit val indexable: ElasticsearchIndexable[Member] = ProfileIndexService.MemberIndexable
  }

  @Before def setUp(): Unit = {
    new ProfileElasticsearchConfig {
      client.execute {
        createIndex(index.name).mapping(properties(fields)).analysis(analysis)
      }.await.result.acknowledged should be(true)
    }
    blockUntilIndexExists(index.name)
  }

  @After def tearDown(): Unit = {
    deleteIndex(index.name)
    blockUntilIndexNotExists(index.name)
  }

  @Test def find(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val m = new StudentMember
      m.universityId = "0672089"
      m.userId = "cuscav"
      m.firstName = "Mathew"
      m.lastName = "Mannion"
      m.homeDepartment = Fixtures.department("CS", "Computer Science")
      m.lastUpdatedDate = new DateTime(2000, 1, 2, 0, 0, 0)
      m.userType = Student
      m.inUseFlag = "Active"

      queryService.profileService.getMemberByUniversityId("0672089") returns Some(m)

      // Index the profile
      client.execute {
        indexInto(index).source(m.asInstanceOf[Member]).id(m.id)
      }.await.result.result should be ("created")
      blockUntilCount(1, index.name)

      // General sanity that this is working before we go into the tests of the query service
      search(index) should containId(m.universityId)
      search(index).query(queryStringQuery("Mathew")) should containId(m.universityId)
      search(index).query(queryStringQuery("mat*")) should containId(m.universityId)
      search(index).query(termQuery("userType", "S")) should containId(m.universityId)

      queryService.find("bob thornton", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
      queryService.find("Mathew", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("mat", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("mann", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("m mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("mathew james mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
      queryService.find("mat mannion", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("m m", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("m m", Seq(m.homeDepartment), Set(Student, Staff), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("m m", Seq(Fixtures.department("OT", "Some other department"), m.homeDepartment), Set(Student, Staff), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("m m", Seq(Fixtures.department("OT", "Some other department")), Set(Student, Staff), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
      queryService.find("m m", Seq(m.homeDepartment), Set(Staff), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
    }
  }

  @Test def findInactive(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val m = new StudentMember
      m.universityId = "0672089"
      m.userId = "cuscav"
      m.firstName = "Mathew"
      m.lastName = "Mannion"
      m.homeDepartment = Fixtures.department("CS", "Computer Science")
      m.lastUpdatedDate = new DateTime(2000, 1, 2, 0, 0, 0)
      m.userType = Student
      m.inUseFlag = "Inactive - Ended 12/12/99"

      queryService.profileService.getMemberByUniversityIdStaleOrFresh("0672089") returns Some(m)

      // Index the profile
      client.execute {
        indexInto(index).source(m.asInstanceOf[Member]).id(m.id)
      }.await.result.result should be ("created")
      blockUntilCount(1, index.name)

      // Check inactive student is filtered out
      queryService.find(query = "mat", departments = Seq(m.homeDepartment), userTypes = Set(), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))

      // Check inactive student is included
      queryService.find(query = "mat", departments = Seq(m.homeDepartment), userTypes = Set(), searchAllDepts = false, activeOnly = false).head should be(m)
    }
  }

  @Test def findCopesWithApostrophes(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val m = new StudentMember
      m.universityId = "0000001"
      m.userId = "helpme"
      m.firstName = "Johnny"
      m.lastName = "O'Connell"
      m.homeDepartment = Fixtures.department("CS", "Computer Science")
      m.lastUpdatedDate = new DateTime(2000, 1, 2, 0, 0, 0)
      m.userType = Student
      m.inUseFlag = "Active"

      queryService.profileService.getMemberByUniversityId(m.universityId) returns Some(m)

      // Index the profile
      client.execute {
        indexInto(index).source(m.asInstanceOf[Member]).id(m.id)
      }.await.result.result should be ("created")
      blockUntilCount(1, index.name)

      queryService.find("bob thornton", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
      queryService.find("joconnell", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
      queryService.find("o'connell", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("connell", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("johnny connell", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("johnny o'connell", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("j o connell", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("j oconnell", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("j o'c", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("j o c", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
    }
  }

  @Test def asciiFolding(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val m = new StudentMember
      m.universityId = "1300623"
      m.userId = "smrlar"
      m.firstName = "Aist\u0117"
      m.lastName = "Kiltinavi\u010Di\u016Ba"
      m.homeDepartment = Fixtures.department("CS", "Computer Science")
      m.lastUpdatedDate = new DateTime(2000, 1, 2, 0, 0, 0)
      m.userType = Student
      m.inUseFlag = "Active"

      queryService.profileService.getMemberByUniversityId(m.universityId) returns Some(m)

      // Index the profile
      client.execute {
        indexInto(index).source(m.asInstanceOf[Member]).id(m.id)
      }.await.result.result should be ("created")
      blockUntilCount(1, index.name)

      // General sanity that this is working before we go into the tests of the query service
      search(index) should containId(m.universityId)

      queryService.find("bob thornton", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
      queryService.find("Aist\u0117", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("aist", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("aiste", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("a kiltinavi\u010Di\u016Ba", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("aiste kiltinavi\u010Di\u016Ba", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("aiste kiltinaviciua", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
    }
  }

  @Test def synonyms(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val m = new StudentMember
      m.universityId = "0000001"
      m.userId = "helpme"
      m.firstName = "Jenniffer"
      m.lastName = "Saunders"
      m.homeDepartment = Fixtures.department("CS", "Computer Science")
      m.lastUpdatedDate = new DateTime(2000, 1, 2, 0, 0, 0)
      m.userType = Student
      m.inUseFlag = "Active"

      queryService.profileService.getMemberByUniversityId(m.universityId) returns Some(m)

      // Index the profile
      client.execute {
        indexInto(index).source(m.asInstanceOf[Member]).id(m.id)
      }.await.result.result should be ("created")
      blockUntilCount(1, index.name)

      queryService.find("bob thornton", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true) should be(Symbol("empty"))
      queryService.find("jenifer", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("jenifer saunders", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
      queryService.find("jennie saunders", Seq(m.homeDepartment), Set(), searchAllDepts = false, activeOnly = true).head should be(m)
    }
  }

  @Test def stripTitles(): Unit = new ProfileQuerySanitisation {
    stripTitles("Mathew Mannion") should be("Mathew Mannion")
    stripTitles("Mr Mathew Mannion") should be("Mathew Mannion")
    stripTitles("Mr. Mathew Mannion") should be("Mathew Mannion")
    stripTitles("Prof.Mathew Mannion") should be("Mathew Mannion")
  }

  @Test def sanitiseQuery(): Unit = new ProfileQuerySanitisation {
    sanitiseQuery("//x/y/") should be("\\/\\/x\\/y\\/")
    sanitiseQuery("Prof.Mathew Mannion/Mat Mannion") should be("Mathew Mannion\\/Mat Mannion")
  }

}
