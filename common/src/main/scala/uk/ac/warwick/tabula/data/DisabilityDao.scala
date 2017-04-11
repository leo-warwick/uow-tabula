package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Disability


trait DisabilityDaoComponent {
	val disabilityDao: DisabilityDao
}

trait AutowiringDisabilityDaoComponent extends DisabilityDaoComponent {
	val disabilityDao: DisabilityDao = Wire[DisabilityDao]
}

trait DisabilityDao {
	def saveOrUpdate(disability: Disability)
	def getByCode(code: String): Option[Disability]
	def getAllDisabilityCodes: Seq[String]
}

@Repository
class DisabilityDaoImpl extends DisabilityDao with Daoisms {

	def saveOrUpdate(disability: Disability): Unit = session.saveOrUpdate(disability)

	def getByCode(code: String): Option[Disability] =
		session.newQuery[Disability]("from Disability disability where code = :code").setString("code", code).uniqueResult

	def getAllDisabilityCodes: Seq[String] =
		session.newQuery[String]("select distinct code from Disability").seq
}
