package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.model.MemberNote
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class MemberNoteCreationFixtureCommandInternal extends CommandInternal[MemberNote] {
	self: MemberDaoComponent with MemberNoteDaoComponent with TransactionalComponent =>

	var memberId: String = _
	var note: String = _
	var title = ""
	var creatorId: String = _

	def applyInternal(): MemberNote = transactional() {
		val member = memberDao.getByUniversityId(memberId).getOrElse(
			throw new IllegalArgumentException(s"Member with university ID $memberId not found")
		)
		val memberNote = new MemberNote
		memberNote.member = member
		memberNote.note = note
		memberNote.title = title
		memberNote.creatorId = creatorId
		memberNoteDao.saveOrUpdate(memberNote)
		memberNote
	}

}

object MemberNoteCreationFixtureCommand {
	def apply(): MemberNoteCreationFixtureCommandInternal with ComposableCommand[MemberNote] with AutowiringMemberDaoComponent with AutowiringMemberNoteDaoComponent with AutowiringTransactionalComponent with Unaudited with PubliclyVisiblePermissions ={
		new MemberNoteCreationFixtureCommandInternal
			with ComposableCommand[MemberNote]
			with AutowiringMemberDaoComponent
			with AutowiringMemberNoteDaoComponent
			with AutowiringTransactionalComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}