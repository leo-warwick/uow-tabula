package uk.ac.warwick.tabula.services.elasticsearch

import java.util.concurrent.TimeoutException

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.{Department, Member, MemberUserType}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.elasticsearch.ProfileQuerySanitisation._
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent}

import scala.concurrent.duration._

trait ProfileQueryService
  extends ProfileQueryMethods

trait ProfileQueryMethods {
  def findWithQuery(
    query: String,
    departments: Seq[Department],
    includeTouched: Boolean,
    userTypes: Set[MemberUserType],
    searchAcrossAllDepartments: Boolean,
    activeOnly: Boolean
  ): Seq[Member]

  def find(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], searchAllDepts: Boolean, activeOnly: Boolean): Seq[Member] = {
    if (!query.hasText) Nil
    else findWithQuery(query, departments, includeTouched = true, userTypes = userTypes, searchAcrossAllDepartments = searchAllDepts, activeOnly = activeOnly)
  }

  def find(ownDepartment: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member] =
    findWithQuery("", Seq(ownDepartment), includeTouched, userTypes, searchAcrossAllDepartments = false, activeOnly = true)
}

@Service
class ProfileQueryServiceImpl extends AbstractQueryService
  with ProfileQueryService
  with ProfileQueryMethodsImpl
  with ProfileServiceComponent {

  /**
    * The name of the index alias that this service reads from
    */
  @Value("${elasticsearch.index.profiles.alias}") var indexName: String = _
  lazy val index = Index(indexName)

  @Autowired var profileService: ProfileService = _

}

trait ProfileQuerySanitisation {
  private val Title = """^(?:Mr|Ms|Mrs|Miss|Dr|Sir|Doctor|Prof(?:essor)?)(\.?|\b)\s*""".r
  private val FullStops = """\.(\S)""".r

  def stripTitles(query: String): String =
    FullStops.replaceAllIn(
      Title.replaceAllIn(query, ""),
      ". $1")

  def sanitiseQuery(query: String): String = {
    val deslashed = query.replace("/", "\\/") // TAB-1331
    stripTitles(deslashed)
  }

  def autoWildcard(query: String): String =
    query.split("\\s+").map { str =>
      if (str.endsWith("*")) str
      else s"$str*"
    }.mkString(" ")
}

object ProfileQuerySanitisation extends ProfileQuerySanitisation

trait ProfileQueryMethodsImpl extends ProfileQueryMethods {
  self: ElasticsearchClientComponent
    with ElasticsearchSearching
    with ProfileServiceComponent =>

  def findWithQuery(
    query: String,
    departments: Seq[Department],
    includeTouched: Boolean,
    userTypes: Set[MemberUserType],
    searchAcrossAllDepartments: Boolean,
    activeOnly: Boolean
  ): Seq[Member] =
    if (departments.isEmpty && !searchAcrossAllDepartments) Seq()
    else try {
      val textQuery = query.maybeText.map { q =>
        queryStringQuery(autoWildcard(sanitiseQuery(q)))
          .defaultOperator("AND")
          .analyzeWildcard(true)
          .asfields("firstName", "lastName", "fullName")
      }

      val deptQuery =
        if (searchAcrossAllDepartments) None
        else {
          val deptQueries = departments.map { dept => termQuery("department", dept.code) }
          val touchedQueries =
            if (includeTouched) departments.map { dept => termQuery("touchedDepartments", dept.code) }
            else Nil

          Some(boolQuery().should(deptQueries ++ touchedQueries))
        }

      val userTypeQuery =
        if (userTypes.isEmpty) None
        else Some(boolQuery().should(userTypes.map { userType => termQuery("userType", userType.dbValue) }))

      // Active only
      val inUseQuery = if (activeOnly) Some(
        boolQuery()
          .should(
            termQuery("inUseFlag", "Active"),
            prefixQuery("inUseFlag", "Inactive - Starts")
          )
      ) else None

      // Course ended in the previous 6 months
      val courseEndedQuery = if (activeOnly) Some(
        rangeQuery("courseEndDate")
          gte DateFormats.IsoDate.print(DateTime.now.minusMonths(6))
          lte DateFormats.IsoDate.print(DateTime.now.plusYears(300))
      ) else None

      val queries = Seq(textQuery, deptQuery, userTypeQuery, inUseQuery, courseEndedQuery).flatten

      val universityIds = client.execute {
        searchRequest.query(boolQuery().must(queries))
      }
        .map { response => response.result.hits.hits.map(_.id).toSeq
        }
        .recover { case _ => Nil } // ignore any error
        .await(15.seconds) // Avoid Hibernate horror by waiting for the Future here, then initialising in the main thread

      if (activeOnly)
        universityIds.flatMap(profileService.getMemberByUniversityId(_))
      else
        universityIds.flatMap(profileService.getMemberByUniversityIdStaleOrFresh(_))
    } catch {
      case _: TimeoutException => Seq() // Invalid query string or timeout
    }
}

trait ProfileQueryServiceComponent {
  def profileQueryService: ProfileQueryService
}

trait AutowiringProfileQueryServiceComponent extends ProfileQueryServiceComponent {
  var profileQueryService: ProfileQueryService = Wire[ProfileQueryService]
}
