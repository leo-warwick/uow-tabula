package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType.Staff

object SearchAgentsCommand {
	def apply(user: CurrentUser) =
	new SearchAgentsCommandInternal(user)
		with ComposableCommand[Seq[Member]]
		with Unaudited
		with SearchProfilesCommandPermissions
}

class SearchAgentsCommandInternal(user: CurrentUser) extends AbstractSearchProfilesCommand(user, Staff)
	with CommandInternal[Seq[Member]] {

	override def applyInternal(): Seq[Member] =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()

	private def queryMatches = {
		profileQueryService.findWithQuery(query, Seq(), includeTouched = false, userTypes, searchAcrossAllDepartments = true)
	}
}
