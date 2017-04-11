package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMemberNoteDaoComponent, MemberNoteDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{ExtenuatingCircumstances, Member, MemberNote}

trait MemberNoteServiceComponent {
	def memberNoteService: MemberNoteService
}

trait AutowiringMemberNoteServiceComponent extends MemberNoteServiceComponent {
	var memberNoteService: MemberNoteService = Wire[MemberNoteService]
}

trait MemberNoteService {

	def getNoteById(id: String): Option[MemberNote]
	def listNotes(student: Member): Seq[MemberNote]
	def listNonDeletedNotes(student: Member): Seq[MemberNote]
	def saveOrUpdate(memberNote: MemberNote)
	def delete(note: MemberNote)

	def getExtenuatingCircumstancesById(id: String): Option[ExtenuatingCircumstances]
	def listExtenuatingCircumstances(student: Member): Seq[ExtenuatingCircumstances]
	def listNonDeletedExtenuatingCircumstances(student: Member): Seq[ExtenuatingCircumstances]
	def saveOrUpdate(circumstances: ExtenuatingCircumstances)
	def delete(circumstances: ExtenuatingCircumstances)

}


abstract class AbstractMemberNoteService extends MemberNoteService {
	self: MemberNoteDaoComponent =>

	def getNoteById(id: String): Option[MemberNote] =
		memberNoteDao.getNoteById(id)

	def listNotes(student: Member): Seq[MemberNote] =
		memberNoteDao.listNotes(student, includeDeleted = true)

	def listNonDeletedNotes(student: Member): Seq[MemberNote] =
		memberNoteDao.listNotes(student, includeDeleted = false)

	def saveOrUpdate(memberNote: MemberNote): Unit =
		memberNoteDao.saveOrUpdate(memberNote)

	def delete(memberNote: MemberNote): Unit =
		memberNoteDao.delete(memberNote)

	def getExtenuatingCircumstancesById(id: String): Option[ExtenuatingCircumstances] =
		memberNoteDao.getExtenuatingCircumstancesById(id)

	def listExtenuatingCircumstances(student: Member): Seq[ExtenuatingCircumstances] =
		memberNoteDao.listExtenuatingCircumstances(student, includeDeleted = true)

	def listNonDeletedExtenuatingCircumstances(student: Member): Seq[ExtenuatingCircumstances] =
		memberNoteDao.listExtenuatingCircumstances(student, includeDeleted = false)

	def saveOrUpdate(circumstances: ExtenuatingCircumstances): Unit =
		memberNoteDao.saveOrUpdate(circumstances)

	def delete(circumstances: ExtenuatingCircumstances): Unit =
		memberNoteDao.delete(circumstances)

}

@Service("memberNoteService")
class MemberNoteServiceImpl
	extends AbstractMemberNoteService
	with AutowiringMemberNoteDaoComponent