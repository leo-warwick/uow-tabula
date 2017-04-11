package uk.ac.warwick.tabula.data


import org.hibernate.criterion.Order
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model._

trait UpstreamModuleListDaoComponent {
	val upstreamModuleListDao: UpstreamModuleListDao
}

trait AutowiringUpstreamModuleListDaoComponent extends UpstreamModuleListDaoComponent {
	val upstreamModuleListDao: UpstreamModuleListDao = Wire[UpstreamModuleListDao]
}

trait UpstreamModuleListDao {
	def save(list: UpstreamModuleList): Unit
	def saveOrUpdate(list: UpstreamModuleList): Unit
	def findByCode(code: String): Option[UpstreamModuleList]
	def findByCodes(codes: Seq[String]): Seq[UpstreamModuleList]
	def countAllModuleLists: Int
	def listModuleLists(start: Int, limit: Int): Seq[UpstreamModuleList]
}

@Repository
class UpstreamModuleListDaoImpl extends UpstreamModuleListDao with Daoisms {

	def save(list: UpstreamModuleList): Unit =
		findByCode(list.code).getOrElse { session.save(list) }

	def saveOrUpdate(list: UpstreamModuleList): Unit =
		session.saveOrUpdate(list)

	def findByCode(code: String): Option[UpstreamModuleList] = {
		getById[UpstreamModuleList](code)
	}

	def findByCodes(codes: Seq[String]): Seq[UpstreamModuleList] = {
		safeInSeq(
			() => session.newCriteria[UpstreamModuleList],
			"code",
			codes
		)
	}

	def countAllModuleLists: Int = {
		session.newCriteria[UpstreamModuleList].count.intValue()
	}

	def listModuleLists(start: Int, limit: Int): Seq[UpstreamModuleList] = {
		session.newCriteria[UpstreamModuleList]
			.addOrder(Order.asc("code"))
			.setFirstResult(start)
		  .setMaxResults(limit)
		  .seq
	}

}
