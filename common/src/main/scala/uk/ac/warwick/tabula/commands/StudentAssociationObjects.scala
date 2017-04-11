package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.CourseType

/**
  * Data store objects for handling student associations
	* An association is the connection between a student a student another entity
	* Entities currently include agent, small groups and markers
	*/

case class StudentAssociationData(
	universityId: String,
	firstName: String,
	lastName: String,
	courseType: CourseType,
	routeCode: String,
	yearOfStudy: Int
) {
	override def equals(that: Any): Boolean = {
		that match {
			case that: StudentAssociationData => this.universityId == that.universityId
			case _ => false
		}
	}
}

case class StudentAssociationEntityData(
	entityId: String,
	displayName: String,
	sortName: String,
	isHomeDepartment: Option[Boolean],
	capacity: Option[Int],
	students: Seq[StudentAssociationData]
) {
	def updateStudents(newStudents: Seq[StudentAssociationData]): StudentAssociationEntityData = StudentAssociationEntityData(
		entityId = this.entityId,
		displayName = this.displayName,
		sortName = this.sortName,
		isHomeDepartment = this.isHomeDepartment,
		capacity = this.capacity,
		students = newStudents
	)
}

case class StudentAssociationResult(unallocated: Seq[StudentAssociationData], allocated: Seq[StudentAssociationEntityData])
