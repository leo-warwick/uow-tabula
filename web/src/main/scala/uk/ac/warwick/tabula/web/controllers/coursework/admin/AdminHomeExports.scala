package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.joda.time.ReadableInstant
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model._

import scala.collection.JavaConverters._
import scala.xml._

object AdminHomeExports {
	class XMLBuilder(val department: Department, val info: DepartmentHomeInformation) extends AdminHomeExport {

		var topLevelUrl: String = Wire.property("${toplevel.url}")

		// Pimp XML elements to allow a map mutator for attributes
		implicit class PimpedElement(elem: Elem) {
			def %(attrs:Map[String,Any]): Elem = {
				val seq = for( (n,v) <- attrs ) yield new UnprefixedAttribute(n, toXMLString(Some(v)), Null)
				(elem /: seq) ( _ % _ )
			}
		}

		def toXMLString(value: Option[Any]): String = value match {
			case Some(o: Option[Any]) => toXMLString(o)
			case Some(i: ReadableInstant) => isoFormat(i)
			case Some(b: Boolean) => b.toString.toLowerCase
			case Some(i: Int) => i.toString
			case Some(s: String) => s
			case Some(other) => other.toString
			case None => ""
		}

		def toXML: Elem = {
			val assignments = info.modules.flatMap { _.assignments.asScala }.filter { _.isAlive }

			<assignments>
				{ assignments map assignmentElement }
			</assignments>
		}

		def assignmentElement(assignment: Assignment): Elem = {
			<assignment>

			</assignment> % assignmentData(assignment)
		}
	}

	trait AdminHomeExport {
		val department: Department
		val info: DepartmentHomeInformation
		def topLevelUrl: String

		val isoFormatter = DateFormats.IsoDateTime
		def isoFormat(i: ReadableInstant): String = isoFormatter print i

		protected def assignmentData(assignment: Assignment): Map[String, Any] = Map(
			"module-code" -> assignment.module.code,
			"id" -> assignment.id,
			"open-date" -> assignment.openDate,
			"open-ended" -> assignment.openEnded,
			"close-date" -> (if (assignment.openEnded) "" else assignment.closeDate),
			"last-submission-date" -> assignment.submissions.asScala.map { _.submittedDate }.sortBy { _.getMillis }.reverse.headOption,
			"submissions-zip-url" -> (topLevelUrl + "/coursework" + Routes.admin.assignment.submissionsZip(assignment))
		)
	}
}
