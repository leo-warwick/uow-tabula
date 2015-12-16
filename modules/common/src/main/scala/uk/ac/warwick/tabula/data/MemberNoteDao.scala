package uk.ac.warwick.tabula.data

import uk.ac.warwick.spring.Wire
import org.hibernate.criterion.Order._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model._

trait MemberNoteDaoComponent {
	val memberNoteDao: MemberNoteDao
}

trait AutowiringMemberNoteDaoComponent extends MemberNoteDaoComponent {
	val memberNoteDao = Wire[MemberNoteDao]
}

trait MemberNoteDao {
	def getById(id: String): Option[MemberNote]
	def list(student: Member, includeDeleted: Boolean = false): Seq[MemberNote]
	def saveOrUpdate(memberNote: MemberNote)
	def delete(memberNote: MemberNote)

}

@Repository
class MemberNoteDaoImpl extends MemberNoteDao with Daoisms {
	def getById(id: String): Option[MemberNote] = session.getById[MemberNote](id)

	def list(student: Member, includeDeleted: Boolean): Seq[MemberNote] =	{
			val criteria = session.newCriteria[MemberNote].add(is("member", student))
			if (!includeDeleted) {
				criteria.add(is("deleted", false))
			}
			criteria.addOrder(desc("lastUpdatedDate")).seq
	}

	def saveOrUpdate(memberNote: MemberNote) = session.saveOrUpdate(memberNote)

	def delete(memberNote: MemberNote) = session.delete(memberNote)

}
