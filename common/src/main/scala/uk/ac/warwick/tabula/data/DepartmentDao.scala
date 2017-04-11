package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.Department
import org.hibernate.criterion.Order
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.helpers.StringUtils._

trait DepartmentDao {
	def allDepartments: Seq[Department]
	def allRootDepartments: Seq[Department]
	def getByCode(code: String): Option[Department]
	def getById(id: String): Option[Department]
	def saveOrUpdate(department: Department)
}

@Repository
class DepartmentDaoImpl extends DepartmentDao with Daoisms {

	def allDepartments: Seq[Department] =
		session.newCriteria[Department]
			.addOrder(Order.asc("code"))
			.list
			.distinct

	def allRootDepartments: Seq[Department] =
		allDepartments.filterNot(_.hasParent)

	// Fetches modules eagerly
	def getByCode(code: String): Option[Department] =	code.maybeText.flatMap { code =>
		session.newQuery[Department]("from Department d left join fetch d.modules where d.code = :code")
			.setString("code", code.toLowerCase())
			.uniqueResult
	}

	def getById(id: String): Option[Department] = getById[Department](id)

	def saveOrUpdate(department: Department): Unit = session.saveOrUpdate(department)

}