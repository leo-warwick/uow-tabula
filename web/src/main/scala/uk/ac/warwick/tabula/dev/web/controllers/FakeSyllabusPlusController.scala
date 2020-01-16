package uk.ac.warwick.tabula.dev.web.controllers

import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.BasicResponseHandler
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringApacheHttpClientComponent, UserLookupService}

import scala.collection.mutable
import scala.util.{Success, Try}
import scala.xml.{Elem, XML}

/**
  * Proxy student requests to Syllabus+, cacheing responses.
  *
  * Allows for the override of individual student responses, for testing purposes.
  */
@Controller
class FakeSyllabusPlusController extends Logging with AutowiringApacheHttpClientComponent {

  val userLookup: UserLookupService = Wire[UserLookupService]
  val studentTimetables: mutable.Map[StudentYearKey, String] = mutable.Map.empty
  val moduleTimetables: mutable.Map[ModuleYearKey, String] = mutable.Map.empty
  val moduleNoStudentsTimetables: mutable.Map[ModuleNoStudentsYearKey, String] = mutable.Map.empty
  val staffTimetables: mutable.Map[StaffYearKey, String] = mutable.Map.empty
  val baseUri: String = Wire.optionProperty("${scientia.stubfallback.url}")
    .getOrElse("https://timetablingmanagement.warwick.ac.uk/xml")

  def studentUri(year: String): String = baseUri + year + "/?StudentXML"

  def moduleUri(year: String): String = baseUri + year + "/?ModuleXML"

  def moduleNoStudentsUri(year: String): String = baseUri + year + "/?ModuleNoStudentsXML"

  def staffUri(year: String): String = baseUri + year + "/?StaffXML"

  @RequestMapping(value = Array("/stubTimetable/{year}"), params = Array("StudentXML"))
  def getStudent(@RequestParam("p0") studentId: String, @PathVariable year: String): Elem = {
    val xml = studentTimetables.getOrElseUpdate(StudentYearKey(studentId, year), {
      val req = RequestBuilder.get(studentUri(year)).addParameter("p0", studentId)
      val xml = Try(httpClient.execute(req.build(), new BasicResponseHandler)) match {
        case Success(s) => s
        // If we get an error back, just return then XML for an empty list immediately,
        // otherwise the XML handler in ScientiaHttpTimetableFetchingService
        // will wait for the request keep-alive to time out (60s) before finally giving up.
        // n.b. if we made this controller return a non-200 status code then we probably wouldn't have
        // to do this.
        case _ => "<?xml version=\"1.0\" encoding=\"utf-8\"?><Data><Activities></Activities></Data>"
      }
      xml
    })
    XML.loadString(xml)
  }

  // note that the "year" variable should be in the same format Syllabus+ uses
  // i.e. 1213 for academic year 2012-2013
  @RequestMapping(method = Array(RequestMethod.POST), value = Array("/stubTimetable/student"))
  def saveStudent(@RequestParam studentId: String, @RequestParam year: String, @RequestParam content: String): Unit = {
    if (!studentId.matches("^[0-9]+")) {
      // it's probably a usercode, since functional tests don't have access  to warwickIds for their users
      val user = userLookup.getUserByUserId(studentId)
      studentTimetables.put(StudentYearKey(user.getWarwickId, year), content)
    } else {
      studentTimetables.put(StudentYearKey(studentId, year), content)
    }
  }

  @RequestMapping(value = Array("/stubTimetable/{year}"), params = Array("ModuleXML"))
  def getModule(@RequestParam("p0") moduleCode: String, @PathVariable year: String): Elem = {
    val xml = moduleTimetables.getOrElseUpdate(ModuleYearKey(moduleCode, year), {
      val req = RequestBuilder.get(moduleUri(year)).addParameter("p0", moduleCode)
      val xml = Try(httpClient.execute(req.build(), new BasicResponseHandler)) match {
        case Success(s) => s
        // If we get an error back, just return then XML for an empty list immediately,
        // otherwise the XML handler in ScientiaHttpTimetableFetchingService
        // will wait for the request keep-alive to time out (60s) before finally giving up.
        // n.b. if we made this controller return a non-200 status code then we probably wouldn't have
        // to do this.
        case _ => "<?xml version=\"1.0\" encoding=\"utf-8\"?><Data><Activities></Activities></Data>"
      }
      xml
    })
    XML.loadString(xml)
  }

  // note that the "year" variable should be in the same format Syllabus+ uses
  // i.e. 1213 for academic year 2012-2013
  @RequestMapping(method = Array(RequestMethod.POST), value = Array("/stubTimetable/module"))
  def saveModule(@RequestParam moduleCode: String, @RequestParam year: String, @RequestParam content: String): Unit = {
    moduleTimetables.put(ModuleYearKey(moduleCode, year), content)
  }

  @RequestMapping(value = Array("/stubTimetable/{year}"), params = Array("ModuleNoStudentsXML"))
  def getModuleNoStudents(@RequestParam("p0") moduleCode: String, @PathVariable year: String): Elem = {
    val xml = moduleNoStudentsTimetables.getOrElseUpdate(ModuleNoStudentsYearKey(moduleCode, year), {
      val req = RequestBuilder.get(moduleNoStudentsUri(year)).addParameter("p0", moduleCode)
      val xml = Try(httpClient.execute(req.build(), new BasicResponseHandler)) match {
        case Success(s) => s
        // If we get an error back, just return then XML for an empty list immediately,
        // otherwise the XML handler in ScientiaHttpTimetableFetchingService
        // will wait for the request keep-alive to time out (60s) before finally giving up.
        // n.b. if we made this controller return a non-200 status code then we probably wouldn't have
        // to do this.
        case _ => "<?xml version=\"1.0\" encoding=\"utf-8\"?><Data><Activities></Activities></Data>"
      }
      xml
    })
    XML.loadString(xml)
  }

  @RequestMapping(method = Array(RequestMethod.POST), value = Array("/stubTimetable/moduleNoStudents"))
  def saveModuleNoStudents(@RequestParam moduleCode: String, @RequestParam year: String, @RequestParam content: String): Unit = {
    moduleNoStudentsTimetables.put(ModuleNoStudentsYearKey(moduleCode, year), content)
  }

  @RequestMapping(value = Array("/stubTimetable/{year}"), params = Array("StaffXML"))
  def getStaff(@RequestParam("p0") staffId: String, @PathVariable year: String): Elem = {
    val xml = staffTimetables.getOrElseUpdate(StaffYearKey(staffId, year), {
      val req = RequestBuilder.get(staffUri(year)).addParameter("p0", staffId)
      val xml = Try(httpClient.execute(req.build(), new BasicResponseHandler)) match {
        case Success(s) => s
        // If we get an error back, just return then XML for an empty list immediately,
        // otherwise the XML handler in ScientiaHttpTimetableFetchingService
        // will wait for the request keep-alive to time out (60s) before finally giving up.
        // n.b. if we made this controller return a non-200 status code then we probably wouldn't have
        // to do this.
        case _ => "<?xml version=\"1.0\" encoding=\"utf-8\"?><Data><Activities></Activities></Data>"
      }
      xml
    })
    XML.loadString(xml)
  }

  // note that the "year" variable should be in the same format Syllabus+ uses
  // i.e. 1213 for academic year 2012-2013
  @RequestMapping(method = Array(RequestMethod.POST), value = Array("/stubTimetable/staff"))
  def saveStaff(@RequestParam staffId: String, @RequestParam year: String, @RequestParam content: String): Unit = {
    if (!staffId.matches("^[0-9]+")) {
      // it's probably a usercode, since functional tests don't have access  to warwickIds for their users
      val user = userLookup.getUserByUserId(staffId)
      staffTimetables.put(StaffYearKey(user.getWarwickId, year), content)
    } else {
      staffTimetables.put(StaffYearKey(staffId, year), content)
    }
  }

}

case class StudentYearKey(studentId: String, year: String)

case class ModuleYearKey(moduleCode: String, year: String)

case class ModuleNoStudentsYearKey(moduleCode: String, year: String)

case class StaffYearKey(staffId: String, year: String)
