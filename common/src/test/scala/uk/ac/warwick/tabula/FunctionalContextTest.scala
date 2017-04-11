package uk.ac.warwick.tabula

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.FunctionalContextTest._
import org.springframework.beans.factory.NoSuchBeanDefinitionException

/**
 * Just a test of the FunctionalContextTesting trait, which allows for
 * quickly defining an application context in Scala and running some test
 * code within that context, having Wire pick up dependencies from it.
 *
 * Arguably this is enabling a continuation of rampant dependency grabbing
 * from across the app which does not help with testing.
 *
 * Any bean(){} definitions should be super simple - if there is any amount
 * of boilerplate initialisation of a test dependency, it should be broken
 * out into a reusable method somewhere (like some of the stuff in TestFixtures,
 * which should probably be exposed as an object.)
 */
class FunctionalContextTest extends TestBase with FunctionalContextTesting with Mockito {

	@Test
	def contextAllowsWire() {

		// Create a context based on the Ctx class defined below.
		inContext[Ctx] {
			val wired = new WiredBean
			wired.mystring should be("cool string")
			wired.thisYear should be(new AcademicYear(2012))

			val service = Wire[StringService]
			service.resolve("egg") should be("chicken")
			verify(service, times(1)).resolve("egg")
		}

		// Check that the context is all gone away after inContext.
		try {
			val unwired = new WiredBean
			unwired.mystring should be(null)
		} catch {
			case _: NoSuchBeanDefinitionException =>
				// fine, just means an existing Spring app ctx is in place now.
		}

	}

}

object FunctionalContextTest {

	class WiredBean {
		val mystring: String = Wire[String]("mystring")
		val thisYear: AcademicYear = Wire[AcademicYear]
	}

	trait StringService {
		def resolve(key: String): String
	}

	class Ctx extends FunctionalContext with Mockito {
		bean("mystring") {
			"cool string"
		}
		bean() {
			new AcademicYear(olympicYear())
		}
		bean("mockStringService") {
			val s = mock[StringService]
			s.resolve("egg") returns ("chicken")
			s
		}
		val olympicYear = bean() {
			2012
		}
	}


}