package uk.ac.warwick.tabula.system.exceptions

object ExceptionTokens {
	private var counter = System.currentTimeMillis()

	def newToken: String = {
		counter += 1
		counter.toString
	}
}