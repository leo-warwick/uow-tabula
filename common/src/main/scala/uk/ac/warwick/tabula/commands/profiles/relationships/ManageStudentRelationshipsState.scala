package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}

trait ManageStudentRelationshipsState {
	def department: Department
	def relationshipType: StudentRelationshipType
	def user: CurrentUser
}
