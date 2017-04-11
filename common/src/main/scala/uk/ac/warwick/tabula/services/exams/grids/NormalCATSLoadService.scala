package uk.ac.warwick.tabula.services.exams.grids

import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{NormalCATSLoad, Route}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.{AutowiringNormalCATSLoadDaoComponent, NormalCATSLoadDaoComponent}

import scala.collection.mutable

class NormalLoadLookup(academicYear: AcademicYear, yearOfStudy: YearOfStudy, normalCATSLoadService: NormalCATSLoadService) {
	private val cache = mutable.Map[Route, Option[BigDecimal]]()
	def withoutDefault(route: Route): Option[BigDecimal] = cache.get(route) match {
		case Some(option) =>
			option
		case _ =>
			cache.put(route, normalCATSLoadService.find(route, academicYear, yearOfStudy).map(_.normalLoad))
			cache(route)
	}
	def apply(route: Route): BigDecimal = withoutDefault(route).getOrElse(route.degreeType.normalCATSLoad)
	def routes: Seq[Route] = cache.keys.toSeq
}

trait NormalCATSLoadService {

	def saveOrUpdate(load: NormalCATSLoad): Unit
	def delete(load: NormalCATSLoad): Unit
	def find(route: Route, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[NormalCATSLoad]
	def findAll(routes: Seq[Route], academicYear: AcademicYear): Seq[NormalCATSLoad]

}

abstract class AbstractNormalCATSLoadService extends NormalCATSLoadService {

	self: NormalCATSLoadDaoComponent =>

	def saveOrUpdate(load: NormalCATSLoad): Unit =
		normalCATSLoadDao.saveOrUpdate(load)

	def delete(load: NormalCATSLoad): Unit =
		normalCATSLoadDao.delete(load)

	def find(route: Route, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[NormalCATSLoad] =
		normalCATSLoadDao.find(route, academicYear, yearOfStudy)

	def findAll(routes: Seq[Route], academicYear: AcademicYear): Seq[NormalCATSLoad] =
		normalCATSLoadDao.findAll(routes, academicYear)

}

@Service("normalCATSLoadService")
class NormalCATSLoadServiceImpl
	extends AbstractNormalCATSLoadService
		with AutowiringNormalCATSLoadDaoComponent

trait NormalCATSLoadServiceComponent {
	def normalCATSLoadService: NormalCATSLoadService
}

trait AutowiringNormalCATSLoadServiceComponent extends NormalCATSLoadServiceComponent {
	var normalCATSLoadService: NormalCATSLoadService = Wire[NormalCATSLoadService]
}
