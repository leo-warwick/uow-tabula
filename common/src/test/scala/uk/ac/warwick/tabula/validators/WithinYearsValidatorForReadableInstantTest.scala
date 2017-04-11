package uk.ac.warwick.tabula.validators

import uk.ac.warwick.tabula.TestBase
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants

class WithinYearsValidatorForReadableInstantTest extends TestBase {

	@WithinYears(maxFuture = 2)
	var futureTest: DateTime = _

	val now = new DateTime(2013, DateTimeConstants.DECEMBER, 1, 12, 18, 9, 0)

	@Test def maxFuture { withFakeTime(now) {
		val a = classOf[WithinYearsValidatorForReadableInstantTest].getDeclaredField("futureTest").getAnnotation(classOf[WithinYears])

		val validator = new WithinYearsValidatorForReadableInstant()
		validator.initialize(a)

		validator.isValid(now, null) should be (true)
		validator.isValid(now.plusYears(1), null) should be (true)
		validator.isValid(now.plusYears(2), null) should be (true)
		validator.isValid(now.plusYears(3).minusDays(1), null) should be (true) // less than 3 full years
		validator.isValid(now.plusYears(3), null) should be (false)

		validator.isValid(now.minusYears(1), null) should be (true)
		validator.isValid(now.minusYears(2), null) should be (true)
		validator.isValid(now.minusYears(3), null) should be (true)
		validator.isValid(now.minusYears(300), null) should be (true)

		validator.isValid(null, null) should be (true)
	}}

	@WithinYears(maxPast = 2)
	var pastTest: DateTime = _

	@Test def maxPast { withFakeTime(now) {
		val a = classOf[WithinYearsValidatorForReadableInstantTest].getDeclaredField("pastTest").getAnnotation(classOf[WithinYears])

		val validator = new WithinYearsValidatorForReadableInstant()
		validator.initialize(a)

		validator.isValid(now, null) should be (true)
		validator.isValid(now.minusYears(1), null) should be (true)
		validator.isValid(now.minusYears(2), null) should be (true)
		validator.isValid(now.minusYears(3).plusDays(1), null) should be (true) // less than 3 full years
		validator.isValid(now.minusYears(3), null) should be (false)

		validator.isValid(now.plusYears(1), null) should be (true)
		validator.isValid(now.plusYears(2), null) should be (true)
		validator.isValid(now.plusYears(3), null) should be (true)
		validator.isValid(now.plusYears(300), null) should be (true)

		validator.isValid(null, null) should be (true)
	}}

	@WithinYears(maxFuture = 0, maxPast = 0) // must be within 1 year
	var withinYear: DateTime = _

	@Test def equal { withFakeTime(now) {
		val a = classOf[WithinYearsValidatorForReadableInstantTest].getDeclaredField("withinYear").getAnnotation(classOf[WithinYears])

		val validator = new WithinYearsValidatorForReadableInstant()
		validator.initialize(a)

		validator.isValid(now, null) should be (true)
		validator.isValid(now.plusYears(1).minusDays(1), null) should be (true)
		validator.isValid(now.plusYears(1), null) should be (false)
		validator.isValid(now.minusYears(1).plusDays(1), null) should be (true)
		validator.isValid(now.minusYears(1), null) should be (false)

		validator.isValid(null, null) should be (true)
	}}

}