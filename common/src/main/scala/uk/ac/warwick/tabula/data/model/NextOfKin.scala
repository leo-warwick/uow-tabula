package uk.ac.warwick.tabula.data.model


import javax.persistence._
import javax.persistence.CascadeType._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.ToString

@Entity
class NextOfKin extends GeneratedId with ToString {
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	var member: Member = _

	var firstName: String = _
	var lastName: String = _

	var relationship: String = _

	@OneToOne(cascade = Array(ALL), fetch = FetchType.LAZY)
	@JoinColumn(name="ADDRESS_ID")
	var address: Address = _

	// Daytime phone is the address telephone
	//var daytimePhone: String = _

	var eveningPhone: String = _
	var email: String = _

	def fullName: String = firstName + " " + lastName

	def toStringProps = Seq(
		"member" -> member,
		"name" -> (firstName + " " + lastName),
		"relationship" -> relationship)

}