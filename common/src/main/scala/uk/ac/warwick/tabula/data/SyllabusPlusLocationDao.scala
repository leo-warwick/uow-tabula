package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.SyllabusPlusLocation

trait SyllabusPlusLocationDaoComponent {
	val syllabusPlusLocationDao: SyllabusPlusLocationDao
}

trait AutowiringSyllabusPlusLocationDaoComponent extends SyllabusPlusLocationDaoComponent {
	val syllabusPlusLocationDao: SyllabusPlusLocationDao = Wire[SyllabusPlusLocationDao]
}

trait SyllabusPlusLocationDao {
	def getAll(): Seq[SyllabusPlusLocation]

	def getByUpstreamName(upstreamName: String): Option[SyllabusPlusLocation]

	def getById(id: String): Option[SyllabusPlusLocation]

	def saveOrUpdate(location: SyllabusPlusLocation)

	def delete(location: SyllabusPlusLocation): Unit
}

@Repository
class SyllabusPlusLocationDaoImpl extends SyllabusPlusLocationDao with Daoisms {
	override def getAll(): Seq[SyllabusPlusLocation] =
		session.newCriteria[SyllabusPlusLocation]
			.addOrder(Order.asc("upstreamName"))
			.seq

	def getByUpstreamName(upstreamName: String): Option[SyllabusPlusLocation] =
		session.newCriteria[SyllabusPlusLocation]
			.add(is("upstreamName", upstreamName))
			.uniqueResult

	override def getById(id: String): Option[SyllabusPlusLocation] =
		getById[SyllabusPlusLocation](id)

	def saveOrUpdate(location: SyllabusPlusLocation): Unit = session.saveOrUpdate(location)

	def delete(location: SyllabusPlusLocation): Unit = session.delete(location)
}
