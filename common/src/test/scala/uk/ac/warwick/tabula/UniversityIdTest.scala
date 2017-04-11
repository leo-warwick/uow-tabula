package uk.ac.warwick.tabula

class UniversityIdTest extends TestBase {

	@Test def isValid() {
		UniversityId.isValid("0672089") should be (true)
		UniversityId.isValid("4072089") should be (true)
		UniversityId.isValid("672089") should be (false)
		UniversityId.isValid("06720890") should be (false)
		UniversityId.isValid("O672089") should be (false)
	}

	@Test def zeroPad() {
		UniversityId.zeroPad("12345678") should be ("12345678")
		UniversityId.zeroPad("0123456") should be ("0123456")
		UniversityId.zeroPad("123456") should be ("0123456")
		UniversityId.zeroPad("3456") should be ("0003456")
	}

}