package uk.ac.warwick.tabula.services.timetables

import org.apache.http.impl.client.CloseableHttpClient
import org.joda.time.LocalTime
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.data.model.{MapLocation, Module, SyllabusPlusLocation}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.userlookup.User

import scala.util.Success
import scala.xml.XML

class TimetableFetchingServiceTest extends TestBase with Mockito {

  val module: Module = Fixtures.module("cs132")

  @Test def parseXML(): Unit = {
    val userLookup = new MockUserLookup

    val tutor = new User("abcdef")
    tutor.setFoundUser(true)
    tutor.setWarwickId("1170047")

    val student = new User("student")
    student.setFoundUser(true)
    student.setWarwickId("1234567")

    userLookup.registerUserObjects(tutor, student)

    val service = new ScientiaHttpTimetableFetchingService(mock[ScientiaConfiguration])
      with LocationFetchingServiceComponent
      with SmallGroupServiceComponent
      with ModuleAndDepartmentServiceComponent
      with UserLookupComponent
      with ProfileServiceComponent
      with SyllabusPlusLocationServiceComponent
      with ApacheHttpClientComponent {
      val locationFetchingService: LocationFetchingService = (_: String) => Success(Nil)
      val smallGroupService: SmallGroupService = mock[SmallGroupService]
      val moduleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
      var userLookup: UserLookupService = mock[UserLookupService]
      val profileService: ProfileService = mock[ProfileService]
      val syllabusPlusLocationService: SyllabusPlusLocationService = mock[SyllabusPlusLocationService]
      val httpClient: CloseableHttpClient = mock[CloseableHttpClient]
    }

    service.moduleAndDepartmentService.getModulesByCodes(any[Seq[String]]) answers { codes: Any =>
      codes.asInstanceOf[Seq[String]].map(code => Fixtures.module(code))
    }

    service.syllabusPlusLocationService.getByUpstreamName(any[String]) returns None
    service.syllabusPlusLocationService.getByUpstreamName("L5") returns Some(SyllabusPlusLocation("L5", "Lecture Theatre 5", "31389"))
    service.syllabusPlusLocationService.getByUpstreamName("MS.02") returns Some(SyllabusPlusLocation("MS.02", "MS.02", "40879"))

    val events = service.parseXml(XML.loadString(TimetableEvents), AcademicYear(2012), student.getWarwickId, service.locationFetchingService, service.moduleAndDepartmentService, userLookup)
    events.size should be(10)
    events.head should be(TimetableEvent(
      uid = "625c69daa83eaaa6c12da006276eb947",
      name = "CS132L",
      title = "",
      description = "",
      startTime = new LocalTime(12, 0),
      endTime = new LocalTime(13, 0),
      weekRanges = Seq(WeekRange(6, 10)),
      day = DayOfWeek.Friday,
      eventType = TimetableEventType.Lecture,
      location = Some(MapLocation("Lecture Theatre 5", "31389", Some("L5"))),
      onlineDeliveryUrl = None,
      deliveryMethod= None,
      parent = TimetableEvent.Parent(Some(module)),
      comments = None,
      staff = Seq(tutor),
      students = Nil,
      year = AcademicYear(2012),
      relatedUrl = None,
      attendance = Map()
    ))
    // When faced with multiple rooms - snatch the first and run away
    events(1).location should be(Some(MapLocation("MS.02", "40879", Some("MS.02"))))
    events(1).comments should be(Some("Some comments"))
    events(1).students should be(Seq(student))
    events(1).eventType should be (TimetableEventType.Lecture)
  }

  val TimetableEvents =
    """<?xml version="1.0" encoding="UTF-8"?>
<Data>
   <Activities>
      <Activity>
         <name>CS132L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>6-10</weeks>
         <day>4</day>
         <type>LEC</type>
				 <comments></comments>
         <rooms>
            <room>L5</room>
         </rooms>
         <modules>
            <module>CS132</module>
         </modules>
         <staffmembers>
            <staffmember>1170047</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS133Ltest</name>
         <description />
         <start>09:00</start>
         <end>11:00</end>
         <weeks>31</weeks>
         <day>2</day>
         <type>LEC ALLOCATE IN TABULA</type>
         <comments>Some comments</comments>
         <rooms>
            <room>MS.02</room>
						<room>LIB4</room>
         </rooms>
         <modules>
            <module>CS133</module>
         </modules>
         <staffmembers>
            <staffmember>1170047</staffmember>
            <staffmember>8570237</staffmember>
         </staffmembers>
				 <students>
		 			  <student>1234567</student>
	 			 </students>
      </Activity>
      <Activity>
         <name>CS130L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>1-10</weeks>
         <day>3</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS130</module>
         </modules>
         <staffmembers>
            <staffmember>1170588</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS133L</name>
         <description />
         <start>14:00</start>
         <end>15:00</end>
         <weeks>1-10, 15-18, 20-24</weeks>
         <day>1</day>
         <type>LEC</type>
         <rooms>
            <room>H0.52</room>
         </rooms>
         <modules>
            <module>CS133</module>
         </modules>
         <staffmembers>
            <staffmember>1170047</staffmember>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS130L</name>
         <description />
         <start>14:00</start>
         <end>15:00</end>
         <weeks>1-10</weeks>
         <day>4</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS130</module>
         </modules>
         <staffmembers>
            <staffmember>1170588</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS126L</name>
         <description />
         <start>15:00</start>
         <end>16:00</end>
         <weeks>15-24</weeks>
         <day>1</day>
         <type>LEC</type>
         <rooms>
            <room>PLT</room>
         </rooms>
         <modules>
            <module>CS126</module>
         </modules>
         <staffmembers>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS126L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>15-24</weeks>
         <day>2</day>
         <type>LEC</type>
         <rooms>
            <room>H0.52</room>
         </rooms>
         <modules>
            <module>CS126</module>
         </modules>
         <staffmembers>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS126L</name>
         <description />
         <start>12:00</start>
         <end>13:00</end>
         <weeks>15-24</weeks>
         <day>3</day>
         <type>LEC</type>
         <rooms>
            <room>PLT</room>
         </rooms>
         <modules>
            <module>CS126</module>
         </modules>
         <staffmembers>
            <staffmember>8570237</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS131L</name>
         <description />
         <start>17:00</start>
         <end>18:00</end>
         <weeks>15-24</weeks>
         <day>3</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS131</module>
         </modules>
         <staffmembers>
            <staffmember>1174683</staffmember>
         </staffmembers>
      </Activity>
      <Activity>
         <name>CS131L</name>
         <description />
         <start>10:00</start>
         <end>11:00</end>
         <weeks>15-24</weeks>
         <day>2</day>
         <type>LEC</type>
         <rooms>
            <room>CS_CS1.04</room>
         </rooms>
         <modules>
            <module>CS131</module>
         </modules>
         <staffmembers>
            <staffmember>1174683</staffmember>
         </staffmembers>
      </Activity>
   </Activities>
</Data>"""

}
