package uk.ac.warwick.tabula.api.web.controllers.timetables

import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.web.views.FullCalendarEvent
import uk.ac.warwick.tabula.{MockUserLookup, Mockito, TestBase}

class GetMemberCalendarJsonApiTest extends TestBase with Mockito {

  val api = new ApiController with GetMemberCalendarJsonApi with PathVariableMemberCalendarApi with UserLookupComponent {
    val userLookup = new MockUserLookup
  }

  @Test
  def colouriseBasedOnModule(): Unit = {
    // create a bunch events spread over 3 modules, with blank colours
    val events = for {
      i <- 1 until 21
      j = i % 3}
      yield {
        FullCalendarEvent(
          title = s"Test event $i",
          fullTitle = "",
          allDay = false,
          start = 0L,
          end = 0L,
          formattedStartTime = "",
          formattedEndTime = "",
          formattedInterval = "",
          syllabusPlusName = None,
          onlineDeliveryUrl = None,
          parentType = "Module",
          parentShortName = s"module$j",
          parentFullName = s"Module $j",
          relatedUrl = None
        )
      }

    // Add a Busy event at the end
    val coloured = FullCalendarEvent.colourEvents(events :+

      FullCalendarEvent(
        title = s"Busy",
        fullTitle = "",
        allDay = false,
        start = 0L,
        end = 0L,
        formattedStartTime = "",
        formattedEndTime = "",
        formattedInterval = "",
        syllabusPlusName = None,
        onlineDeliveryUrl = None,
        relatedUrl = None
      )
    )

    // every event should have a colour
    coloured.find(_.backgroundColor == "") should not be Symbol("defined")

    // there should be 4 colours in use
    coloured.map(_.backgroundColor).distinct.size should be(4)

    coloured.last.backgroundColor should be("#bbb")
  }

}
