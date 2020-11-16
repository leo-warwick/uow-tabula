package uk.ac.warwick.tabula.services.timetables

import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.RequestBuilder
import org.joda.time.{DateTimeConstants, LocalTime}
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange, WeekRangeListUserType}
import uk.ac.warwick.tabula.data.model.{MapLocation, StudentMember}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, FeaturesComponent, ScalaFactoryBean}

import scala.concurrent.Future
import scala.xml.Elem

trait ScientiaConfiguration {
  val perYearUris: Seq[(String, AcademicYear)]
  val cacheSuffix: String
  val cacheExpiryTime: Int
  val returnEvents: Boolean = true
}

trait ScientiaConfigurationComponent {
  val scientiaConfiguration: ScientiaConfiguration
}

class ScientiaConfigurationImpl extends ScientiaConfiguration {
  self: ClockComponent with FeaturesComponent =>

  def scientiaFormat(year: AcademicYear): String = {
    // e.g. 1314
    (year.startYear % 100).toString + (year.endYear % 100).toString
  }

  lazy val scientiaBaseUrl: String = Wire.optionProperty("${scientia.base.url}").getOrElse("https://test-timetablingmanagement.warwick.ac.uk/xml")

  // TAB-6462- Current academic year is enabled on 1st Aug but timetabling feed becomes active later in Sept so we can disable in Aug to stop 404 errors
  lazy val currentAcademicYear: Option[AcademicYear] = {
    if (features.timetableFeedCurrentAcademicYear) {
      Some(AcademicYear.forDate(clock.now).extended)
    } else
      None
  }

  lazy val prevAcademicYear: Option[AcademicYear] = {
    // TAB-3074 we only fetch the previous academic year if the month is >= AUGUST and < OCTOBER
    val month = clock.now.getMonthOfYear
    if (month >= DateTimeConstants.AUGUST && month < DateTimeConstants.OCTOBER)
      Some(AcademicYear.forDate(clock.now).extended).map(_.previous.extended)
    else
      None
  }

  def yearProperty: Option[Seq[AcademicYear]] =
    Wire.optionProperty("${scientia.years}").map(_.split(",").map(AcademicYear.parse))

  lazy val academicYears: Seq[AcademicYear] = yearProperty.getOrElse {
    Seq(prevAcademicYear, currentAcademicYear).flatten
  }

  lazy val perYearUris: Seq[(String, AcademicYear)] = academicYears.map { year => (scientiaBaseUrl + scientiaFormat(year) + "/", year) }

  lazy val cacheSuffix: String = Wire.optionProperty("${scientia.cacheSuffix}").getOrElse("")

  override val cacheExpiryTime: Int = 60 * 60 // 1 hour in seconds
}

trait AutowiringScientiaConfigurationComponent extends ScientiaConfigurationComponent {
  val scientiaConfiguration = new ScientiaConfigurationImpl with SystemClockComponent with AutowiringFeaturesComponent
}

private class ScientiaHttpTimetableFetchingService(scientiaConfiguration: ScientiaConfiguration) extends CompleteTimetableFetchingService with Logging {
  self: LocationFetchingServiceComponent
    with SmallGroupServiceComponent
    with ModuleAndDepartmentServiceComponent
    with UserLookupComponent
    with ProfileServiceComponent
    with SyllabusPlusLocationServiceComponent
    with ApacheHttpClientComponent =>

  lazy val perYearUris: Seq[(String, AcademicYear)] = scientiaConfiguration.perYearUris

  lazy val studentUris: Seq[(String, AcademicYear)] = perYearUris.map {
    case (uri, year) => (uri + "?StudentXML", year)
  }
  lazy val staffUris: Seq[(String, AcademicYear)] = perYearUris.map {
    case (uri, year) => (uri + "?StaffXML", year)
  }
  lazy val courseUris: Seq[(String, AcademicYear)] = perYearUris.map {
    case (uri, year) => (uri + "?CourseXML", year)
  }
  lazy val moduleNoStudentsUris: Seq[(String, AcademicYear)] = perYearUris.map {
    case (uri, year) => (uri + "?ModuleNoStudentsXML", year)
  }
  lazy val moduleWithSudentsUris: Seq[(String, AcademicYear)] = perYearUris.map {
    case (uri, year) => (uri + "?ModuleXML", year)
  }
  lazy val roomUris: Seq[(String, AcademicYear)] = perYearUris.map {
    case (uri, year) => (uri + "?RoomXML", year)
  }

  // an HTTPClient response handler which reads XML from the response and parses it into a list of TimetableEvents
  // the timetable response doesn't include its year, so we pass that in separately.
  def handler(year: AcademicYear, excludeSmallGroupEventsInTabula: Boolean = false, uniId: String): ResponseHandler[Seq[TimetableEvent]] =
    ApacheHttpClientUtils.xmlResponseHandler { node =>
      parseXml(node, year, uniId, locationFetchingService, moduleAndDepartmentService, userLookup)
    }

  private def hasSmallGroups(moduleCode: Option[String], year: AcademicYear) =
    moduleCode.flatMap(moduleAndDepartmentService.getModuleByCode).fold(false) { module =>
      !smallGroupService.getSmallGroupSets(module, year).forall(_.archived)
    }

  def getTimetableForStudent(universityId: String): Future[EventList] = doRequest(studentUris, universityId, excludeSmallGroupEventsInTabula = true)

  def getTimetableForModule(moduleCode: String, includeStudents: Boolean): Future[EventList] = {
    if (includeStudents) doRequest(moduleWithSudentsUris, moduleCode)
    else doRequest(moduleNoStudentsUris, moduleCode)
  }

  def getTimetableForCourse(courseCode: String): Future[EventList] = doRequest(courseUris, courseCode)

  def getTimetableForRoom(roomName: String): Future[EventList] = doRequest(roomUris, roomName)

  def getTimetableForStaff(universityId: String): Future[EventList] = doRequest(
    staffUris,
    universityId,
    excludeSmallGroupEventsInTabula = true,
    excludeEventTypes = Seq(TimetableEventType.Seminar, TimetableEventType.Practical)
  )

  def doRequest(
    uris: Seq[(String, AcademicYear)],
    param: String,
    excludeSmallGroupEventsInTabula: Boolean = false,
    excludeEventTypes: Seq[TimetableEventType] = Seq()
  ): Future[EventList] = {
    // fetch the events from each of the supplied URIs, and flatmap them to make one big list of events
    val results: Seq[Future[EventList]] = uris.map { case (uri, year) =>
      // add ?p0={param} to the URL's get parameters
      val req =
        RequestBuilder.get(uri)
          .addParameter("p0", param)
          .build()

      // execute the request.
      // If the status is OK, pass the response to the handler function for turning into TimetableEvents
      // else return an empty list.
      logger.info(s"Requesting timetable data from ${req.getURI.toString}")

      val result = Future {
        val ev = httpClient.execute(req, handler(year, excludeSmallGroupEventsInTabula, param))

        if (ev.isEmpty) {
          logger.info(s"Timetable request successful but no events returned: ${req.getURI.toString}")
        }

        ev
      }

      // Some extra logging here
      result.failed.foreach { e =>
        logger.warn(s"Request for ${req.getURI.toString} failed: ${e.getMessage}")
      }

      result.map { events =>
        if (excludeSmallGroupEventsInTabula)
          EventList.fresh(events.filterNot { event =>
            event.eventType == TimetableEventType.Seminar &&
              hasSmallGroups(event.parent.shortName, year)
          })
        else EventList.fresh(events)
      }.map(events => events.filterNot(e => excludeEventTypes.contains(e.eventType)))
    }

    Futures.combine(results, EventList.combine).map(eventsList =>
      if (!scientiaConfiguration.returnEvents) {
        EventList.empty
      } else if (eventsList.events.isEmpty) {
        logger.info(s"All timetable years are empty for $param")

        val studentCourseEndedInThePast: Boolean = profileService.getMemberByUniversityId(param)
          .flatMap {
            case student: StudentMember =>
              val endYears = student.freshOrStaleStudentCourseDetails.map(_.latestStudentCourseYearDetails.academicYear.endYear)
              if (endYears.isEmpty) None else Some(endYears.max)
            case _ => None
          }.exists(_ < AcademicYear.now().startYear)

        if (studentCourseEndedInThePast) {
          // TAB-6421 do not throw exception when student with a course end date in the past
          EventList.empty
        } else {
          throw new TimetableEmptyException(uris, param)
        }
      } else {
        eventsList
      }
    )
  }

  def parseXml(
    xml: Elem,
    year: AcademicYear,
    uniId: String,
    locationFetchingService: LocationFetchingService,
    moduleAndDepartmentService: ModuleAndDepartmentService,
    userLookup: UserLookupService
  ): Seq[TimetableEvent] = {
    val moduleCodes = (xml \\ "module").map(_.text.toLowerCase).distinct
    if (moduleCodes.isEmpty) logger.info(s"No modules returned for: $uniId")
    val moduleMap = moduleAndDepartmentService.getModulesByCodes(moduleCodes).groupBy(_.code).mapValues(_.head)
    xml \\ "Activity" map { activity =>
      val name = (activity \\ "name").text

      val initialStartTime = new LocalTime((activity \\ "start").text)
      val initialEndTime = new LocalTime((activity \\ "end").text)

      //TAB-8848 -Some 20/21 S+ events have been set with start time  > end time(23pm-0am slots) but they belong to 8-9 am slot.
      val (startTime, endTime) = if (initialStartTime.getHourOfDay == 23) {
        logger.info(s"Changed start/end time to 8-9 am slot for S+ invalid event: $name, startTime:$initialStartTime: endTime:$initialEndTime")
        (initialStartTime.withHourOfDay(8), initialEndTime.withHourOfDay(9))
      } else (initialStartTime, initialEndTime)


      val location = (activity \\ "room").headOption.map(_.text) match {
        case Some(text) if !text.isEmpty =>
          // try and get the location from the map of managed rooms without calling the api. fall back to searching for this room
          syllabusPlusLocationService.getByUpstreamName(text).map(_.asMapLocation)
            .orElse({
              // S+ has some (not all) rooms as "AB_AB1.2", where AB is a building code
              // we're generally better off without this when searching.
              val removeBuildingNames = "^[^_]*_".r
              Some(locationFetchingService.locationFor(removeBuildingNames.replaceFirstIn(text, "")))
            })
        case _ => None
      }

      val parent = TimetableEvent.Parent(moduleMap.get((activity \\ "module").text.toLowerCase))

      val dayOfWeek = DayOfWeek.apply((activity \\ "day").text.toInt + 1)

      val uid =
        DigestUtils.md5Hex(
          Seq(
            name,
            startTime.toString,
            endTime.toString,
            dayOfWeek.toString,
            location.map(_.name).getOrElse(""),
            parent.shortName.getOrElse(""),
            (activity \\ "weeks").text
          ).mkString
        )

      TimetableEvent(
        uid = uid,
        name = name,
        title = (activity \\ "title").text,
        description = (activity \\ "description").text,
        eventType = TimetableEventType((activity \\ "type").text),
        weekRanges = new WeekRangeListUserType().convertToObject((activity \\ "weeks").text),
        day = dayOfWeek,
        startTime = startTime,
        endTime = endTime,
        location = location,
        comments = Option((activity \\ "comments").text).flatMap(_.maybeText),
        parent = parent,
        staff = userLookup.usersByWarwickUniIds((activity \\ "staffmember").map(_.text)).values.collect { case FoundUser(u) => u }.toSeq,
        students = userLookup.usersByWarwickUniIds((activity \\ "student").map(_.text)).values.collect { case FoundUser(u) => u }.toSeq,
        year = year,
        relatedUrl = None,
        attendance = Map()
      )
    }
  }
}

class TimetableEmptyException(val uris: Seq[(String, AcademicYear)], val param: String)
  extends IllegalStateException(s"Received empty timetables for $param using: ${uris.map { case (uri, _) => uri }.mkString(", ")}")

object ScientiaHttpTimetableFetchingService {
  val cacheName = "SyllabusPlusTimetableLists"

  def apply(scientiaConfiguration: ScientiaConfiguration): CompleteTimetableFetchingService = {
    val service =
      new ScientiaHttpTimetableFetchingService(scientiaConfiguration)
        with WAI2GoHttpLocationFetchingServiceComponent
        with AutowiringSmallGroupServiceComponent
        with AutowiringModuleAndDepartmentServiceComponent
        with AutowiringWAI2GoConfigurationComponent
        with AutowiringUserLookupComponent
        with AutowiringApacheHttpClientComponent
        with AutowiringProfileServiceComponent
        with AutowiringSyllabusPlusLocationServiceComponent

    if (scientiaConfiguration.perYearUris.exists(_._1.contains("stubTimetable"))) {
      // don't cache if we're using the test stub - otherwise we won't see updates that the test setup makes
      service
    } else {
      new CachedCompleteTimetableFetchingService(service, s"$cacheName${scientiaConfiguration.cacheSuffix}", scientiaConfiguration.cacheExpiryTime)
    }
  }
}

trait ScientiaTimetableFetchingServiceComponent extends CompleteTimetableFetchingServiceComponent

@Profile(Array("dev", "test", "production"))
@Service("scientiaHttpTimetableFetchingService")
class ScientiaHttpTimetableFetchingServiceFactory extends ScalaFactoryBean[CompleteTimetableFetchingService] with AutowiringScientiaConfigurationComponent {
  override def createInstance(): CompleteTimetableFetchingService =
    ScientiaHttpTimetableFetchingService(scientiaConfiguration)
}

@Service("scientiaTimetableFetchingService")
class ScientiaTimetableFetchingService extends ScalaFactoryBean[CompleteTimetableFetchingService] {
  @Qualifier("scientiaHttpTimetableFetchingService")
  @Autowired
  var scientiaHttpTimetableFetchingService: CompleteTimetableFetchingService = _

  override def createInstance(): CompleteTimetableFetchingService =
    new CombinedTimetableFetchingService(scientiaHttpTimetableFetchingService)
}

trait ScientiaHttpTimetableFetchingServiceComponent {
  def scientiaHttpTimetableFetchingService: CompleteTimetableFetchingService
}

trait AutowiringScientiaHttpTimetableFetchingServiceComponent extends ScientiaHttpTimetableFetchingServiceComponent {
  val scientiaHttpTimetableFetchingService: CompleteTimetableFetchingService = Wire.named[CompleteTimetableFetchingService]("scientiaHttpTimetableFetchingService")
}

@Profile(Array("sandbox"))
@Service("scientiaHttpTimetableFetchingService")
class SandboxScientiaHttpTimetableFetchingService extends CompleteTimetableFetchingService
  with AutowiringProfileServiceComponent
  with AutowiringModuleAndDepartmentServiceComponent {

  override def getTimetableForStudent(universityId: String): Future[EventList] = {
    profileService.getMemberByUniversityId(universityId).collect { case student: StudentMember => student }.map { student =>
      Future.sequence(
        student.mostSignificantCourse.latestStudentCourseYearDetails.moduleRegistrations
          .map { mr => getTimetableForModule(mr.module.code, includeStudents = false) }
      ).map(EventList.combine)
    }.getOrElse(Future.successful(EventList.empty))
  }

  override def getTimetableForStaff(universityId: String): Future[EventList] = Future.successful(EventList.empty)

  override def getTimetableForModule(moduleCode: String, includeStudents: Boolean): Future[EventList] = {
    moduleAndDepartmentService.getModuleByCode(moduleCode).map { module =>
      Future.successful(EventList.fresh((0 until 2).map { i =>
        val lastDigit = (7 * module.code.charAt(3).asDigit) + module.code.charAt(5).asDigit + (i * 9)
        val name = s"${moduleCode.toUpperCase}L"

        val startTime = new LocalTime(9, 0).plusHours(lastDigit % 9)
        val endTime = startTime.plusHours(1).plusHours(lastDigit % 2)

        val dayOfWeek = DayOfWeek((lastDigit % 5) + 1)

        val location = lastDigit % 10 match {
          case 0 => Some(MapLocation("A0.08A", "52953"))
          case 1 => Some(MapLocation("H2.03", "21518"))
          case 2 => Some(MapLocation("MOAC seminar room", "38566"))
          case 3 => Some(MapLocation("H1.03", "21430"))
          case 4 => Some(MapLocation("S2.86", "37733"))
          case 5 => Some(MapLocation("Westwood Lecture Theatre", "38990"))
          case 6 => Some(MapLocation("L3", "44845"))
          case 7 => Some(MapLocation("OC0.05", "52131"))
          case 8 => Some(MapLocation("L4", "31390"))
          case 9 => Some(MapLocation("MA2.03", "33347"))
        }

        val parent = TimetableEvent.Parent(Some(module))
        val weeks = lastDigit % 5 match {
          case 0 => Seq(WeekRange(0, 10))
          case 1 => Seq(WeekRange(15, 20))
          case 2 => Seq(WeekRange(15, 24))
          case 3 => Seq(WeekRange(0, 5), WeekRange(7, 10))
          case 4 => Seq(WeekRange(15, 20), WeekRange(22), WeekRange(24))
        }

        val uid =
          DigestUtils.md5Hex(
            Seq(
              name,
              startTime.toString,
              endTime.toString,
              dayOfWeek.toString,
              location.map(_.name).getOrElse(""),
              parent.shortName.getOrElse(""),
              weeks.mkString("")
            ).mkString
          )

        TimetableEvent(
          uid = uid,
          name = name,
          title = "",
          description = "",
          eventType = TimetableEventType.Lecture,
          weekRanges = weeks,
          day = dayOfWeek,
          startTime = startTime,
          endTime = endTime,
          location = location,
          comments = None,
          parent = parent,
          staff = Nil,
          students = Nil,
          year = AcademicYear.now(),
          relatedUrl = None,
          attendance = Map()
        )
      }))
    }.getOrElse(Future.successful(EventList.empty))
  }

  override def getTimetableForCourse(courseCode: String): Future[EventList] = Future.successful(EventList.empty)

  override def getTimetableForRoom(roomName: String): Future[EventList] = Future.successful(EventList.empty)
}

trait AutowiringScientiaTimetableFetchingServiceComponent extends ScientiaTimetableFetchingServiceComponent {
  val timetableFetchingService: CompleteTimetableFetchingService = Wire.named[CompleteTimetableFetchingService]("scientiaTimetableFetchingService")
}
