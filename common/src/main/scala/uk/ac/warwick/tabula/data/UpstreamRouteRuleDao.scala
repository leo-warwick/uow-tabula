package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Restrictions
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

trait UpstreamRouteRuleDaoComponent {
	val upstreamRouteRuleDao: UpstreamRouteRuleDao
}

trait AutowiringUpstreamRouteRuleDaoComponent extends UpstreamRouteRuleDaoComponent {
	val upstreamRouteRuleDao: UpstreamRouteRuleDao = Wire[UpstreamRouteRuleDao]
}

trait UpstreamRouteRuleDao {
	def saveOrUpdate(list: UpstreamRouteRule): Unit
	def list(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[UpstreamRouteRule]
	def removeAll(): Unit
}

@Repository
class UpstreamRouteRuleDaoImpl extends UpstreamRouteRuleDao with Daoisms {

	def saveOrUpdate(list: UpstreamRouteRule): Unit =
		session.saveOrUpdate(list)

	def list(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[UpstreamRouteRule] = {
		session.newCriteria[UpstreamRouteRule]
			.add(is("route", route))
			.add(Restrictions.disjunction()
				.add(is("_academicYear", academicYear))
				.add(is("_academicYear", null))
			).add(is("yearOfStudy", yearOfStudy))
			.seq
	}

	def removeAll(): Unit = {
		session.createSQLQuery("delete from UpstreamRouteRuleEntry").executeUpdate()
		session.createSQLQuery("delete from UpstreamRouteRule").executeUpdate()
	}

}
