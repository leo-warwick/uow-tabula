package uk.ac.warwick.tabula

import scala.util.Random

/**
 * Not actually used anywhere but we could use it as an auxiliary ID for something
 * that needed a short URL, like an assignment.
 */
object ShortCode {
	val length = 5
	def random(): String = Random.alphanumeric.take(length).mkString
}

