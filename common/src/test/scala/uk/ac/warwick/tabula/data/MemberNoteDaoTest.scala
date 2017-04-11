package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}
import org.junit.Before

class MemberNoteDaoTest extends PersistenceTestBase {

	val memberNoteDao = new MemberNoteDaoImpl

	@Before
	def setup() {
		  memberNoteDao.sessionFactory = sessionFactory
	}

	@Test def saveAndFetch {
		transactional { tx =>

			val student = Fixtures.student("123", "abc")
			val note = Fixtures.memberNoteWithId("the note", student, "123")

			memberNoteDao.getNoteById(note.id) should be (None)

			memberNoteDao.saveOrUpdate(note)

			memberNoteDao.getNoteById(note.id) should be (Option(note))
			memberNoteDao.getNoteById(note.id).get.note should be ("the note")

		}
	}

	@Test def listNotesIncludeDeleted {
		transactional { tx =>

			val student = Fixtures.student("456", "def")
			session.saveOrUpdate(student)
			val note = Fixtures.memberNote("another note", student )

			memberNoteDao.listNotes(student).size should be (0)

			memberNoteDao.saveOrUpdate(note)

			memberNoteDao.listNotes(student, false).size should be (1)

			note.deleted = true
			memberNoteDao.saveOrUpdate(note)
			memberNoteDao.listNotes(student, false).size should be (0)
			memberNoteDao.listNotes(student, true).size should be (1)

		}
	}

	@Test def listNotes {
		transactional { tx =>

			val student = Fixtures.student("456", "def")
			session.saveOrUpdate(student)
			val note = Fixtures.memberNote("another note", student )

			memberNoteDao.listNotes(student).size should be (0)

			memberNoteDao.saveOrUpdate(note)

			memberNoteDao.listNotes(student, false).size should be (1)

		}
	}

}
