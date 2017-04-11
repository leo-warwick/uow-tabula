package uk.ac.warwick.tabula

import org.junit.Test

class CompactUuidTest extends TestBase {
	@Test def compacting {
		val uuid    = "16164939-101f-427b-ae87-d237c127843e"
		val compact = "FhZJORAfQnuuh9I3wSeEPg"

		CompactUuid.compact(uuid) should be (Some(compact))
		CompactUuid.compact("Invalid stuff") should be (None)

		CompactUuid.uncompact(compact) should be (Some(uuid))
		CompactUuid.uncompact(compact+"9") should be (None)
	}
}
