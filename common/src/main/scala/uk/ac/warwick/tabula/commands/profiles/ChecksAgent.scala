package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.data.AutowiringRelationshipDaoComponent

trait ChecksAgent extends AutowiringRelationshipDaoComponent {

	def isAgent(universityId:String): Boolean = {
		relationshipDao.isAgent(universityId)
	}

}
