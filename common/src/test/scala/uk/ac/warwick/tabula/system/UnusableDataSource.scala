package uk.ac.warwick.tabula.system

import javax.sql.DataSource
import org.springframework.jdbc.datasource.AbstractDataSource

/**
  Placeholder for a data source that we expect never to be used during tests,
  such as an external database which we can't easily reproduce in a test environment.
*/
class UnusableDataSource extends AbstractDataSource {
  override def getConnection() = throw new UnsupportedOperationException("This is not a real data source!")
  override def getConnection(u:String, p:String): Nothing = getConnection()
}
