package uk.ac.warwick.tabula.system

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.InitializingBean
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.Closeables._
import org.springframework.beans.factory.BeanInitializationException
import org.hibernate.SessionFactory
import javax.sql.DataSource
import org.hibernate.`type`.StringType

/**
  * Selects a Unicode string from the database to see if it gets there and
  * back again in one piece. It doesn't store it anywhere, so it's just a
  * check of the database connection, not whether you're using a Unicode-safe
  * column type.
  */
trait DatabaseEncodingChecker extends InitializingBean with Logging {

  val testString = "a-\u01ee"

  def fetchString: String

  override def afterPropertiesSet(): Unit = {
    val fetchedString = fetchString

    if (testString != fetchedString) {
      throw new BeanInitializationException("Database connection is ruining strings - expected " + testString + ", got " + fetchedString)
    } else {
      logger.debug("Retrieved Unicode string from database in one piece, international characters should be okay")
    }
  }

}

class SessionFactoryDatabaseEncodingChecker @Autowired()(val sessionFactory: SessionFactory) extends DatabaseEncodingChecker {

  override def fetchString: String = closeThis(sessionFactory.openSession()) { session =>
    val query = session.createSQLQuery("select :string")
    query.setParameter("string", testString, StringType.INSTANCE)
    query.uniqueResult().toString
  }

}

class DataSourceDatabaseEncodingChecker @Autowired()(val dataSource: DataSource) extends DatabaseEncodingChecker {

  override def fetchString: String = closeThis(dataSource.getConnection) { conn =>
    closeThis(conn.prepareStatement("select ?")) { stmt =>
      stmt.setString(1, testString)
      closeThis(stmt.executeQuery()) { rs =>
        rs.next()
        rs.getString(1)
      }
    }
  }

}