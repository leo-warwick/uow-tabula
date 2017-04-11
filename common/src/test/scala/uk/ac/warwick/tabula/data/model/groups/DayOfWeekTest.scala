package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.TestBase


class DayOfWeekTest extends TestBase {
	@Test
	def find() {
		DayOfWeek.find(3) should be (Some(DayOfWeek.Wednesday))
		DayOfWeek.find(9) should be (None)
	}

	@Test
	def applyValid() {
		DayOfWeek(3) should be (DayOfWeek.Wednesday)
	}

	@Test(expected=classOf[IllegalArgumentException])
	def applyInvalid() {
		DayOfWeek(0)
		fail("No exception")
	}

	@Test
	def equality() {
		DayOfWeek.Tuesday should be (DayOfWeek.Tuesday)
		DayOfWeek.Tuesday should not be (DayOfWeek.Wednesday)
	}
}
