package uk.ac.warwick.tabula.data.model.forms

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.forms.FormFieldContext._


class FormFieldContextUserTypeTest extends TestBase {

		@Test def convertToObject() {
			val ffc = new FormFieldContextUserType
			ffc.convertToObject("submission") should be (Submission)
			ffc.convertToObject("feedback") should be (Feedback)
			an [IllegalArgumentException] should be thrownBy { ffc.convertToObject("disastrous") }
		}

		@Test def convertToValue() {
			val t = new FormFieldContextUserType
			t.convertToValue(Submission) should be ("submission")
			t.convertToValue(Feedback) should be ("feedback")
		}

	}