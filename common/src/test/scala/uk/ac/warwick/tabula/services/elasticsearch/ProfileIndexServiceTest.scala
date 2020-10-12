package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.searches.sort.SortOrder
import com.sksamuel.elastic4s.{Index, Response}
import org.hibernate.Session
import org.joda.time.DateTime
import org.junit.After
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.data.{MemberDaoImpl, SessionComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, PersistenceTestBase, TestElasticsearchClient}
import uk.ac.warwick.util.core.StopWatch

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future

class ProfileIndexServiceTest extends PersistenceTestBase with Mockito with TestElasticsearchClient {

  val index = Index("profile")

  private trait Fixture {
    val dao: MemberDaoImpl = new MemberDaoImpl with SessionComponent {
      val session: Session = sessionFactory.getCurrentSession
    }

    val indexer = new ProfileIndexService
    indexer.indexName = ProfileIndexServiceTest.this.index.name
    indexer.client = ProfileIndexServiceTest.this.client
    indexer.memberDao = dao

    // Creates the index
    indexer.ensureIndexExists().await should be(true)

    implicit val indexable: ElasticsearchIndexable[Member] = ProfileIndexService.MemberIndexable
  }

  @After def tearDown(): Unit = {
    deleteIndex(index.name)
  }

  @Transactional
  @Test def fields(): Unit = withFakeTime(dateTime(2000, 6)) {
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

      indexer.indexItems(Seq(m)).await
      blockUntilCount(1, index.name)

      // University ID is the ID field so it isn't in the doc source
      val doc: GetResponse = client.execute {
        get(index, m.universityId)
      }.futureValue.result

      doc.source should be(Map(
        "userId" -> "cuscav",
        "firstName" -> "Mathew",
        "lastName" -> "Mannion",
        "fullName" -> "Mathew Mannion",
        "department" -> List("CS"),
        "touchedDepartments" -> List("CS"),
        "courseEndDate" -> "2100-06-01", // 100 years after the "current" date
        "userType" -> "S",
        "inUseFlag" -> "Active",
        "lastUpdatedDate" -> "2000-01-02T00:00:00Z"
      ))
    }
  }

  @Transactional
  @Test def indexing(): Unit = withFakeTime(dateTime(2000, 6)) {
    new Fixture {
      val stopwatch = new StopWatch
      stopwatch.start("creating items")

      val items: IndexedSeq[StudentMember] = for (i <- 1 to 100)
        yield {
          val m = new StudentMember
          m.universityId = i.toString
          m.userId = i.toString
          m.lastUpdatedDate = new DateTime(2000, 1, 2, 0, 0, 0).plusSeconds(i)
          m.userType = Student
          m.inUseFlag = "Active"

          m
        }
      stopwatch.stop()

      for (item <- items) dao.saveOrUpdate(item)
      session.flush()
      dao.getByUniversityId("1").isDefined should be(true)

      dao.listUpdatedSince(new DateTime(2000, 1, 1, 0, 0, 0), 10).size should be(10)
      dao.listUpdatedSince(new DateTime(1999, 6, 1, 0, 0, 0), 25).size should be(25)

      stopwatch.start("indexing")

      indexer.indexFrom(new DateTime(2000, 1, 1, 0, 0, 0)).await

      blockUntilCount(100, index.name)
      client.execute {
        search(index)
      }.await.result.totalHits should be(100)

      stopwatch.stop()

      def listRecent(max: Int): Future[Response[SearchResponse]] =
        client.execute {
          search(index).sortBy(fieldSort("lastUpdatedDate").order(SortOrder.Desc)).limit(max)
        }

      listRecent(100).futureValue.result.hits.hits.length should be(100)

      val moreItems: Seq[StudentMember] = {
        val m = new StudentMember
        m.universityId = "x9000"
        m.userId = "x9000"
        m.lastUpdatedDate = new DateTime(2000, 1, 1, 0, 0, 0).plusSeconds(9000)
        m.inUseFlag = "Active"

        val members = Seq(m)
        members.foreach {
          dao.saveOrUpdate(_)
        }
        dao.getByUniversityId("x9000")

        members
      }
      indexer.indexItems(moreItems)

      blockUntilCount(101, index.name)

      listRecent(13).futureValue.result.hits.hits.length should be(13)

      // First query is slowest, but subsequent queries quickly drop
      // to a much smaller time
      for (i <- 1 to 20) {
        stopwatch.start("searching for newest item forever attempt " + i)
        val newest =
          client.execute {
            search(index).sortBy(fieldSort("lastUpdatedDate").order(SortOrder.Desc)).limit(1)
          }

        newest.futureValue.result.hits.hits.head.sourceAsMap("userId") should be("100")
        stopwatch.stop()
      }

      // index again to check that it doesn't do any once-only stuff
      indexer.indexFrom(indexer.newestItemInIndexDate.await.get).await
    }
  }

}
