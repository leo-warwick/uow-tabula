package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.{AutowiringRouteDaoComponent, RouteDaoComponent, Daoisms}
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet

trait RouteServiceComponent {
	def routeService: RouteService
}

trait AutowiringRouteServiceComponent extends RouteServiceComponent {
	var routeService = Wire[RouteService]
}

trait RouteService {
	def saveOrUpdate(route: Route)
	def getByCode(code: String): Option[Route]
	def findMonitoringPointSet(route: Route, year: Option[Int]): Option[MonitoringPointSet]
}

abstract class AbstractRouteService extends RouteService {
	self: RouteDaoComponent =>

	def saveOrUpdate(route: Route) = routeDao.saveOrUpdate(route)
	def getByCode(code: String): Option[Route] = routeDao.getByCode(code)
	def findMonitoringPointSet(route: Route, year: Option[Int]) = routeDao.findMonitoringPointSet(route, year)
}

@Service("routeService")
class RouteServiceImpl 
	extends AbstractRouteService
		with AutowiringRouteDaoComponent