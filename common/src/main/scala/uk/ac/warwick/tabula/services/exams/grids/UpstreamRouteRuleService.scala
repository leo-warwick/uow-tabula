package uk.ac.warwick.tabula.services.exams.grids

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Route, UpstreamRouteRule}
import uk.ac.warwick.tabula.data.{AutowiringUpstreamRouteRuleDaoComponent, UpstreamRouteRuleDaoComponent}

trait UpstreamRouteRuleService {

	def saveOrUpdate(list: UpstreamRouteRule): Unit
	def removeAll(): Unit
	def list(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[UpstreamRouteRule]

}

abstract class AbstractUpstreamRouteRuleService extends UpstreamRouteRuleService {

	self: UpstreamRouteRuleDaoComponent =>

	def saveOrUpdate(list: UpstreamRouteRule): Unit =
		upstreamRouteRuleDao.saveOrUpdate(list)

	def removeAll(): Unit =
		upstreamRouteRuleDao.removeAll()

	def list(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[UpstreamRouteRule] =
		upstreamRouteRuleDao.list(route, academicYear, yearOfStudy)

}

@Service("upstreamRouteRuleService")
class UpstreamRouteRuleServiceImpl
	extends AbstractUpstreamRouteRuleService
		with AutowiringUpstreamRouteRuleDaoComponent

trait UpstreamRouteRuleServiceComponent {
	def upstreamRouteRuleService: UpstreamRouteRuleService
}

trait AutowiringUpstreamRouteRuleServiceComponent extends UpstreamRouteRuleServiceComponent {
	var upstreamRouteRuleService: UpstreamRouteRuleService = Wire[UpstreamRouteRuleService]
}
