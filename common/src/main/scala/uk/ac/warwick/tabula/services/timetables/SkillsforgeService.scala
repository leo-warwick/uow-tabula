package uk.ac.warwick.tabula.services.timetables

import com.google.common.base.Charsets
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpResponse, StatusLine}
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import org.springframework.http.HttpStatus
import play.api.libs.json._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.ApacheHttpClientComponent
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, FeaturesComponent, RequestFailedException}

import scala.concurrent.Future
import scala.util.control.NonFatal

trait SkillsforgeServiceComponent extends EventOccurrenceSourceComponent {
  self: SkillsForgeConfigurationComponent
    with FeaturesComponent
    with ApacheHttpClientComponent =>

  override def eventOccurrenceSource: EventOccurrenceSource = new SkillsforgeService

  class SkillsforgeService
    extends EventOccurrenceSource
      with Logging {

    // TAB-6942
    private def shouldQuerySkillsforge(member: Member): Boolean = features.skillsforge && existsInSkillsforge(member)

    private val appropriateEconomicsCourses = Set("TECA-L1PL", "TECA-L1PJ")
    private val appropriateEnrolmentStatusCodes = Set("1", "1P", "1PV", "1U", "1V", "2", "2V", "AA", "E", "F", "FP", "FPV", "FV", "L", "LN", "S", "T", "TI", "TL")

    private def doesMatchRelevantCourse(scyd: StudentCourseYearDetails): Boolean =
      scyd.studentCourseDetails.course.code.startsWith("R") || appropriateEconomicsCourses(scyd.studentCourseDetails.course.code)

    //If student enrolment status as Permanently withdrawn with transfer code as Successfully completed or enrolment status within one of those specified in appropriateEnrolmentStatusCodes then valid mamber for Skillsforge
    private def doesMatchRelevantEnrolmentStatus(scyd: StudentCourseYearDetails): Boolean =
      appropriateEnrolmentStatusCodes(scyd.enrolmentStatus.code) || (scyd.enrolmentStatus.code == "P" && scyd.studentCourseDetails.reasonForTransferCode == "SC")

    private def existsInSkillsforge(member: Member): Boolean = member match {
      case s: StudentMember => s.activeNow && s.freshOrStaleStudentCourseYearDetails(AcademicYear.now()).exists(scyd => {
        doesMatchRelevantCourse(scyd) && doesMatchRelevantEnrolmentStatus(scyd)
      })
      case _ => {
        if (logger.isDebugEnabled) logger.debug(s"Not querying Skillsforge for user ${member.userId} as they are not the right type of user.")
        false
      }
    }

    override def occurrencesFor(
      member: Member,
      currentUser: CurrentUser,
      context: TimetableEvent.Context,
      start: LocalDate,
      endInclusive: LocalDate): Future[EventOccurrenceList] =
      if (shouldQuerySkillsforge(member)) {
        Future {
          // For testing
          config.hardcodedUserId.foreach { id =>
            logger.info(s"Fetching Skillsforge data for hardcoded ID $id (ignoring current user ${member.userId})")
          }
          val userId = config.hardcodedUserId.getOrElse(member.userId)
          val end = endInclusive.plusDays(1) // Skillsforge is exclusive, we're inclusive

          if (logger.isDebugEnabled) {
            logger.debug(s"Fetching new Skillsforge data for $userId - $start to $end inclusive")
          }

          val req = RequestBuilder.get(s"${config.baseUri}/$userId")
            .addHeader("X-Auth-Token", config.authToken)
            .addParameter("bookingsAfter", ISODateTimeFormat.date().print(start))
            .addParameter("bookingsBefore", ISODateTimeFormat.date().print(end))
            .build()

          try {

            val (statusLine: StatusLine, bodyString: String) = httpClient.execute(req, (res: HttpResponse) => {
              (res.getStatusLine, EntityUtils.toString(res.getEntity, Charsets.UTF_8))
            })

            if (statusLine.getStatusCode == HttpStatus.UNAUTHORIZED.value) {
              // This seems more like we definitely got the token wrong, so always throw here
              throw new RuntimeException(s"Unauthorized response: ${statusLine.getReasonPhrase}\n$bodyString")
            } else {
              val data: JsObject = Json.parse(bodyString).as[JsObject]

              val occurrences: Seq[EventOccurrence] = if ((data \ "success").as[Boolean]) {
                (data \ "data").as[Seq[JsObject]].map(Skillsforge.toEventOccurrence)
              } else {
                val errorMessage = (data \ "errorMessage").asOpt[String].getOrElse(s"Unknown error from Skillsforge: $data")
                /*
                  The API will return this error if a user doesn't exist:

                    {"success":false,"data":null,"errorMessage":"Could not retrieve users bookings - EMBD.GBRFU:1.3: Could not complete operation: Insufficient privileges."}

                   However, it will also return this exact same error if you don't have permission (such as when the auth token is incorrect).
                   I did query this but the developers said this is just how it works. (Though I've also seen it return an HTML page and a 401
                   with this same error, which is now also handled above.)

                   I've made a flag which will be set true on one of the instances, so hopefully if there are problems with the auth token we will
                   see them there without bogging down the logs elsewhere.
                */
                if (errorMessage.contains("Insufficient privileges") && !config.reportAuthErrors) {
                  // Now that we are limiting this request just to users we actually expect to be in Skillsforge, we are
                  // logging at WARN level so it's more visible.
                  logger.warn(s"Ignoring error for Usercode($userId) because it is probably just a nonexistent user: $errorMessage")
                  Nil
                } else {
                  throw new RuntimeException(s"Skillsforge error: $errorMessage")
                }
              }

              EventOccurrenceList(occurrences, Some(new DateTime()))
            }
          } catch {
            case NonFatal(e) =>
              throw new RequestFailedException("The Skillsforge service could not be reached", e)
          }
        }
      } else {
        Future.successful(EventOccurrenceList.empty)
      }

  }

}


case class SkillsforgeConfiguration(
  baseUri: String,
  authToken: String,
  hardcodedUserId: Option[String],
  reportAuthErrors: Boolean,
)

trait SkillsForgeConfigurationComponent {
  def config: SkillsforgeConfiguration
}

trait AutowiringSkillsforgeConfigurationComponent extends SkillsForgeConfigurationComponent {
  lazy val config = SkillsforgeConfiguration(
    baseUri = Wire.property("${skillsforge.base.url}"),
    authToken = Wire.property("${skillsforge.authToken}"),
    hardcodedUserId = Wire.optionProperty("${skillsforge.hardcodedUserId}").filter(_.hasText),
    reportAuthErrors = Wire.property("${skillsforge.reportAuthErrors}").toBoolean,
  )
}

object Skillsforge {

  // Joda can parse the incoming dates with its default parser, but when this is converted
  // to Java Time it will likely need a custom pattern to handle the +HHMM zone offset at the end,
  // which it doesn't seem able to handle by default.
  implicit val dateTimeReads: Reads[DateTime] = JodaReads.DefaultJodaDateTimeReads

  def toEventOccurrence(obj: JsObject): EventOccurrence = {
    val startDate = (obj \ "eventStartIsoDate").as[DateTime].toLocalDateTime
    val endDate = (obj \ "eventEndIsoDate").as[DateTime].toLocalDateTime

    EventOccurrence(
      uid = (obj \ "bookingId").as[String],
      name = "Skillsforge event",
      title = (obj \ "eventTitle").as[String],
      description = "",
      eventType = TimetableEventType.Other("Skillsforge"),
      start = startDate,
      end = endDate,
      location = (obj \ "venue").asOpt[JsObject].map(toLocation),
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      parent = Parent(),
      comments = None,
      staff = Nil,
      relatedUrl = None,
      attendance = None
    )
  }

  def toLocation(obj: JsObject): Location = NamedLocation(
    (obj \ "detailedLocation").asOpt[String].getOrElse {
      (obj \ "displayString").as[String]
    }
  )


}

