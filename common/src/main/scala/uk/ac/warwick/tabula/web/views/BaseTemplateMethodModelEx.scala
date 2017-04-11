package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.tabula.JavaImports._
import collection.JavaConverters._
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel

/**
 * Handles some of the boilerplate of unwrapping model objects into regular objects.
 */
abstract class BaseTemplateMethodModelEx extends TemplateMethodModelEx {

	def execMethod(args: Seq[_]): Object

	override def exec(args: JList[_]): Object = execMethod(unwrapArgs(args))

	protected def unwrapArgs(list: JList[_]): Seq[AnyRef] =
		list.asScala.toSeq.map { model =>
			DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel])
		}
}