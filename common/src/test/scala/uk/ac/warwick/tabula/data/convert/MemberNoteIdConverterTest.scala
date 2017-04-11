package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.services.MemberNoteService
import uk.ac.warwick.tabula.data.model.MemberNote
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase

class MemberNoteIdConverterTest extends TestBase with Mockito {

	val converter = new MemberNoteIdConverter
	var service: MemberNoteService = mock[MemberNoteService]
	converter.service = service

	@Test def validInput {
		val memberNote = new MemberNote
		memberNote.id = "12345"
		service.getNoteById("12345") returns (Some(memberNote))
		converter.convertRight("12345") should be (memberNote)
	}

	@Test def invalidInput {
		service.getNoteById("123") returns (None)
		converter.convertRight("123") should be (null)
	}

	@Test def formatting {
		val memberNote = new MemberNote
		memberNote.id = "54321"
		converter.convertLeft(memberNote) should be ("54321")
		converter.convertLeft(null) should be (null)
	}

}
