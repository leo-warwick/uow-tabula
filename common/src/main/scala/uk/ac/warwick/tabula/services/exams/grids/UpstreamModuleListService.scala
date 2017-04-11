package uk.ac.warwick.tabula.services.exams.grids

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.UpstreamModuleList
import uk.ac.warwick.tabula.data.{AutowiringUpstreamModuleListDaoComponent, UpstreamModuleListDaoComponent}

trait UpstreamModuleListService {

	def save(list: UpstreamModuleList): Unit
	def saveOrUpdate(list: UpstreamModuleList): Unit
	def countAllModuleLists: Int
	def listModuleLists(start: Int, limit: Int): Seq[UpstreamModuleList]
	def findByCodes(codes: Seq[String]): Seq[UpstreamModuleList]

}

abstract class AbstractUpstreamModuleListService extends UpstreamModuleListService {

	self: UpstreamModuleListDaoComponent =>

	def save(list: UpstreamModuleList): Unit =
		upstreamModuleListDao.save(list)

	def saveOrUpdate(list: UpstreamModuleList): Unit =
		upstreamModuleListDao.saveOrUpdate(list)

	def countAllModuleLists: Int =
		upstreamModuleListDao.countAllModuleLists

	def listModuleLists(start: Int, limit: Int): Seq[UpstreamModuleList] =
		upstreamModuleListDao.listModuleLists(start, limit)

	def findByCodes(codes: Seq[String]): Seq[UpstreamModuleList] =
		upstreamModuleListDao.findByCodes(codes)

}

@Service("upstreamModuleListService")
class UpstreamModuleListServiceImpl
	extends AbstractUpstreamModuleListService
		with AutowiringUpstreamModuleListDaoComponent

trait UpstreamModuleListServiceComponent {
	def upstreamModuleListService: UpstreamModuleListService
}

trait AutowiringUpstreamModuleListServiceComponent extends UpstreamModuleListServiceComponent {
	var upstreamModuleListService: UpstreamModuleListService = Wire[UpstreamModuleListService]
}
