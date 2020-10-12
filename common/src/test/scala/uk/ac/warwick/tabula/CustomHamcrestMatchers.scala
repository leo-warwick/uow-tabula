package uk.ac.warwick.tabula

import org.hamcrest.{BaseMatcher, Description}
import org.scalatest.matchers.should.Matchers._

/**
  * Some custom Hamcrest matchers. Usually Scalatest matchers are fine,
  * but in some cases Hamcrest matchers are better, such as when matching
  * the expected args on a Mockito mock.
  */
trait CustomHamcrestMatchers {

  /**
    * Matches a named property with an expected value. Hamcrest has one of these
    * included but it only understands Javabeans. This uses a Scalatest matcher under the hood.
    *
    * {{{
    * there was one(service).save(argThat(hasProperty('code, "XYZ123"))
    * }}}
    */
  class HasPropertyMatcher[A](name: Symbol, value: Any) extends BaseMatcher[A] {
    def matches(obj: Object): Boolean = {
      // Uses the Scalatest Symbol->Matcher conversion internally
      have(name(value)).apply(obj).matches
    }

    def describeTo(d: Description): Unit = {
      d.appendText("Has property ").appendValue(name.name).appendText(" with value ").appendValue(value)
    }
  }

  def hasProperty[A](name: Symbol, value: AnyRef) = new HasPropertyMatcher[A](name, value)

}

// Use as a static import (recommended)
object CustomHamcrestMatchers extends CustomHamcrestMatchers
