package uk.ac.warwick.tabula.sandbox

import org.joda.time.DateTime
import uk.ac.warwick.util.core.sql.{AbstractResultSet, AbstractResultSetMetaData}
import uk.ac.warwick.tabula.JavaImports.JBigDecimal

class MapResultSet(map: Map[String, _]) extends AbstractResultSet {

	override def getString(columnLabel: String): String = getObject(columnLabel, classOf[String])
	override def getInt(columnLabel: String): Int = getObject(columnLabel, classOf[Int])
	override def getDate(columnLabel: String) = new java.sql.Date(getObject(columnLabel, classOf[DateTime]).getMillis)
	override def getBigDecimal(columnLabel: String): _root_.uk.ac.warwick.tabula.JavaImports.JBigDecimal = getObject(columnLabel, classOf[JBigDecimal])

	override def getObject[A](columnLabel: String, objectType: Class[A]): A = map(columnLabel).asInstanceOf[A]

	override def getMetaData: AbstractResultSetMetaData = {
		val columns = map.keys.toSeq

		new AbstractResultSetMetaData {
			override def getColumnCount: Int = columns.size
			override def getColumnName(index: Int): String = columns(index - 1)
		}
	}

}