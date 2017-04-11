package uk.ac.warwick.tabula.api.web.controllers.timetables

import net.fortuna.ical4j.data.CalendarOutputter
import org.joda.time.DateTime
import org.springframework.mock.web.MockHttpServletResponse
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.timetables.ViewMemberEventsRequest
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.timetables.EventOccurrence
import uk.ac.warwick.tabula.{Fixtures, AcademicYear, Mockito, TestBase}
import uk.ac.warwick.tabula.services.{TermService, TermServiceComponent}
import uk.ac.warwick.tabula.services.timetables.{EventOccurrenceService, EventOccurrenceServiceComponent}
import uk.ac.warwick.util.termdates.Term.TermType
import uk.ac.warwick.util.termdates.TermImpl

import scala.util.{Success, Try}

class GeneratesTimetableIcalFeedTest extends TestBase with Mockito {

	val generator = new GeneratesTimetableIcalFeed with TermServiceComponent with EventOccurrenceServiceComponent {
		val termService: TermService = smartMock[TermService]
		val eventOccurrenceService: EventOccurrenceService = mock[EventOccurrenceService]
	}

	@Test
	def emptyTimetableValidates() {
		val ical = generator.getIcalFeed(Seq(), Fixtures.staff("1234"))

		new CalendarOutputter().output(ical, new MockHttpServletResponse().getWriter)
		// doesn't throw an exception
	}

}
