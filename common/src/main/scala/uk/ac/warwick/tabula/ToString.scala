package uk.ac.warwick.tabula

/**
 * Trait which defines a basic `toString` method for you.
 * All you have to do is return a list of interesting properties
 * from `toStringProps`, and it will stringify that along with
 * the class name.
 */
trait ToString {
	def toStringProps: Seq[(String, Any)]
	override def toString(): String = ToString.forObject(this, toStringProps : _*)
}

/** Alternative to the trait that avoids polluting the class's interface.
	* Just use ToString() to implement your toString method.
	*/
object ToString {

	def forProps(props: (String, Any)*): String =
		props.map { case (k, v) => k + "=" + v }.mkString("[", ",", "]")

	def forObject(self: AnyRef, props: (String, Any)*): String =
		self.getClass.getSimpleName + forProps(props: _*)

}