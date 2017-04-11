package uk.ac.warwick.tabula.services.timetables

import dispatch.classic.{Handler, Request, url}
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, PeriodFormatter, PeriodFormatterBuilder}
import org.joda.time.{DateTime, Period}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.trusted.TrustedApplicationUtils
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, CurrentUser, FeaturesComponent}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.Try
import scala.xml.Elem

trait ExamTimetableConfiguration {
	val examTimetableUrl: String
}

trait ExamTimetableConfigurationComponent {
	val examTimetableConfiguration: ExamTimetableConfiguration
}

trait AutowiringExamTimetableConfigurationComponent extends ExamTimetableConfigurationComponent {
	val examTimetableConfiguration = new AutowiringExamTimetableConfiguration

	class AutowiringExamTimetableConfiguration extends ExamTimetableConfiguration {
		lazy val examTimetableUrl: String = Wire.optionProperty("${examTimetable.url}").getOrElse("https://exams.warwick.ac.uk/timetable/")
	}

}

trait ExamTimetableHttpTimetableFetchingServiceComponent extends StaffAndStudentTimetableFetchingServiceComponent {
	self: ExamTimetableConfigurationComponent =>

	lazy val timetableFetchingService = ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
}

private class ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration: ExamTimetableConfiguration)
	extends StaffTimetableFetchingService with StudentTimetableFetchingService with Logging {

	self: DispatchHttpClientComponent with TrustedApplicationsManagerComponent
		with UserLookupComponent with TermServiceComponent with FeaturesComponent =>

	// a dispatch response handler which reads XML from the response and parses it into a list of TimetableEvents
	def handler(uniId: String): (Map[String, Seq[String]], Request) => Handler[Seq[TimetableEvent]] = { (_: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req <> { node =>
			ExamTimetableHttpTimetableFetchingService.parseXml(node, uniId, termService)
		}
	}

	private def featureProtected(arg: String)(f: (String) => Future[EventList]): Future[EventList] = {
		if (features.personalExamTimetables) {
			f(arg)
		} else {
			Future(EventList(Nil, None))
		}
	}


	override def getTimetableForStudent(universityId: String): Future[EventList] = featureProtected(universityId) { doRequest }
	override def getTimetableForStaff(universityId: String): Future[EventList] = featureProtected(universityId) { doRequest }

	def doRequest(param: String): Future[EventList] = {
		userLookup.getUserByWarwickUniId(param) match {
			case user if !user.isFoundUser =>
				logger.warn(s"Tried to get exam timetable for $param but could find no such user")
				Future(EventList(Nil, None))
			case user =>
				val endpoint = s"${examTimetableConfiguration.examTimetableUrl}$param.xml"

				val trustedAppHeaders = TrustedApplicationUtils.getRequestHeaders(
					applicationManager.getCurrentApplication,
					user.getUserId,
					endpoint
				).asScala.map { header => header.getName -> header.getValue }.toMap

				val req = url(endpoint) <:< trustedAppHeaders

				logger.info(s"Requesting exam timetable data from ${req.to_uri.toString}")

				val result = Future {
					httpClient.when(_ == 200)(req >:+ handler(param))
				}
				result.onFailure { case e =>
					logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
				}
				result.map(EventList.fresh)
		}
	}

}

object ExamTimetableHttpTimetableFetchingService extends Logging {

	val cacheName = "ExamTimetableLists"

	def apply(examTimetableConfiguration: ExamTimetableConfiguration): CachedStaffAndStudentTimetableFetchingService = {
		val service =	new ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
			with AutowiringDispatchHttpClientComponent
			with AutowiringTrustedApplicationsManagerComponent
			with AutowiringUserLookupComponent
			with AutowiringTermServiceComponent
			with AutowiringFeaturesComponent

		new CachedStaffAndStudentTimetableFetchingService(service, cacheName)
	}

	def parseXml(xml: Elem, universityId: String, termService: TermService): Seq[TimetableEvent] = {
		val timetable = ExamTimetableFetchingService.examTimetableFromXml(xml, termService)
		val examProfile = (xml \\ "exam-profile" \\ "profile").text

		timetable.exams.map(timetableExam => {
			val uid = DigestUtils.md5Hex(Seq(examProfile, timetableExam.paper).mkString)
			TimetableEvent(
				uid = uid,
				name = "Exam",
				title = "Exam",
				description = "",
				eventType = TimetableEventType.Exam,
				weekRanges = Seq(WeekRange(
					termService.getTermFromDateIncludingVacations(timetableExam.startDateTime)
						.getAcademicWeekNumber(timetableExam.startDateTime)
				)),
				day = DayOfWeek(timetableExam.startDateTime.dayOfWeek.get),
				startTime = timetableExam.startDateTime.toLocalTime,
				endTime = timetableExam.endDateTime.toLocalTime,
				location = None,
				comments = Some("More information available on the <a href=\"%s\">exam timetable</a>".format(Routes.Profile.examTimetable(universityId))),
				parent = TimetableEvent.Parent(),
				staff = Nil,
				students = Nil,
				year = timetableExam.academicYear,
				relatedUrl = None,
				attendance = Map()
			)
		})
	}
}

object ExamTimetableFetchingService {

	val examDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("dd MMM HH:mm")
	val examPeriodFormatter: PeriodFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix("hr ").appendMinutes().appendSuffix("mins").toFormatter
	val examReadingTimeFormatter: PeriodFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix(":").appendMinutes().appendSuffix(":").appendSeconds().toFormatter

	case class ExamTimetableExam(
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
		room: String
	)

	case class ExamTimetable(
		header: String,
		instructions: String,
		exams: Seq[ExamTimetableExam]
	)

	def examTimetableFromXml(xml: Elem, termService: TermService): ExamTimetable = {
		val header = (xml \\ "exam-headerinfo").text
		val instructions = (xml \\ "exam-instructions").text
		val academicYear = AcademicYear((xml \\ "academic-year").text.toInt)
		val exams = (xml \\ "exam").map(examNode => {
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

			ExamTimetableExam(
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
				room
			)
		})
		ExamTimetable(header, instructions, exams)
	}

}

trait ExamTimetableFetchingService {
	def getTimetable(member: Member, viewer: CurrentUser): Future[ExamTimetableFetchingService.ExamTimetable]
}

abstract class AbstractExamTimetableFetchingService extends ExamTimetableFetchingService with Logging {

	self: DispatchHttpClientComponent with ExamTimetableConfigurationComponent
		with TrustedApplicationsManagerComponent with TermServiceComponent =>

	def getTimetable(member: Member, viewer: CurrentUser): Future[ExamTimetableFetchingService.ExamTimetable] = {
		val endpoint = s"${examTimetableConfiguration.examTimetableUrl}${member.universityId}.xml"

		val trustedAppHeaders = TrustedApplicationUtils.getRequestHeaders(
			applicationManager.getCurrentApplication,
			viewer.userId,
			endpoint
		).asScala.map { header => header.getName -> header.getValue }.toMap

		val req = url(endpoint) <:< trustedAppHeaders

		logger.info(s"Requesting exam timetable data from ${req.to_uri.toString}")

		// a dispatch response handler which reads XML from the response and parses it into an ExamTimetable
		def handler = { (_: Map[String,Seq[String]], req: dispatch.classic.Request) =>
			req <> { node =>
				ExamTimetableFetchingService.examTimetableFromXml(node, termService)
			}
		}

		val result = Future {
			httpClient.when(_ == 200)(req >:+ handler)
		}
		result.onFailure { case e =>
			logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
		}
		result
	}
}

@Service("examTimetableFetchingService")
class ExamTimetableFetchingServiceImpl
	extends AbstractExamTimetableFetchingService
	with AutowiringDispatchHttpClientComponent
	with AutowiringExamTimetableConfigurationComponent
	with AutowiringTrustedApplicationsManagerComponent
	with AutowiringUserLookupComponent
	with AutowiringTermServiceComponent

trait ExamTimetableFetchingServiceComponent {
	def examTimetableFetchingService: ExamTimetableFetchingService
}


trait AutowiringExamTimetableFetchingServiceComponent extends ExamTimetableFetchingServiceComponent {
	var examTimetableFetchingService: ExamTimetableFetchingService = Wire[ExamTimetableFetchingService]
}