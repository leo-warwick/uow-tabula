package uk.ac.warwick.tabula.helpers

/**
	* Allows chaining of Ordering to enable things like: sort by first name ascending then by last name descending.
	* Mainly useful when the number of fields to sort by is decided at run time.
	*/
class ComposableOrdering[T](orderings: Ordering[T]*) extends Ordering[T] {

	override def compare(x: T, y: T): Int = {
		orderings.toStream.map(_.compare(x, y)).find(_ != 0).getOrElse(0)
	}

}
