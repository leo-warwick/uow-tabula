package uk.ac.warwick.tabula.services.timetables

import org.joda.time.LocalTime
import org.mockito.Matchers
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.data.model.{Module, NamedLocation}
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.userlookup.User

import scala.xml.XML

class TimetableFetchingServiceTest extends TestBase with Mockito {

	val module: Module = Fixtures.module("cs132")

	@Test def parseXML() {
		val locationFetchingService = new LocationFetchingService {
			def locationFor(name: String) = NamedLocation(name)
		}
		val mockModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		mockModuleAndDepartmentService.getModulesByCodes(Matchers.any[Seq[String]]) answers {codes =>
			codes.asInstanceOf[Seq[String]].map(code => Fixtures.module(code))
		}

		val userLookup = new MockUserLookup

		val tutor = new User("abcdef")
		tutor.setFoundUser(true)
		tutor.setWarwickId("1170047")

		val student = new User("student")
		student.setFoundUser(true)
		student.setWarwickId("1234567")

		userLookup.registerUserObjects(tutor, student)

		val events = ScientiaHttpTimetableFetchingService.parseXml(XML.loadString(TimetableEvents), AcademicYear(2012), student.getWarwickId, locationFetchingService, mockModuleAndDepartmentService, userLookup)
		events.size should be (10)
		events.head should be (TimetableEvent(
			uid="9d6a9b75399d1b61791569af49abcc7e",
			name="CS132L",
			title="",
			description="",
			startTime=new LocalTime(12, 0),
			endTime=new LocalTime(13, 0),
			weekRanges=Seq(WeekRange(6, 10)),
			day=DayOfWeek.Friday,
			eventType=TimetableEventType.Lecture,
			location=Some(NamedLocation("L5")),
			parent=TimetableEvent.Parent(Some(module)),
			comments=None,
			staff=Seq(tutor),
			students=Nil,
		  year = AcademicYear(2012),
			relatedUrl = None,
			attendance = Map()
		))
		events(1).comments should be (Some("Some comments"))
		events(1).students should be (Seq(student))
	}

	val TimetableEvents = """<?xml version="1.0" encoding="UTF-8"?>
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
         <type>LEC</type>
         <comments>Some comments</comments>
         <rooms>
            <room>MS.02</room>
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