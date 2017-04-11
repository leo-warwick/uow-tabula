package uk.ac.warwick.tabula.data

import org.hibernate.ScrollableResults

/**
 * Minimal implementation of ScrollableResults that we can use to mock up a Scrollable
 * object using a plain old sequence of things, instead of the database.
 */
class MockScrollableResults(seq: Seq[_ <: AnyRef]) extends ScrollableResults {
	val itr: Iterator[AnyRef] = seq.iterator

	def next(): Boolean = itr.hasNext
	def get(i: Int): AnyRef = if (i == 0) itr.next() else ???
	def get() = Array(itr.next())
	def close(): Unit = {}

	def getType(i: Int) = ???
	def getCharacter(col: Int) = ???
	def scroll(i: Int) = ???
	def getRowNumber = ???
	def getLocale(col: Int) = ???
	def beforeFirst() = ???
	def getTimeZone(col: Int) = ???
	def last() = ???
	def isLast = ???
	def getBinary(col: Int) = Array()
	def getDouble(col: Int) = ???
	def isFirst = ???
	def setRowNumber(rowNumber: Int) = ???
	def getClob(col: Int) = ???
	def getFloat(col: Int) = ???
	def getBigDecimal(col: Int) = ???
	def getLong(col: Int) = ???
	def getCalendar(col: Int) = ???
	def afterLast() = ???
	def getByte(col: Int) = ???
	def getBoolean(col: Int) = ???
	def getShort(col: Int) = ???
	def getBigInteger(col: Int) = ???
	def getInteger(col: Int) = ???
	def getDate(col: Int) = ???
	def getText(col: Int) = ???
	def previous() = ???
	def getBlob(col: Int) = ???
	def first() = ???
	def getString(col: Int) = ???

}
