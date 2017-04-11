package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.tabula.data.model.Member

trait CurrentMemberComponent {
	def optionalCurrentMember: Option[Member]
	def currentMember: Member
}
