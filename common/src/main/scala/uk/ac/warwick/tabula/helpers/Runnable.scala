package uk.ac.warwick.tabula.helpers

import java.lang.{ Runnable => JRunnable }
import language.implicitConversions
import java.util.concurrent.Callable

/**
 * Wrapper for java.lang.Runnable that lets you pass in a function to
 * run, which is more concise.
 */
final class Runnable(f: => Unit) extends JRunnable {
	override def run() { f }
}

/**
 * Easy Runnable.
 * val task = Runnable { println("Yeah!") }
 */
object Runnable {
	def apply(f: => Unit) = new Runnable(f)

	// Import this if you want implicit conversion.
	implicit def ImplicitConversion(f: => Unit): JRunnable = Runnable(f)
}

object FunctionConversions {
	implicit def asGoogleFunction[A,B](f: (A) => B) = new com.google.common.base.Function[A,B] {
		override def apply(input: A): B = f(input)
	}

	implicit def asJavaCallable[B](f: => B) = new Callable[B] {
		override def call(): B = f
	}
}
