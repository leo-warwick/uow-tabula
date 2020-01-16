package uk.ac.warwick.tabula.data

import org.hibernate.Session
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, Route, RouteTeachingInformation}

trait RouteDaoComponent {
  val routeDao: RouteDao
}

trait AutowiringRouteDaoComponent extends RouteDaoComponent {
  val routeDao: RouteDao = Wire[RouteDao]
}

trait RouteDao {
  def allRoutes: Seq[Route]

  def saveOrUpdate(route: Route): Unit

  def getByCode(code: String): Option[Route]

  def getAllByCodes(codes: Seq[String]): Seq[Route]

  def getById(id: String): Option[Route]

  def findByDepartment(department: Department): Seq[Route]

  def findActiveByDepartment(department: Department): Seq[Route]

  def stampMissingRows(dept: Department, seenCodes: Seq[String]): Int

  def findRoutesNamedLike(query: String): Seq[Route]

  def saveOrUpdate(teachingInfo: RouteTeachingInformation): Unit

  def delete(teachingInfo: RouteTeachingInformation): Unit

  def getTeachingInformationByRouteCodeAndDepartmentCode(routeCode: String, departmentCode: String): Option[RouteTeachingInformation]

  def findActiveBySITSDepartmentCode(departmentCode: String): Seq[Route]
}

@Repository
class RouteDaoImpl extends RouteDao with Daoisms {

  def allRoutes: Seq[Route] =
    session.newCriteria[Route]
      .addOrder(Order.asc("code"))
      .seq
      .distinct

  def saveOrUpdate(route: Route): Unit = session.saveOrUpdate(route)

  def getByCode(code: String): Option[Route] =
    session.newQuery[Route]("from Route r where code = :code").setString("code", code).uniqueResult


  def getAllByCodes(codes: Seq[String]): Seq[Route] = {
    safeInSeq(() => {
      session.newCriteria[Route]
    }, "code", codes)
  }

  def getById(id: String): Option[Route] = getById[Route](id)

  def findByDepartment(department: Department): Seq[Route] =
    session.newQuery[Route]("from Route r where adminDepartment = :dept").setEntity("dept", department).seq

  def findActiveByDepartment(department: Department): Seq[Route] =
    session.newQuery[Route]("from Route r where adminDepartment = :dept and active = true").setEntity("dept", department).seq

  def stampMissingRows(dept: Department, seenCodes: Seq[String]): Int = {
    val hql =
      """
				update Route r
				set
					r.missingFromImportSince = :now
				where
					r.adminDepartment = :department and
					r.missingFromImportSince is null
		"""

    val query =
      if (seenCodes.isEmpty) session.newUpdateQuery(hql)
      else session.newUpdateQuery(hql + " and r.code not in (:seenCodes)").setParameterList("seenCodes", seenCodes)

    query
      .setParameter("now", DateTime.now)
      .setEntity("department", dept)
      .executeUpdate()
  }

  def findRoutesNamedLike(query: String): Seq[Route] = {
    session.newCriteria[Route]
      .add(disjunction()
        .add(likeIgnoreCase("code", s"%${query.toLowerCase}%"))
        .add(likeIgnoreCase("name", s"%${query.toLowerCase}%"))
      )
      .setMaxResults(20).seq.sorted(Route.DegreeTypeOrdering)
  }

  def saveOrUpdate(teachingInfo: RouteTeachingInformation): Unit = session.saveOrUpdate(teachingInfo)

  def delete(teachingInfo: RouteTeachingInformation): Unit = session.delete(teachingInfo)

  def getTeachingInformationByRouteCodeAndDepartmentCode(routeCode: String, departmentCode: String): Option[RouteTeachingInformation] =
    session.newCriteria[RouteTeachingInformation]
      .createAlias("route", "route")
      .createAlias("department", "department")
      .add(is("route.code", routeCode.toLowerCase()))
      .add(is("department.code", departmentCode.toLowerCase()))
      .uniqueResult

  def findActiveBySITSDepartmentCode(departmentCode: String): Seq[Route] =
    session.newCriteria[Route]
      .add(is("sitsDepartmentCode", departmentCode))
      .add(is("active", true))
      .seq

}