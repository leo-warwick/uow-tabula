package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations.Type
import org.joda.time.DateTime
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.NamedQueries
import javax.persistence.NamedQuery

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "modeofattendance.code", query = "select modeOfAttendance from ModeOfAttendance modeOfAttendance where code = :code")))
class ModeOfAttendance {

	def this(code: String = null, shortName: String = null, fullName: String = null) {
		this()
		this.code = code
		this.shortName = shortName
		this.fullName = fullName
	}

	@Id var code: String = _
	var shortName: String = _
	var fullName: String = _

	var lastUpdatedDate: DateTime = DateTime.now

	override def toString: String = fullName.toLowerCase()

	def fullNameAliased: String = {
		if (code.equals("F"))
			"Full-time"
		else fullName
	}

}
