package uk.ac.warwick.tabula.data

import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._

trait NormalCATSLoadDaoComponent {
	val normalCATSLoadDao: NormalCATSLoadDao
}

trait AutowiringNormalCATSLoadDaoComponent extends NormalCATSLoadDaoComponent {
	val normalCATSLoadDao: NormalCATSLoadDao = Wire[NormalCATSLoadDao]
}

trait NormalCATSLoadDao {
	def saveOrUpdate(load: NormalCATSLoad): Unit
	def delete(load: NormalCATSLoad): Unit
	def find(route: Route, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[NormalCATSLoad]
	def findAll(routes: Seq[Route], academicYear: AcademicYear): Seq[NormalCATSLoad]
}

@Repository
class NormalCATSLoadDaoImpl extends NormalCATSLoadDao with Daoisms {

	def saveOrUpdate(load: NormalCATSLoad): Unit =
		session.saveOrUpdate(load)

	def delete(load: NormalCATSLoad): Unit =
		session.delete(load)

	def find(route: Route, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[NormalCATSLoad] = {
		session.newCriteria[NormalCATSLoad]
			.add(is("route", route))
			.add(is("academicYear", academicYear))
			.add(is("yearOfStudy", yearOfStudy))
		  .uniqueResult
	}

	def findAll(routes: Seq[Route], academicYear: AcademicYear): Seq[NormalCATSLoad] = {
		safeInSeq[NormalCATSLoad](
			() => {
				session.newCriteria[NormalCATSLoad]
					.add(is("academicYear", academicYear))
			},
			"route",
			routes
		)
	}

}
