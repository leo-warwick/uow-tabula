package uk.ac.warwick.tabula.data.model

import org.joda.time.DateTime
import javax.persistence._

@Entity
@NamedQueries(Array(
	new NamedQuery(name = "course.code", query = "select course from Course course where code = :code")))
class Course {

	def this(code: String = null, name: String = null) {
		this()
		this.code = code
		this.name = name
	}

	@Id var code: String = _
	var shortName: String = _
	var name: String = _
	var title: String = _

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="department_id")
	private var _department: Department = _
	def department_=(department: Department): Unit = {
		_department = department
	}
	def department: Option[Department] = Option(_department)
	

	var lastUpdatedDate: DateTime = DateTime.now

	override def toString: String = name

}
