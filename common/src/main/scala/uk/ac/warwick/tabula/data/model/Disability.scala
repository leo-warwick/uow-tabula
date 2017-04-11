package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence.{Entity, Id}

object Disability {
	val notReportableDefinition = "NONE"
}

@Entity
class Disability {
	def this(code: String = null, definition: String = null) {
		this()
		this.code = code
		this.sitsDefinition = definition
	}

	@Id var code: String = _
	var shortName: String = _
	var sitsDefinition: String = _
	var tabulaDefinition: String = _
	var lastUpdatedDate: DateTime = DateTime.now

	/**
	 * Use our override out of preference, where available.
	 * It's manually maintained in the database.
	 */
	def definition: String = Option(tabulaDefinition).getOrElse(sitsDefinition)

	/**
	 * If tabulaDefinition is null (ie. there's been a new SITS definition added), fail safe to true.
	 * Otherwise, true only for tabulaDefinitions we have defined as not reportably disabled in the database
	 */
	def reportable: Boolean = Option(tabulaDefinition).forall(_ != Disability.notReportableDefinition)

	override def toString: String = definition
}