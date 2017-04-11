package uk.ac.warwick.tabula.system.exceptions

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import collection.JavaConverters._

class CompositeExceptionHandlerTest extends TestBase with Mockito {

	val listener1: ExceptionHandler = mock[ExceptionHandler]
	val listener2: ExceptionHandler = mock[ExceptionHandler]

	val handler = new CompositeExceptionHandler(Seq(listener1, listener2).asJava)

	@Test def itWorks {
		val context = ExceptionContext("1", new RuntimeException("An egg cracked"), Some(testRequest("https://tabula.warwick.ac.uk/web/power/flight?super=magic")))

		handler.exception(context)
		verify(listener1, times(1)).exception(context)
		verify(listener2, times(1)).exception(context)
	}

}