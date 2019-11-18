package uk.ac.warwick.tabula.web

import uk.ac.warwick.tabula.TestBase
import javax.servlet.http
import org.springframework.mock.web.MockHttpServletResponse
import language.reflectiveCalls

class CookiesTest extends TestBase {

  @Test
  def cookieValues(): Unit = {
    val array = Array[http.Cookie](
      new http.Cookie("name", "Billy"),
      new http.Cookie("isGood", "true"),
      new http.Cookie("isNasty", "false"),
      new http.Cookie("isNonsense", "blahblah")
    )

    val cookies = new Cookies(array)
    cookies.getString("name") should be(Some("Billy"))
    cookies.getString("wasd") should be(None)
    cookies.getBoolean("isGood", default = false) should be(true)
    cookies.getBoolean("isNasty", default = false) should be(false)
    cookies.getBoolean("isNonsense", default = false) should be(false)
  }

  @Test
  def magicCookie(): Unit = {
    val httpCookie = new http.Cookie("name", "Billy")
    val magicCookie = new Cookie(httpCookie)

    magicCookie.path should be(null)
    magicCookie.value should be("Billy")

    magicCookie.path = "/path"
    magicCookie.value = "myvalue"

    httpCookie.getPath should be("/path")
    httpCookie.getValue should be("myvalue")
  }

  /* Request can have a null array of cookies if the user agent sends no header */
  @Test
  def nullArray(): Unit = {
    val cookies = new Cookies(null)
    cookies.getString("name") should be(None)
    cookies.getCookie("name") should be(None)
    cookies.getBoolean("a", default = false) should be(false)
    cookies.getBoolean("a", default = true) should be(true)
  }

  @Test def constructor(): Unit = {
    val cookie1 = new Cookie("name", "something")
    cookie1.cookie.getName should be("name")
    cookie1.cookie.getValue should be("something")
    cookie1.cookie.getPath should be(null)

    val cookie2 = new Cookie("name", "something", path = "/yes")
    cookie2.cookie.getName should be("name")
    cookie2.cookie.getValue should be("something")
    cookie2.cookie.getPath should be("/yes")
  }

  @Test def implicitResponse(): Unit = {
    import Cookies._

    val response = new MockHttpServletResponse
    response.addCookie(new Cookie("name", "something"))

    response.getCookie("name").getValue should be("something")
  }

}
