package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.spring.Wire

trait SitsStatusDaoComponent {
	val sitsStatusDao: SitsStatusDao
}

trait AutowiringSitsStatusDaoComponent extends SitsStatusDaoComponent {
	val sitsStatusDao: SitsStatusDao = Wire[SitsStatusDao]
}

trait SitsStatusDao {
	def saveOrUpdate(sitsStatus: SitsStatus)
	def getByCode(code: String): Option[SitsStatus]
	def getAllStatusCodes: Seq[String]
	def getFullName(code: String): Option[String]
}

@Repository
class SitsStatusDaoImpl extends SitsStatusDao with Daoisms {

	def saveOrUpdate(sitsStatus: SitsStatus): Unit = session.saveOrUpdate(sitsStatus)

	def getByCode(code: String): Option[SitsStatus] =
		session.newQuery[SitsStatus]("from SitsStatus sitsStatus where code = :code").setString("code", code).uniqueResult

	def getAllStatusCodes: Seq[String] =
		session.newQuery[String]("select distinct code from SitsStatus").seq

	def getFullName(code: String): Option[String] =
		session.newQuery[String]("select fullName from SitsStatus where code = :code").setString("code", code).uniqueResult
}
