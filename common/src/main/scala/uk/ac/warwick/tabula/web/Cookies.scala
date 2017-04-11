package uk.ac.warwick.tabula.web

import javax.servlet.http.HttpServletResponse
import javax.servlet.http

import language.implicitConversions

/**
 * A Scala-ish wrapper for servlet Cookie.
 */
class Cookie(val cookie: http.Cookie) {
	def this(name: String, value: String, path: String = null) = {
		this(new http.Cookie(name, value))
		if (path != null) {
			this.path = path
		}
	}

	def path_=(p: String): Unit = { cookie.setPath(p) }
	def path: String = cookie.getPath

	def value: String = cookie.getValue
	def value_=(value: String): Unit = cookie.setValue(value)
}

/**
 * Scala-ish wrapper for the array of servlet Cookies from a request.
 */
class Cookies(val _cookies: Array[http.Cookie]) {
	lazy val cookies: Array[http.Cookie] = if (_cookies == null) Array.empty[http.Cookie] else _cookies
	def getCookie(name: String): Option[Cookie] = wrap(cookies.find { _.getName == name })
	def getString(name: String): Option[String] = getCookie(name) match {
		case Some(cookie) => Some(cookie.value)
		case None => None
	}
	def getBoolean(name: String, default: Boolean): Boolean = getCookie(name) match {
		case Some(cookie) => try {
			cookie.value.toBoolean
		} catch {
			case _ @ (_: NumberFormatException | _: IllegalArgumentException) => default
		}
		case None => default
	}
	private def wrap(cookie: Option[http.Cookie]): Option[Cookie] = cookie match {
		case Some(cookie) => Some(new Cookie(cookie))
		case None => None
	}
}

/**
 * Defines implicit conversions from boring cookie arrays to nice useful Cookies objects,
 * and adds implicit methods to HttpServletResponse to support adding our Cookie class.
 */
object Cookies {
	implicit def toMagicCookies(cookies: Array[http.Cookie]) = new Cookies(cookies)
	implicit class CookieMethods(response: HttpServletResponse) {
		def addCookie(cookie: Cookie): Unit = response.addCookie(cookie.cookie)
	}
}