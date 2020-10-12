package uk.ac.warwick.tabula

import org.scalatest.funsuite.AnyFunSuite

/**
  * Yeah, it's a test for a test class, what are you gonna do about it
  */
class WebsignonMethodsTest extends AnyFunSuite {

  test("Sign out failure should extract detail about who's signed in") {
    val input = "Blaviorwjiowefef\n\n\n\n      Signed in as tabula-functest-admin2 user    \n\n\n\n\t\t\tBlah"
    val output = WebsignonMethods.parseSignedInDetail(input)
    assert(output === "Signed in as tabula-functest-admin2 user")
  }

}
