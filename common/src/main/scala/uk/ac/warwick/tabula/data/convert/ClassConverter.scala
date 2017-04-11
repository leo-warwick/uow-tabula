package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.TwoWayConverter
import org.springframework.format.Formatter
import java.util.Locale

import uk.ac.warwick.tabula.helpers.StringUtils._

class ClassConverter extends TwoWayConverter[String, Class[_]] {

	override def convertRight(className: String): Class[_] =
		if (className.hasText) try { Class.forName(className) } catch { case e: ClassNotFoundException => null }
		else null

	override def convertLeft(clazz: Class[_]): String = Option(clazz) match {
		case Some(clazz) => clazz.getName
		case None => null
	}

}