package uk.ac.warwick.tabula.services.timetables

import java.time.Duration

import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.client.{HttpResponseException, ResponseHandler}
import org.apache.http.util.EntityUtils
import org.joda.time.format._
import org.joda.time.{DateTime, LocalDate, Period}
import org.springframework.stereotype.Service
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.trusted.TrustedApplicationUtils
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.convert.FiniteDurationConverter
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, CacheStrategyComponent}
import uk.ac.warwick.tabula.services.timetables.ExamTimetableFetchingService._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.util.cache._

import scala.concurrent.Future
import scala.jdk.DurationConverters._
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

trait ExamTimetableConfiguration {
  val examTimetableUrl: String
  val examProfilesUrl: String
  val examProfilesCacheExpiry: Duration
}

trait ExamTimetableConfigurationComponent {
  val examTimetableConfiguration: ExamTimetableConfiguration
}

trait AutowiringExamTimetableConfigurationComponent extends ExamTimetableConfigurationComponent {
  val examTimetableConfiguration = new AutowiringExamTimetableConfiguration

  class AutowiringExamTimetableConfiguration extends ExamTimetableConfiguration {
    lazy val examTimetableUrl: String = Wire.optionProperty("${examTimetable.url}").getOrElse("https://exams.warwick.ac.uk/timetable/")
    lazy val examProfilesUrl: String = Wire.optionProperty("${examProfiles.url}").getOrElse("https://exams.warwick.ac.uk/api/v1/examProfiles.json")
    lazy val examProfilesCacheExpiry: Duration =
      Wire.optionProperty("${examProfiles.cacheExpiry}")
        .map(FiniteDurationConverter.asDuration)
        .map(_.toJava)
        .getOrElse(Duration.ofHours(1))
  }

}

trait ExamTimetableHttpTimetableFetchingServiceComponent extends StaffAndStudentTimetableFetchingServiceComponent {
  self: ExamTimetableConfigurationComponent =>

  lazy val timetableFetchingService: StudentTimetableFetchingService with StaffTimetableFetchingService =
    ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
}

private class ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration: ExamTimetableConfiguration)
  extends StaffTimetableFetchingService with StudentTimetableFetchingService with Logging {

  self: ApacheHttpClientComponent with TrustedApplicationsManagerComponent
    with UserLookupComponent with FeaturesComponent with TopLevelUrlComponent =>

  // an HTTPClient response handler which reads XML from the response and parses it into a list of TimetableEvents
  def handler(uniId: String): ResponseHandler[Seq[TimetableEvent]] =
    ApacheHttpClientUtils.handler {
      // Catch 404s and handle them quietly
      case response if response.getStatusLine.getStatusCode == HttpStatus.SC_NOT_FOUND =>
        EntityUtils.consumeQuietly(response.getEntity)
        Nil

      case response =>
        ApacheHttpClientUtils.xmlResponseHandler { node =>
          ExamTimetableHttpTimetableFetchingService.parseXml(node, uniId, toplevelUrl)
        }.handleResponse(response)
    }

  private def featureProtected(arg: String)(f: String => Future[EventList]): Future[EventList] = {
    if (features.personalExamTimetables) {
      f(arg)
    } else {
      Future(EventList(Nil, None))
    }
  }

  override def getTimetableForStudent(universityId: String): Future[EventList] = featureProtected(universityId)(doRequest)

  override def getTimetableForStaff(universityId: String): Future[EventList] = featureProtected(universityId)(doRequest)

  def doRequest(param: String): Future[EventList] = {
    userLookup.getUserByWarwickUniId(param) match {
      case user if !user.isFoundUser || user.isLoginDisabled =>
        logger.info(s"Tried to get exam timetable for $param but could find no active user")
        Future.successful(EventList(Nil, None))
      case user =>
        val endpoint = s"${examTimetableConfiguration.examTimetableUrl}timetable.xml"

        val req = RequestBuilder.get(endpoint).build()
        TrustedApplicationUtils.signRequest(applicationManager.getCurrentApplication, user.getUserId, req)

        logger.info(s"Requesting exam timetable data from $endpoint")

        val result = Future {
          httpClient.execute(req, handler(param))
        }
        result.onComplete {
          case Failure(e: HttpResponseException) =>
            logger.warn(s"Request for $endpoint failed: HTTP ${e.getStatusCode}", e)
          case Failure(e) =>
            logger.warn(s"Request for $endpoint failed", e)
          case _ =>
        }
        result.map(EventList.fresh)
    }
  }

}

object ExamTimetableHttpTimetableFetchingService extends Logging {

  val cacheName = "ExamTimetableLists"

  def apply(examTimetableConfiguration: ExamTimetableConfiguration): CachedStaffAndStudentTimetableFetchingService = {
    val service = new ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
      with AutowiringApacheHttpClientComponent
      with AutowiringTrustedApplicationsManagerComponent
      with AutowiringUserLookupComponent
      with AutowiringFeaturesComponent
      with AutowiringTopLevelUrlComponent

    new CachedStaffAndStudentTimetableFetchingService(service, cacheName)
  }

  def parseXml(xml: Elem, universityId: String, topLevelUrl: String): Seq[TimetableEvent] = {
    val timetable = ExamTimetableFetchingService.examTimetableFromXml(xml)

    timetable.exams.flatMap { timetableExam =>
      val uid = DigestUtils.md5Hex(Seq(timetableExam.profile, timetableExam.paper).mkString)
      Try(timetableExam.academicYear.weekForDate(timetableExam.startDateTime.toLocalDate).weekNumber) match {
        case Success(weekNumber) =>
          Some(TimetableEvent(
            uid = uid,
            name = "Exam",
            title = "Exam",
            description = "",
            eventType = TimetableEventType.Exam,
            weekRanges = Seq(WeekRange(weekNumber)),
            day = DayOfWeek(timetableExam.startDateTime.dayOfWeek.get),
            startTime = timetableExam.startDateTime.toLocalTime,
            endTime = timetableExam.endDateTime.toLocalTime,
            location = None,
            onlineDeliveryUrl = None,
            deliveryMethod = None,
            comments = Some(s"""More information available on the <a href="$topLevelUrl${Routes.Profile.examTimetable(universityId)}">exam timetable</a>"""),
            parent = TimetableEvent.Parent(),
            staff = Nil,
            students = Nil,
            year = timetableExam.academicYear,
            relatedUrl = None,
            attendance = Map()
          ))

        case Failure(t) =>
          logger.error(s"Failed to get a week number for ${timetableExam.startDateTime.toLocalDate} during academic year ${timetableExam.academicYear}", t)
          None
      }
    }
  }
}

object ExamTimetableFetchingService {

  val examDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("dd MMM HH:mm")
  val examPeriodFormatter: PeriodFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix("hr ").appendMinutes().appendSuffix("mins").toFormatter
  val examReadingTimeFormatter: PeriodFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix(":").appendMinutes().appendSuffix(":").appendSeconds().toFormatter

  case class ExamTimetableExam(
    profile: String,
    academicYear: AcademicYear,
    moduleCode: String,
    paper: String,
    section: String,
    length: Period,
    lengthString: String,
    readingTime: Option[Period],
    isOpenBook: Boolean,
    startDateTime: DateTime,
    endDateTime: DateTime,
    extraTimePerHour: Option[Int],
    room: String,
    seat: Option[String]
  )

  case class ExamTimetable(
    header: String,
    instructions: String,
    exams: Seq[ExamTimetableExam]
  )

  def examTimetableFromXml(xml: Elem): ExamTimetable = {
    val header = (xml \\ "exam-headerinfo").headOption.map(_.text).getOrElse("")
    val instructions = (xml \\ "exam-instructions").headOption.map(_.text).getOrElse("")

    if ((xml \\ "exam-profile").isEmpty) {
      ExamTimetable(header, instructions, Nil)
    } else {
      val exams = (xml \\ "exam-profile").flatMap { profile =>
        val profileCode = (profile \ "profile").text
        val academicYear = AcademicYear((profile \ "academic-year").text.toInt).extended

        (profile \ "exam").map { examNode =>
          val moduleCode = (examNode \\ "module").text
          val paper = (examNode \\ "paper").text
          val section = (examNode \\ "section").text
          val lengthString = (examNode \\ "length").text
          val length = examPeriodFormatter.parsePeriod(lengthString)
          val readingTime = (examNode \\ "read-time").headOption.flatMap(readingTimeNode =>
            readingTimeNode.text.maybeText.map(s => examReadingTimeFormatter.parsePeriod(s.replace("01/01/1900 ", "")))
          )
          val isOpenBook = (examNode \\ "open-book").text.maybeText.isDefined

          val startDateTime = examDateTimeFormatter.parseDateTime("%s %s".format((examNode \\ "date").text, (examNode \\ "time").text)).withYear(academicYear.endYear)
          val extraTimePerHour = (examNode \\ "extratime-perhr").headOption.flatMap(extraTimePerHourNode =>
            extraTimePerHourNode.text.maybeText.flatMap(s => Try(s.toInt).toOption)
          )
          val extraTime = extraTimePerHour.map(minutes =>
            Period.minutes((length.toStandardMinutes.getMinutes.toFloat / 60 * minutes).toInt)
          )
          val endDateTime = Seq(Some(length), readingTime, extraTime).flatten.foldLeft(startDateTime)((dt, period) => dt.plus(period))

          val room = (examNode \\ "room").text
          val seat = (examNode \\ "seat").text.maybeText

          ExamTimetableExam(
            profileCode,
            academicYear,
            moduleCode,
            paper,
            section,
            length,
            lengthString,
            readingTime,
            isOpenBook,
            startDateTime,
            endDateTime,
            extraTimePerHour,
            room,
            seat
          )
        }
      }

      ExamTimetable(header, instructions, exams)
    }
  }

  case class ExamProfile(
    code: String,
    name: String,
    academicYear: AcademicYear,
    startDate: LocalDate,
    endDate: LocalDate,
    published: Boolean,
    seatNumbersPublished: Boolean
  )

  val readsLocalDate: Reads[LocalDate] = implicitly[Reads[String]].map(LocalDate.parse(_, ISODateTimeFormat.localDateParser))
  val readsExamProfile: Reads[ExamProfile] = (
    (__ \ "code").read[String] and
    (__ \ "name").read[String] and
    (__ \ "academicYear").read[String].map(AcademicYear.parse) and
    (__ \ "startDate").read[LocalDate](readsLocalDate) and
    (__ \ "endDate").read[LocalDate](readsLocalDate) and
    (__ \ "published").read[Boolean] and
    (__ \ "seatNumbersPublished").read[Boolean]
  )(ExamProfile.apply _)

  // Yucky wrapper to make sure it's Serializable
  case class ExamProfileList(examProfiles: Seq[ExamProfile] with java.io.Serializable)
}

trait ExamTimetableFetchingService {
  def getTimetable(member: Member, viewer: CurrentUser): Future[ExamTimetableFetchingService.ExamTimetable]
  def getExamProfiles: Future[Seq[ExamProfile]]
}

abstract class AbstractExamTimetableFetchingService extends ExamTimetableFetchingService with Logging {
  self: ApacheHttpClientComponent
    with ExamTimetableConfigurationComponent
    with TrustedApplicationsManagerComponent
    with CacheStrategyComponent =>

  override def getTimetable(member: Member, viewer: CurrentUser): Future[ExamTimetableFetchingService.ExamTimetable] = {
    // staff and student have different end points
    val endpoint = if (member.universityId == viewer.universityId) {
      s"${examTimetableConfiguration.examTimetableUrl}timetable.xml"
    } else {
      s"${examTimetableConfiguration.examTimetableUrl}${member.universityId}.xml"
    }

    val req = RequestBuilder.get(endpoint).build()
    TrustedApplicationUtils.signRequest(applicationManager.getCurrentApplication, viewer.userId, req)

    logger.info(s"Requesting exam timetable data from $endpoint")

    // an HTTPClient response handler which reads XML from the response and parses it into an ExamTimetable
    val handler: ResponseHandler[ExamTimetable] =
      ApacheHttpClientUtils.xmlResponseHandler { node =>
        ExamTimetableFetchingService.examTimetableFromXml(node)
      }

    val result = Future {
      httpClient.execute(req, handler)
    }
    result.onComplete {
      case Failure(e: HttpResponseException) =>
        logger.warn(s"Request for $endpoint failed: HTTP ${e.getStatusCode}", e)
      case Failure(e) =>
        logger.warn(s"Request for $endpoint failed", e)
      case _ =>
    }
    result
  }

  val examProfilesCacheKey: String = "examProfiles"
  val examProfilesCacheEntryFactory: CacheEntryFactory[String, ExamProfileList] = new SingularCacheEntryFactory[String, ExamProfileList] {
    override def create(ignored: String): ExamProfileList = {
      val req = RequestBuilder.get(examTimetableConfiguration.examProfilesUrl).build()

      val handler: ResponseHandler[Seq[ExamProfile]] =
        ApacheHttpClientUtils.jsonResponseHandler { data =>
          if ((data \ "success").as[Boolean]) {
            (data \ "examProfiles").validate[Seq[ExamProfile]](Reads.seq(readsExamProfile)).fold(
              invalid => {
                logger.error(s"Error fetching exam profiles: $invalid")
                throw new IllegalStateException(s"Error fetching exam profiles: $invalid")
              },
              identity
            )
          } else throw new IllegalStateException(s"Invalid response from exam profiles endpoint: ${Json.prettyPrint(data)}")
        }

      Try(httpClient.execute(req, handler)).fold(
        t => throw new CacheEntryUpdateException("Couldn't fetch exam profiles", t),
        {
          case profiles: Seq[ExamProfile] with java.io.Serializable => ExamProfileList(profiles)
          case _ => throw new CacheEntryUpdateException("Unserializable collection returned from ExamTimetableFetchingService")
        }
      )
    }

    override def shouldBeCached(profiles: ExamProfileList): Boolean = true
  }

  lazy val examProfilesCache: Cache[String, ExamProfileList] =
    Caches.builder("ExamProfilesCache", examProfilesCacheEntryFactory, cacheStrategy)
      .expireAfterWrite(examTimetableConfiguration.examProfilesCacheExpiry)
      .maximumSize(1)
      .build()

  override def getExamProfiles: Future[Seq[ExamProfile]] = Future {
    examProfilesCache.get(examProfilesCacheKey)
      .examProfiles
  }
}

@Service("examTimetableFetchingService")
class ExamTimetableFetchingServiceImpl
  extends AbstractExamTimetableFetchingService
    with AutowiringApacheHttpClientComponent
    with AutowiringExamTimetableConfigurationComponent
    with AutowiringTrustedApplicationsManagerComponent
    with AutowiringUserLookupComponent
    with AutowiringCacheStrategyComponent

trait ExamTimetableFetchingServiceComponent {
  def examTimetableFetchingService: ExamTimetableFetchingService
}

trait AutowiringExamTimetableFetchingServiceComponent extends ExamTimetableFetchingServiceComponent {
  var examTimetableFetchingService: ExamTimetableFetchingService = Wire[ExamTimetableFetchingService]
}
