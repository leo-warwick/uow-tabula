package uk.ac.warwick.tabula.helpers

object ReverseMapHelper {
	/**
	 * reverses keys and values in many-to-many maps
	 */
	def reverseMap[A, B](aMap: Map[A, Set[B]]) : Map[B, Set[A]] = {
		aMap.toSeq
			.flatMap { case (key, values) =>
			values.map { v => v -> key }
		}
			.groupBy { case (key, _) => key }
			.mapValues { _.map { case (_, value) => value }.toSet }
	}
}
