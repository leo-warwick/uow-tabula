package uk.ac.warwick.tabula

import org.springframework.scala.context.function.{FunctionalConfigApplicationContext, FunctionalConfiguration}
import uk.ac.warwick.spring.{Wire, SpringConfigurer}
import scala.util.Try

abstract class FunctionalContext extends FunctionalConfiguration {
	bean[SpringConfigurer](name="springConfigurer") {
		new SpringConfigurer()
	}
}

trait FunctionalContextTesting {

	def inContext[A <: FunctionalContext : Manifest](fn: => Unit) {

		val oldAppCtx = SpringConfigurer.applicationContext
		val oldBeanConfig = SpringConfigurer.beanConfigurer

		def use(ctx: FunctionalConfigApplicationContext, fn: => Unit) {
			// Try to restore any previous setup by Spring, as the standard Spring test context
			// will assume nothing's messed up its beans as long as @DirtiesContext isn't used
			// (which we avoid because it is expensive to recreate an app context every time)
			try {
				fn
			} finally {
				ctx.destroy()
				SpringConfigurer.applicationContext = oldAppCtx
				SpringConfigurer.beanConfigurer = oldBeanConfig
			}
		}

		use(FunctionalConfigApplicationContext[A](), fn)
	}

}
