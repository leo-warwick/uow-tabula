package uk.ac.warwick.tabula.services.timetables

import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import play.api.libs.json.{JsNull, JsObject, JsString, Json}
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.NamedLocation
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEventType}

class SkillsforgeTest extends TestBase {

  private val testEvent = Json.parse(
    """
        {
            "bookingId": "00892418-5dbe-4c4a-962a-8349c1c1c147",
            "sessionId": "39f9e1ac-4867-4e82-8f70-3832bac406c2",
            "event": {
                "id": "ab4847a7-9713-4997-872d-aebced38aee6",
                "provider": {
                    "id": "a5355dd3-d7af-4c43-b710-23378c92fe97",
                    "orgAlias": "warwick",
                    "code": "RSSP",
                    "displayString": "RSSP"
                },
                "number": "-1",
                "eventCode": "RSSP-1",
                "displayString": "RSSP-1"
            },
            "eventTitle": "Effective Researcher",
            "eventStartDate": 1550667600000,
            "eventEndDate": 1550682000000,
            "eventStartIsoDate": "2019-02-20T13:00:00.000+0000",
            "eventEndIsoDate": "2019-02-20T17:00:00.000+0000",
            "bookingStatus": "BOOKED",
            "facilitators": [
                {
                    "id": "59205da7-a6fc-46da-aff9-a7eb3eadc1d3",
                    "orgAlias": "warwick",
                    "name": "Michele Underwood",
                    "email": "m.underwood.2@warwick.ac.uk",
                    "phoneNumber": null,
                    "accountId": null,
                    "displayString": "Michele Underwood"
                }
            ],
            "venue": {
                "id": "3816fcba-8c48-4ac9-b6f1-b79864e3ddf2",
                "orgAlias": "warwick",
                "generalLocation": "Wolfson Research Exchange, Library, Main Campus",
                "detailedLocation": "Seminar Room 1, Wolfson RE",
                "directions": "Library Road",
                "capacityLimit": 30,
                "displayString": "Seminar Room 1, Wolfson RE, Wolfson Research Exchange, Library, Main Campus"
            },
            "createdDate": 1549287560140,
            "lastModifiedDate": 1549287560140
        }
        """).as[JsObject]


  @Test
  def readsDate(): Unit = {
    val input = "2019-06-20T13:00:00.000+0000"
    // although the zone offet is correctly honoured, the resulting DateTime
    // is in the system default zone. This is sort of reasonable as there's no
    // reliable way to get from zone offset to zone ID.
    val now = DateTime.parse(input).withZone(DateTimeZone.getDefault)
    val datetime = Skillsforge.dateTimeReads.reads(JsString(input)).get
    assert(datetime === now)
  }

  @Test
  def toEventOccurrence(): Unit = {
    val output = Skillsforge.toEventOccurrence(testEvent)
    assert(output === EventOccurrence(
      uid = "00892418-5dbe-4c4a-962a-8349c1c1c147",
      name = "Skillsforge event",
      title = "Effective Researcher",
      description = "",
      eventType = TimetableEventType.Other("Skillsforge"),
      start = LocalDateTime.parse("2019-02-20T13:00:00.000"),
      end = LocalDateTime.parse("2019-02-20T17:00:00.000"),
      location = Some(NamedLocation("Seminar Room 1, Wolfson RE")),
      onlineDeliveryUrl = None,
      deliveryMethod = None,
      parent = Parent(),
      comments = None,
      staff = Nil,
      relatedUrl = None,
      attendance = None,
      sgtGroupId = None,
      sgtGroupEventId = None,
      week = None
    ))
  }

  @Test
  def toEventOccurrenceNullLocation(): Unit = {
    val input = testEvent - "venue" ++ Json.obj("venue" -> JsNull)
    val output = Skillsforge.toEventOccurrence(input)
    output.location shouldBe None
  }

  @Test
  def toEventOccurrenceNoLocation(): Unit = {
    val input = testEvent - "venue"
    val output = Skillsforge.toEventOccurrence(input)
    output.location shouldBe None
  }

}
