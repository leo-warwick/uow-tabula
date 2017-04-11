package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.web.bind.annotation
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestParam
import java.io.PrintWriter
import java.io.StringWriter

import org.springframework.beans.factory.BeanFactoryAware
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import org.hibernate.Session
import uk.ac.warwick.tabula.web.Mav

import scala.beans.BeanProperty

@Controller
@RequestMapping(value = Array("/sysadmin/repl"))
class SysadminREPL extends BaseSysadminController with BeanFactoryAware {

	@BeanProperty var beanFactory: BeanFactory = _
	@Autowired var assignmentService: AssessmentService = _
	@Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = _

	val spel: SpelExpressionParser = new SpelExpressionParser

	@annotation.RequestMapping
	def evaluate(@RequestParam(value = "query", defaultValue = "") query: String): Mav = {
		val response = if (query.hasText) {
			val context = new StandardEvaluationContext(RootObject(session))
			val expression = spel.parseExpression(query)
			try Return(expression.getValue(context))
			catch { case e: Exception => Return(null, e) }
		} else {
			Return(null)
		}
		Mav("sysadmin/repl", "query" -> query, "response" -> response)
	}

	/**
	 * Root object for evaluator. Its properties and methods will be available
	 * as top-level items in your SpEL query.
	 */
	case class RootObject(
		session: Session,
		beanFactory: BeanFactory = beanFactory,
		// expose Assignments as a map of ids
		assignments: MapAccessor[Assignment] = MapAccessor { id =>
			assignmentService.getAssignmentById(id).orNull
		},
		departmentCodes: MapAccessor[Department] = MapAccessor { code =>
			moduleAndDepartmentService.getDepartmentByCode(code).orNull
		})
}

/**
 * Implements a Java Map that only has a working get() method, to allow
 * you to expose some collection of objects as a String-keyed map.
 */
abstract class MapAccessor[A] extends JMap[String, A] {
	def fetch(key: String): A
	override def get(key: Any): A = fetch(key.asInstanceOf[String])
	override def put(key: String, value: A) = throw strop
	override def keySet = throw strop
	override def entrySet = throw strop
	override def values = throw strop
	override def putAll(map: JMap[_ <: String, _ <: A]) = throw strop
	override def size = 1
	override def isEmpty = false
	override def clear = throw strop
	override def containsKey(s: Any) = throw strop
	override def containsValue(v: Any) = throw strop
	override def remove(v: Any) = throw strop

	def strop = new UnsupportedOperationException
}

object MapAccessor {
	/**
	 * Creates a MapAccessor that uses the given function to resolve
	 * the string to an object.
	 */
	def apply[A](fn: String => A) = new MapAccessor[A] {
		override def fetch(id: String): A = fn(id)
	}
}

case class Return(value: Any, exception: Exception = null) {
	lazy val stringValue: String = value.toString
	lazy val isNone: Boolean = value match {
		case None => true
		case _ => false
	}
	lazy val stackTrace: String = {
		val stringer = new StringWriter
		val writer = new PrintWriter(stringer)
		exception.printStackTrace(writer)
		writer.close()
		stringer.toString
	}
	lazy val valueType: String = {
		value match {
			case Some(any: Any) => any.getClass.getSimpleName
			case any: Any => any.getClass.getSimpleName
			case _ => "?"
		}
	}
}