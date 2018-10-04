package uk.ac.warwick.tabula.services

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.SyllabusPlusLocation
import uk.ac.warwick.tabula.data.{AutowiringSyllabusPlusLocationDaoComponent, SyllabusPlusLocationDaoComponent}

trait SyllabusPlusLocationServiceComponent {
	def syllabusPlusLocationService: SyllabusPlusLocationService
}

trait AutowiringSyllabusPlusLocationServiceComponent extends SyllabusPlusLocationServiceComponent {
	var syllabusPlusLocationService: SyllabusPlusLocationService = Wire[SyllabusPlusLocationService]
}

trait SyllabusPlusLocationService {
	def delete(location: SyllabusPlusLocation): Unit

	def save(location: SyllabusPlusLocation): Unit

	def all(): Seq[SyllabusPlusLocation]

	def getByUpstreamName(upstreamName: String): Option[SyllabusPlusLocation]

	def getById(id: String): Option[SyllabusPlusLocation]
}

abstract class AbstractSyllabusPlusLocationService extends SyllabusPlusLocationService {
	self: SyllabusPlusLocationDaoComponent =>

	override def delete(location: SyllabusPlusLocation): Unit = syllabusPlusLocationDao.delete(location)

	override def save(location: SyllabusPlusLocation): Unit = syllabusPlusLocationDao.saveOrUpdate(location)

	override def all(): Seq[SyllabusPlusLocation] = syllabusPlusLocationDao.getAll()

	override def getByUpstreamName(upstreamName: String): Option[SyllabusPlusLocation] = syllabusPlusLocationDao.getByUpstreamName(upstreamName)

	override def getById(id: String): Option[SyllabusPlusLocation] = syllabusPlusLocationDao.getById(id)
}

@Service("syllabusPlusLocationService")
class SyllabusPlusLocationServiceImpl extends AbstractSyllabusPlusLocationService
	with AutowiringSyllabusPlusLocationDaoComponent
