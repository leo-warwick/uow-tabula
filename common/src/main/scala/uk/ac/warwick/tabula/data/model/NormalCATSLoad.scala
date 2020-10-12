package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JBigDecimal, JInteger}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy

object NormalCATSLoad {
  def find(route: Route, academicYear: AcademicYear, yearOfStudy: YearOfStudy)(load: NormalCATSLoad): Boolean = {
    load.route == route && load.academicYear == academicYear && load.yearOfStudy == yearOfStudy
  }
}

@Entity
@Proxy
class NormalCATSLoad extends GeneratedId {

  def this(academicYear: AcademicYear, route: Route, yearOfStudy: YearOfStudy, normalLoad: BigDecimal) = {
    this()
    this.academicYear = academicYear
    this.route = route
    this.yearOfStudy = yearOfStudy
    this.normalLoad = normalLoad
  }

  @Basic
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  @Column(name = "academicYear")
  @NotNull
  var academicYear: AcademicYear = _

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "routeCode", referencedColumnName = "code")
  var route: Route = _

  @NotNull
  var yearOfStudy: JInteger = _

  @Column(name = "normalLoad")
  @NotNull
  private var _normalLoad: JBigDecimal = _

  def normalLoad_=(normalLoad: BigDecimal): Unit = {
    _normalLoad = normalLoad.underlying
  }

  def normalLoad: BigDecimal = BigDecimal(_normalLoad)

}
