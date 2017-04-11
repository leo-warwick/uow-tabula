package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}

case class StudentRelationshipChange(
	oldAgents: Seq[Member],
	modifiedRelationship: StudentRelationship
)
