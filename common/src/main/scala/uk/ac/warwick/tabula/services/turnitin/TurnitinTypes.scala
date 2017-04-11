package uk.ac.warwick.tabula.services.turnitin

import scala.util.matching.Regex


/* Typed strings and other value containers. There are so many string based
 * values in the Turnitin API that it makes sense to wrap them in a simple class,
 * particularly where the string is a more complex format (see DocumentTitle)
 */

case class ClassName(val value: String)
case class ClassId(val value: String)
case class AssignmentName(val value: String)
case class AssignmentId(val value: String)
case class DocumentId(val value: String)

// AnyDocumentTitle is more lenient than DocumentTitle just so we can
// handle items that might not have the expected string format
class AnyDocumentTitle(val value:String)
object AnyDocumentTitle {
    def apply(value: String): AnyDocumentTitle = value match {
        case DocumentTitle(id, extension) => new DocumentTitle(id, extension)
        case _ => new AnyDocumentTitle(value)
    }
}

class DocumentTitle(val id: String, val extension: String) extends AnyDocumentTitle( id + "." + extension )
object DocumentTitle {
    private val Pattern = new Regex("(.+)\\.(.+?)")
    def apply(id:String, extension:String) = new DocumentTitle(id, extension)

    // unapply method is for matching, so you can do e.g.
    // docTitle match { case DocumentTitle(id, extension) => ... }
    def unapply(thing: Any): Option[(String,String)] = thing match {
				case s: CharSequence => s match {
					case Pattern(id, extension) => Some((id, extension))
					case _ => None
				}
				case t: DocumentTitle => Some((t.id, t.extension))
        case _ => None
    }
}