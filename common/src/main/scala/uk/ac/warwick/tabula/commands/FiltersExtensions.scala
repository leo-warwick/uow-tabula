package uk.ac.warwick.tabula.commands

import java.net.{URI, URLDecoder}

import org.apache.http.client.utils.URLEncodedUtils
import org.hibernate.NullPrecedence
import org.hibernate.criterion.{Order, Restrictions}
import org.hibernate.criterion.Restrictions.{gt => _, _}
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.data.ScalaRestriction._
import uk.ac.warwick.tabula.data.convert._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.util.web.UriBuilder

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.TwoWayConverter

object FiltersExtensions {
	val AliasPaths: Map[String, Seq[(String, AliasAndJoinType)]] = Seq(

		"assignment" -> Seq(
			"assignment" -> AliasAndJoinType("assignment")
		),

		"module" -> Seq(
			"assignment" -> AliasAndJoinType("assignment"),
			"assignment.module" -> AliasAndJoinType("module")
		),

		"department" -> Seq(
			"assignment" -> AliasAndJoinType("assignment"),
			"assignment.module" -> AliasAndJoinType("module"),
			"module.adminDepartment" -> AliasAndJoinType("department")
		)
	).toMap

	val MaxExtensionsPerPage = 100
	val DefaultExtensionsPerPage = 25
}

trait FiltersExtensions extends {

	self: TermServiceComponent =>

	import FiltersExtensions._

	def times: JList[TimeFilter]
	def states: JList[ExtensionState]
	def assignments: JList[Assignment]
	def modules: JList[Module]
	def departments: JList[Department]

	def defaultOrder: JList[Order]
	def sortOrder: JList[Order]
	var otherCriteria: JList[String] = JArrayList()

	def serializeFilter: String = {
		val result = new UriBuilder()
		departments.asScala.foreach(p => result.addQueryParameter("departments", p.code))
		modules.asScala.foreach(p => result.addQueryParameter("modules", p.code))
		assignments.asScala.foreach(p => result.addQueryParameter("assignments", p.id))
		states.asScala.foreach(p => result.addQueryParameter("state", p.dbValue))
		times.asScala.foreach(p => result.addQueryParameter("times", p.code))
		otherCriteria.asScala.foreach(p => result.addQueryParameter("otherCriteria", p.toString))
		Option(result.getQuery).getOrElse("")
	}

	def receivedRestriction: Option[ScalaRestriction] = if (times.isEmpty) {
			None
	} else {
		val criterion = disjunction()
		times.asScala.foreach(t => criterion.add(Restrictions.gt("requestedOn", t.time)))
		Some(new ScalaRestriction(criterion))
	}

	def stateRestriction: Option[ScalaRestriction] = inIfNotEmpty("_state", states.asScala)

	def assignmentRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"assignment", assignments.asScala,
		AliasPaths("assignment") : _*
	)

	def moduleRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"module.code", modules.asScala.map(_.code),
		AliasPaths("module") : _*
	)

	def departmentRestriction: Option[ScalaRestriction] = inIfNotEmpty(
		"department.code", departments.asScala.map(_.code),
		AliasPaths("department") : _*
	)

	protected def buildOrders(orders: Seq[Order]): Seq[ScalaOrder] =
		orders.map { underlying =>
			underlying.getPropertyName match {
				case r"""([^\.]+)${aliasPath}\..*""" => ScalaOrder(underlying.nulls(NullPrecedence.LAST), AliasPaths(aliasPath) : _*)
				case _ => ScalaOrder(underlying.nulls(NullPrecedence.LAST))
			}
		}
}

trait DeserializesExtensionsFilter {
	this: FiltersExtensions =>
	def deserializeFilter(filterString: String): Unit
}

trait DeserializesExtensionsFilterImpl extends DeserializesExtensionsFilter with FiltersExtensions with Logging
	with AssessmentServiceComponent with ModuleAndDepartmentServiceComponent with TermServiceComponent {

	def deserializeFilter(filterString: String): Unit = {
		val params: Map[String, Seq[String]] =
			URLEncodedUtils.parse(new URI(null, null, null, URLDecoder.decode(filterString, "UTF-8"), null), "UTF-8")
				.asScala
				.groupBy(_.getName)
				.map { case (name, nameValuePairs) => name -> nameValuePairs.map(_.getValue) }

		modules.clear()
		params.get("modules").foreach{_.foreach{ item =>
			val moduleCodeConverter = new ModuleCodeConverter
			moduleCodeConverter.service = moduleAndDepartmentService
			moduleCodeConverter.convertRight(item) match {
				case module: Module => modules.add(module)
				case _ => logger.warn(s"Could not deserialize filter with module $item")
			}
		}}

		departments.clear()
		params.get("departments").foreach{_.foreach{ item =>
			val departmentConverter = new DepartmentCodeConverter
			departmentConverter.service = moduleAndDepartmentService
			departmentConverter.convertRight(item) match {
				case department: Department => departments.add(department)
				case _ => logger.warn(s"Could not deserialize filter with department $item")
			}
		}}

		departments.clear()
		params.get("assignments").foreach{_.foreach{ item =>
			val assignmentConverter = new AssignmentIdConverter
			assignmentConverter.service = assessmentService
			assignmentConverter.convertRight(item) match {
				case assignment: Assignment => assignments.add(assignment)
				case _ => logger.warn(s"Could not deserialize filter with assignment $item")
			}
		}}

		states.clear()
		params.get("states").foreach{_.foreach{ item =>
			try {
				states.add(ExtensionState.fromCode(item))
			} catch {
				case e: IllegalArgumentException => logger.warn(s"Could not deserialize filter with state $item")
			}
		}}

		times.clear()
		params.get("times").foreach{_.foreach{ item =>
			try {
				times.add(TimeFilter.fromCode(item))
			} catch {
				case e: IllegalArgumentException => logger.warn(s"Could not deserialize filter with time $item")
			}
		}}

		otherCriteria.clear()
		params.get("otherCriteria").foreach{_.foreach{ item => otherCriteria.add(item) }}
	}
}

trait AutowiringDeserializesExtensionsFilterImpl extends DeserializesExtensionsFilterImpl
	with AutowiringAssessmentServiceComponent
	with AutowiringModuleAndDepartmentServiceComponent

sealed abstract class TimeFilter(val code: String, val time: DateTime)
object TimeFilter {
	case object ThisWeek extends TimeFilter("This week", DateTime.now.withDayOfWeek(1))
	case object ThisMonth extends TimeFilter("This month", DateTime.now.withDayOfMonth(1))
	case class ThisTerm(termService: TermService) extends TimeFilter(ThisTerm.Label, termService.getTermFromDate(DateTime.now).getStartDate)
	object ThisTerm { val Label = "This term" }
	case object ThisYear extends TimeFilter("This year", DateTime.now.withDayOfYear(1))

	def fromCode(code: String)(implicit termService: TermService): TimeFilter = code match {
		case ThisWeek.code => ThisWeek
		case ThisMonth.code => ThisMonth
		case ThisTerm.Label => ThisTerm(termService)
		case ThisYear.code => ThisYear
		case _ => throw new IllegalArgumentException()
	}

	def all(implicit termService: TermService) = Seq(ThisWeek, ThisMonth, ThisTerm(termService: TermService), ThisYear)
}

class TimeFilterConverter extends TwoWayConverter[String, TimeFilter] {
	@Autowired implicit var termService: TermService = _

	override def convertRight(code: String): TimeFilter = TimeFilter.fromCode(code)
	override def convertLeft(time: TimeFilter): String = (Option(time) map { _.code }).orNull
}