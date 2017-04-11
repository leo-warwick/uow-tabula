package uk.ac.warwick.tabula.helpers

import java.io.Closeable
import org.hibernate.{ScrollableResults, Session}
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import uk.ac.warwick.tabula.data.Scrollable

object Closeables {
	def ensureClose[T, C <: Closeable](c: C)(fn: => T): T = try fn finally c.close
	/**
	 * Same as #ensureClose but passes the Closeable to the callback, so you don't
	 * have to hold on to the variable yourself.
	 */
	def closeThis[T, C <: Closeable](c: C)(fn: (C) => T): T = try fn(c) finally c.close

	def closeThis[T](s: Session)(fn: (Session) => T): T = try fn(s) finally s.close()
	def closeThis[T](c: Connection)(fn: (Connection) => T): T = try fn(c) finally c.close()
	def closeThis[T](s: PreparedStatement)(fn: (PreparedStatement) => T): T = try fn(s) finally s.close()
	def closeThis[T](rs: ResultSet)(fn: (ResultSet) => T): T = try fn(rs) finally rs.close()
	def closeThis[T](s: ScrollableResults)(fn: (ScrollableResults) => T): T = try fn(s) finally s.close()

}