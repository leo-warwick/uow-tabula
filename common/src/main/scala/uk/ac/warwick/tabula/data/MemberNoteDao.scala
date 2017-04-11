package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import org.hibernate.criterion.Order._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model._

trait MemberNoteDaoComponent {
	val memberNoteDao: MemberNoteDao
}

trait AutowiringMemberNoteDaoComponent extends MemberNoteDaoComponent {
	val memberNoteDao: MemberNoteDao = Wire[MemberNoteDao]
}

trait MemberNoteDao {
	def getNoteById(id: String): Option[MemberNote]
	def listNotes(student: Member, includeDeleted: Boolean = false): Seq[MemberNote]
	def saveOrUpdate(memberNote: MemberNote)
	def delete(memberNote: MemberNote)

	def getExtenuatingCircumstancesById(id: String): Option[ExtenuatingCircumstances]
	def listExtenuatingCircumstances(student: Member, includeDeleted: Boolean = false): Seq[ExtenuatingCircumstances]
	def saveOrUpdate(circumstances: ExtenuatingCircumstances)
	def delete(circumstances: ExtenuatingCircumstances)

}

@Repository
class MemberNoteDaoImpl extends MemberNoteDao with Daoisms {
	def getNoteById(id: String): Option[MemberNote] = getById[MemberNote](id)

	def listNotes(student: Member, includeDeleted: Boolean): Seq[MemberNote] =	{
			val criteria = session.newCriteria[MemberNote].add(is("member", student))
			if (!includeDeleted) {
				criteria.add(is("deleted", false))
			}
			criteria.addOrder(desc("lastUpdatedDate")).seq
	}

	def saveOrUpdate(memberNote: MemberNote): Unit = session.saveOrUpdate(memberNote)

	def delete(memberNote: MemberNote): Unit = session.delete(memberNote)

	def getExtenuatingCircumstancesById(id: String): Option[ExtenuatingCircumstances] = getById[ExtenuatingCircumstances](id)

	def listExtenuatingCircumstances(student: Member, includeDeleted: Boolean): Seq[ExtenuatingCircumstances] =	{
		val criteria = session.newCriteria[ExtenuatingCircumstances].add(is("member", student))
		if (!includeDeleted) {
			criteria.add(is("deleted", false))
		}
		criteria.addOrder(desc("lastUpdatedDate")).seq
	}

	def saveOrUpdate(circumstances: ExtenuatingCircumstances): Unit = session.saveOrUpdate(circumstances)

	def delete(circumstances: ExtenuatingCircumstances): Unit = session.delete(circumstances)

}
