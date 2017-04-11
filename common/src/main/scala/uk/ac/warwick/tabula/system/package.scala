package uk.ac.warwick.tabula

import scala.annotation.meta.getter

package object system {

	/** Add this annotation to a property to exclude it from Spring binding.
	 *  Don't use NoBindInternal directly because by default an annotation will
	 *  only get added to the private field, not the getter or setter. */
	type NoBind = NoBindInternal @getter

}