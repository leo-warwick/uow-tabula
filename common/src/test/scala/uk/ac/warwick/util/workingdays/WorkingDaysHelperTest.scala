package uk.ac.warwick.util.workingdays

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

import scala.collection.JavaConverters._

class WorkingDaysHelperTest extends TestBase {

	/**
		* This is a copy of the WorkingDaysHelperTest in Warwick Utils. If it starts failing, but the latest
		* Warwick Utils tests pass, you probably just need to update to the latest Warwick Utils.
		*/
	@Test
	def enoughDates(): Unit = {
		val helper = new WorkingDaysHelperImpl()
		val dates = helper.getHolidayDates.asScala.toSeq.sorted

		dates.last should be >= LocalDate.now.plusMonths(4)
	}

}
