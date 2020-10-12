package uk.ac.warwick.tabula.data.model

import javax.persistence.{Entity, FetchType, JoinColumn, ManyToOne}
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.services.ModuleRegistrationService

import scala.collection.mutable

@Entity
@Proxy
class CoreRequiredModule extends GeneratedId {

  def this(route: Route, academicYear: AcademicYear, yearOfStudy: Int, module: Module) = {
    this()
    this.route = route
    this.academicYear = academicYear
    this.yearOfStudy = yearOfStudy
    this.module = module
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "routeCode", referencedColumnName = "code")
  var route: Route = _

  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  var academicYear: AcademicYear = _

  var yearOfStudy: JInteger = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moduleCode", referencedColumnName = "code")
  var module: Module = _

}

trait CoreRequiredModuleLookup {
  def apply(route: Route): Seq[CoreRequiredModule]
}

object NullCoreRequiredModuleLookup extends CoreRequiredModuleLookup {
  override def apply(route: Route): Seq[CoreRequiredModule] = Nil
}

class CoreRequiredModuleLookupImpl(academicYear: AcademicYear, yearOfStudy: YearOfStudy, moduleRegistrationService: ModuleRegistrationService) extends CoreRequiredModuleLookup {
  private val cache = mutable.Map[Route, Seq[CoreRequiredModule]]()

  override def apply(route: Route): Seq[CoreRequiredModule] = cache.get(route) match {
    case Some(modules) =>
      modules
    case _ =>
      cache.put(route, moduleRegistrationService.findCoreRequiredModules(route, academicYear, yearOfStudy))
      cache(route)
  }
}
