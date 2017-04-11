package uk.ac.warwick.tabula.profiles.profile

import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.web.FixturesDriver
import uk.ac.warwick.tabula.{AcademicDateHelper, FunctionalTestAcademicYear, FunctionalTestProperties, LoginDetails}
import uk.ac.warwick.util.termdates.TermFactoryImpl

import scala.language.postfixOps
import scala.util.parsing.json.JSON
import scala.xml.Elem

trait TimetableDriver extends FixturesDriver  {

	def setTimetableFor(userId:String, year:FunctionalTestAcademicYear, content:Elem) {
		val uri = FunctionalTestProperties.SiteRoot + "/stubTimetable/student"
		val req = url(uri).POST << Map("studentId" -> userId, "year"->year.toSyllabusPlusFormat, "content"->content.toString)
		http.when(_==200)(req >| )
	}

	def requestWholeYearsTimetableFeedFor(user:LoginDetails, asUser:Option[LoginDetails]=None ):Seq[Map[String,Any]]={
		val http: Http = new Http with thread.Safety {
			override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
				getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
			}
		}
		try {
			val requestor = asUser.getOrElse(user)
			// request all the events for the current year
			val termDates = new AcademicDateHelper(new TermFactoryImpl)
			val startOfYear = termDates.getFirstTermOfYearContaining(new DateTime).getStartDate
			val start = startOfYear.getMillis
			val end = startOfYear.plusYears(1).getMillis
			val req = (url(s"${FunctionalTestProperties.SiteRoot}/api/v1/member/${user.warwickId}/timetable/calendar") <<?
				Map(
					"from" -> start.toString,
					"to" -> end.toString,
					"whoFor" -> user.warwickId,
					"forceBasic" -> "true"
				)
				).as_!(requestor.usercode, requestor.password)

			import scala.language.postfixOps
			val rawJSON = http.x(req as_str)
			JSON.parseFull(rawJSON) match {
				case Some(json: Map[String, Any] @unchecked) => json.get("events") match {
					case Some(events: Seq[Map[String, Any]] @unchecked) => events
					case _ => throw new RuntimeException(s"Couldn't parse JSON into sequence\n $rawJSON")
				}
				case _ => throw new RuntimeException(s"Couldn't parse JSON into sequence\n $rawJSON")
			}
		} finally {
			http.shutdown()
		}

	}
}
