package uk.ac.warwick.tabula.helpers

import freemarker.template.{SimpleHash, TemplateMethodModelEx}
import java.util
import freemarker.ext.beans.BeanModel
import freemarker.template.utility.DeepUnwrap
import freemarker.template.TemplateModel

/**
 * Freemarker can't cope with maps who's keys are not strings
 *
 * Here's a helper to try and fix it.
 *
 *
 */
class FreemarkerMapHelper extends TemplateMethodModelEx {

	override def exec(p1: util.List[_]): AnyRef = {
		(p1.get(0), p1.get(1)) match {
			// The key may already be a wrapped object
			case (m: SimpleHash, k: TemplateModel) => {
				m.toMap.get(DeepUnwrap.unwrap(k)) match {
					case r: AnyRef => r
					case _ => null
				}
			}
			case (m: SimpleHash, k: AnyRef) => {
				m.toMap.get(k) match {
					case r: AnyRef => r
					case _ => null
				}
			}
			// You've called with arguments that don't look like a wrapped map & a key
			case _ => {
				null
			}
		}
	}
}
