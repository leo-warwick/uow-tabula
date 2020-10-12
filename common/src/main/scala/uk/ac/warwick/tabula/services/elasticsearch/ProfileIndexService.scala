package uk.ac.warwick.tabula.services.elasticsearch

import java.io.Closeable

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.analysis._
import com.sksamuel.elastic4s.requests.mappings.FieldDefinition
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentMember}
import uk.ac.warwick.tabula.data.{MemberDao, MemberDaoComponent}

import scala.collection.mutable
import scala.concurrent.Future

object ProfileIndexService {

  implicit object MemberIndexable extends ElasticsearchIndexable[Member] {

    // Having this as a def doesn't really have any significant performance change, but allows the value of DateTime.now
    // to change for tests
    private def farAwayDateTime = DateTime.now.plusYears(100)

    override def fields(item: Member): Map[String, Any] = {
      var fields = mutable.Map[String, Any]()

      fields ++= Seq(
        "userId" -> item.userId,
        "department" -> item.affiliatedDepartments.map(_.code),
        "touchedDepartments" -> item.touchedDepartments.map(_.code),
        "lastUpdatedDate" -> DateFormats.IsoDateTime.print(item.lastUpdatedDate)
      )

      Option(item.firstName).foreach {
        fields += "firstName" -> _
      }
      Option(item.lastName).foreach {
        fields += "lastName" -> _
      }
      item.fullName.foreach {
        fields += "fullName" -> _
      }

      Option(item.userType).foreach {
        fields += "userType" -> _.dbValue
      }

      // Treat permanently withdrawn students as inactive
      item match {
        case student: StudentMember
          if Option(student.freshStudentCourseDetails).nonEmpty && student.mostSignificantCourseDetails.nonEmpty =>

          fields += "inUseFlag" -> "Active" //  students should always be active; use the course date for filtering

          if (student.mostSignificantCourseDetails.get.isEnded) {
            fields += "courseEndDate" -> DateFormats.IsoDate.print(student.mostSignificantCourseDetails.get.endDate)
          } else {
            // Index a date in the future so range queries work
            fields += "courseEndDate" -> DateFormats.IsoDate.print(farAwayDateTime)
          }
        case _ =>
          Option(item.inUseFlag).foreach {
            fields += "inUseFlag" -> _
          }

          // Index a date in the future so range queries work
          fields += "courseEndDate" -> DateFormats.IsoDate.print(farAwayDateTime)
      }

      fields.toMap
    }

    override def lastUpdatedDate(item: Member): DateTime = item.lastUpdatedDate

  }

  object Synonyms {

    // TODO this is so very rubbish
    // Collected from http://search.warwick.ac.uk/search/admin/editNameSynonyms.html 13/12/12

    // Turn scalastyle off because string literals are ok(-ish) here
    // scalastyle:off
    val names: Map[String, Set[String]] = Map(
      "alan" -> Set("allan"),
      "allan" -> Set("alan"),
      "amanda" -> Set("mandy"),
      "andrew" -> Set("andy"),
      "andy" -> Set("andrew"),
      "becky" -> Set("rebecca"),
      "bill" -> Set("william"),
      "billy" -> Set("bill", "william"),
      "bob" -> Set("robert", "rob"),
      "cath" -> Set("katherine"),
      "catherine" -> Set("katherine", "cath"),
      "charles" -> Set("charlie"),
      "charlie" -> Set("charles"),
      "christina" -> Set("christine", "tina"),
      "christine" -> Set("christina", "tina"),
      "christopher" -> Set("chris"),
      "claire" -> Set("clare"),
      "clare" -> Set("claire"),
      "daniel" -> Set("danny", "dan"),
      "danielle" -> Set("danny"),
      "danny" -> Set("dan", "daniel", "danielle"),
      "dave" -> Set("david"),
      "david" -> Set("dave"),
      "debbie" -> Set("debby", "deb", "debra", "deborah"),
      "debby" -> Set("debbie", "deb", "debra", "deborah"),
      "deborah" -> Set("debbie", "debby", "deb", "debra"),
      "debra" -> Set("debbie", "debby", "deb", "deborah"),
      "dick" -> Set("richard"),
      "dorothea" -> Set("dorothy"),
      "dorothy" -> Set("dorothea"),
      "eleanor" -> Set("ellie"),
      "elizabeth" -> Set("liz"),
      "ellie" -> Set("eleanor"),
      "gill" -> Set("jill", "jillian"),
      "gillian" -> Set("gill", "jill", "jillian"),
      "graeme" -> Set("graham"),
      "graham" -> Set("graeme"),
      "gregg" -> Set("greg", "gregory"),
      "gregory" -> Set("greg", "gregg"),
      "hollie" -> Set("holly"),
      "holly" -> Set("hollie"),
      "jackie" -> Set("jacqueline", "jacquie"),
      "jacqueline" -> Set("jackie", "jacquie"),
      "jacquie" -> Set("jackie", "jacqueline"),
      "jaimey" -> Set("james", "jamie", "jamey", "jaimy", "jim", "jimmy"),
      "jaimy" -> Set("james", "jamie", "jamey", "jaimey", "jim", "jimmy"),
      "jaine" -> Set("jane", "jayne"),
      "james" -> Set("jamie", "jamey", "jaimey", "jaimy", "jim", "jimmy"),
      "jamey" -> Set("james", "jamie", "jaimey", "jaimy", "jim", "jimmy"),
      "jamie" -> Set("james", "jamey", "jaimey", "jaimy", "jim", "jimmy"),
      "jane" -> Set("jayne", "jaine"),
      "janet" -> Set("jan"),
      "janette" -> Set("janet", "jan"),
      "jayne" -> Set("jane", "jaine"),
      "jenifer" -> Set("jennifer", "jeniffer", "jenniffer", "jenny", "jennie"),
      "jeniffer" -> Set("jenniffer", "jenifer", "jennifer", "jenny", "jennie"),
      "jennie" -> Set("jenifer", "jennifer", "jeniffer", "jenniffer", "jenny"),
      "jennifer" -> Set("jenniffer", "jenifer", "jeniffer", "jenny", "jennie"),
      "jenniffer" -> Set("jennifer", "jenifer", "jeniffer", "jenny", "jennie"),
      "jenny" -> Set("jenifer", "jennifer", "jeniffer", "jenniffer", "jennie"),
      "jill" -> Set("gill", "gillian"),
      "jillian" -> Set("gillian", "gill", "jill"),
      "jim" -> Set("james", "jamie", "jamey", "jaimey", "jaimy", "jimmy"),
      "jimmy" -> Set("james", "jamie", "jamey", "jaimey", "jaimy", "jim"),
      "john" -> Set("jon", "jonathan"),
      "jon" -> Set("john", "johnathan"),
      "julia" -> Set("julie"),
      "julie" -> Set("julia"),
      "justina" -> Set("justine", "tina"),
      "justine" -> Set("justina", "tina"),
      "kate" -> Set("katie"),
      "katherine" -> Set("cath", "catherine"),
      "katie" -> Set("kate"),
      "kenneth" -> Set("ken"),
      "lewellyn" -> Set("lewelyn", "llewelyn", "llewellyn"),
      "lewelyn" -> Set("lewellyn", "llewelyn", "llewellyn"),
      "liz" -> Set("elizabeth"),
      "llewellyn" -> Set("lewellyn", "llewelyn", "lewelyn"),
      "llewelyn" -> Set("lewellyn", "lewelyn", "llewellyn"),
      "mandy" -> Set("amanda"),
      "mathew" -> Set("matthew"),
      "mathews" -> Set("matthews"),
      "matt" -> Set("mathew", "mat"),
      "matthew" -> Set("mathew"),
      "matthews" -> Set("mathews"),
      "michael" -> Set("mike"),
      "michele" -> Set("michelle"),
      "michelle" -> Set("michele"),
      "mike" -> Set("michael"),
      "nicholas" -> Set("nick", "nicolas"),
      "nick" -> Set("nicholas", "nicolas"),
      "nicolas" -> Set("nicholas", "nick"),
      "patricia" -> Set("pat"),
      "patrick" -> Set("pat"),
      "penelope" -> Set("penny"),
      "penny" -> Set("penelope"),
      "peter" -> Set("pete"),
      "philip" -> Set("phillip", "phil"),
      "philipa" -> Set("phillipa", "phillippa", "philippa", "pippa"),
      "philippa" -> Set("philipa", "phillippa", "phillipa", "pippa"),
      "phillip" -> Set("philip", "phil"),
      "phillipa" -> Set("philipa", "phillippa", "philippa", "pippa"),
      "phillippa" -> Set("philipa", "phillipa", "philippa", "pippa"),
      "pippa" -> Set("philipa", "phillippa", "phillipa", "philippa"),
      "rebecca" -> Set("becky"),
      "richard" -> Set("dick", "dickie"),
      "rob" -> Set("bob"),
      "robert" -> Set("bob", "rob"),
      "robin" -> Set("robyn"),
      "robyn" -> Set("robin"),
      "sarah" -> Set("sara"),
      "shane" -> Set("shayne"),
      "shayne" -> Set("shane"),
      "stephen" -> Set("steven"),
      "steve" -> Set("stephen"),
      "steven" -> Set("stephen", "steve"),
      "sue" -> Set("susannah", "susanna", "suzannah", "suzanna", "suzi", "suzie", "susan", "suzan"),
      "susan" -> Set("suzannah", "suzanna", "susanna", "susannah", "sue", "suzi", "suzan"),
      "suzan" -> Set("suzannah", "suzanna", "susanna", "susannah", "sue", "suzi", "susan"),
      "suzanna" -> Set("suzi", "suzie", "susanna", "susannah", "sue", "susan", "suzan"),
      "suzannah" -> Set("suzanna", "suzi", "suzie", "susanna", "sue", "susan", "suzan"),
      "suzi" -> Set("suzannah", "suzanna", "susanna", "susannah", "sue", "susan", "suzan"),
      "suzie" -> Set("suzi", "suzannah", "suzanna", "susannah", "susanna", "sue", "susan", "suzan"),
      "thomas" -> Set("tom"),
      "tina" -> Set("christine", "christina", "justine", "justina"),
      "tom" -> Set("thomas"),
      "wahlberg" -> Set("wahlburg"),
      "wahlburg" -> Set("wahlberg"),
      "william" -> Set("bill", "billy")
    )
  }

}

@Service
class ProfileIndexService
  extends AbstractIndexService[Member]
    with MemberDaoComponent
    with ProfileElasticsearchConfig {

  override implicit val indexable: ElasticsearchIndexable[Member] = ProfileIndexService.MemberIndexable

  /**
    * The name of the index that this service writes to
    */
  @Value("${elasticsearch.index.profiles.name}") var indexName: String = _
  lazy val index = Index(indexName)

  @Autowired var memberDao: MemberDao = _

  // largest batch of items we'll load in at once during scheduled incremental index.
  final override val IncrementalBatchSize = 1500

  override val UpdatedDateField: String = "lastUpdatedDate"

  def indexByDateAndDepartment(startDate: DateTime, dept: Department): Future[ElasticsearchIndexingResult] = {
    val deptMembers = memberDao.listUpdatedSince(startDate, dept).all
    if (debugEnabled) logger.debug("Indexing " + deptMembers.size + " members with home department " + dept.code)
    indexItems(deptMembers)
  }

  // Note batch size is ignored - we use a Scrollable and it will go through all newer items
  override protected def listNewerThan(startDate: DateTime, batchSize: Int): Iterator[Member] with Closeable =
    memberDao.listUpdatedSince(startDate).all
}

trait ProfileElasticsearchConfig extends ElasticsearchConfig {
  override def fields: Seq[FieldDefinition] = Seq(
    // id field stores the universityId
    textField("firstName").analyzer("name"),
    textField("lastName").analyzer("name"),
    textField("fullName").analyzer("name"),

    keywordField("inUseFlag"),
    keywordField("userType"),

    keywordField("department"),
    keywordField("touchedDepartments"),

    dateField("courseEndDate").format("strict_date"),
    dateField("lastUpdatedDate").format("strict_date_time_no_millis")
  )

  override def analysis: Analysis = Analysis(
    // A custom analyzer for names
    CustomAnalyzer(name = "name",
      // Delimit on whitespace (preserves punctuation for the word delimiter filter)
      tokenizer = "whitespace",
      tokenFilters = List("name_multiplexer")
    )
  ).copy(tokenFilters = List(
    // Define the synonym token filter first, otherwise other defined filters will run on the synonyms
    SynonymGraphTokenFilter(
      "name_synonyms",
      synonyms = ProfileIndexService.Synonyms.names.toSeq.map { case (key, values) =>
        s"$key => ${values.mkString(",")}"
      }.toSet
    ).expand(false),

    WordDelimiterGraphTokenFilter(
      name = "name_word_delimiter",
      catenateAll = Some(true),
      stem_english_possessive = Some(false),
    ),

    // Multi paths here, preserving original, as word delimiter can't run on synonyms
    MultiplexerTokenFilter("name_multiplexer")
      .filters(Seq(
        "lowercase, asciifolding, name_synonyms",
        "lowercase, asciifolding, name_word_delimiter",
      ))
      .preserveOriginal(true),
  ))
}
