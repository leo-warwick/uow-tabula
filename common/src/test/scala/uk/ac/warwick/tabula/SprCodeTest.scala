package uk.ac.warwick.tabula

import org.junit.Test




class SprCodeTest extends TestBase {
	@Test def toId {
		SprCode.getUniversityId("0672088/1") should be ("0672088")
		SprCode.getUniversityId("0672088") should be ("0672088")
	}
}