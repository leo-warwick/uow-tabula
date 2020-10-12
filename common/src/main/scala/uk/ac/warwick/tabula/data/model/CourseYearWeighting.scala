package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.validation.constraints.NotNull
import org.hibernate.annotations.{Proxy, Type}
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.{AcademicYear, ToString}

object CourseYearWeighting {
  def find(course: Course, academicYear: AcademicYear, yearOfStudy: YearOfStudy)(weighting: CourseYearWeighting): Boolean = {
    weighting.course == course && weighting.sprStartAcademicYear == academicYear && weighting.yearOfStudy == yearOfStudy
  }

  implicit val defaultOrdering: Ordering[CourseYearWeighting] {
    def compare(a: CourseYearWeighting, b: CourseYearWeighting): Int
  } = new Ordering[CourseYearWeighting] {
    def compare(a: CourseYearWeighting, b: CourseYearWeighting): Int = {
      if (a.course.code != b.course.code)
        a.course.code.compare(b.course.code)
      else if (a.sprStartAcademicYear != b.sprStartAcademicYear)
        a.sprStartAcademicYear.compare(b.sprStartAcademicYear)
      else
        a.yearOfStudy.compare(b.yearOfStudy)
    }
  }
}

@Entity
@Proxy
class CourseYearWeighting extends GeneratedId with ToString {

  def this(course: Course, academicYear: AcademicYear, yearOfStudy: YearOfStudy, weightingAsPercentage: BigDecimal) = {
    this()
    this.course = course
    this.sprStartAcademicYear = academicYear
    this.yearOfStudy = yearOfStudy
    this.weighting = weightingAsPercentage / percentageMultiplier
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "courseCode", referencedColumnName = "code")
  @NotNull
  var course: Course = _

  @Basic
  @NotNull
  @Column(name = "academicYear")
  @Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
  var sprStartAcademicYear: AcademicYear = _

  @NotNull
  var yearOfStudy: Int = _

  @Column(name = "weighting")
  @NotNull
  private var _weighting: JBigDecimal = _

  def weighting_=(weighting: BigDecimal): Unit = {
    _weighting = weighting.underlying
  }

  def weighting: BigDecimal = BigDecimal(_weighting)

  def weightingAsPercentage_=(weightingAsPercentage: BigDecimal): Unit = {
    weighting = weightingAsPercentage / percentageMultiplier
  }

  @transient
  private val percentageMultiplier = new JBigDecimal(100)

  def weightingAsPercentage: JBigDecimal = weighting.underlying.multiply(percentageMultiplier).stripTrailingZeros

  override def toStringProps: Seq[(String, Any)] = Seq(
    "course" -> course,
    "sprStartAcademicYear" -> sprStartAcademicYear,
    "yearOfStudy" -> yearOfStudy,
    "weighting" -> weighting
  )

  def copyZeroWeighted: CourseYearWeighting = {
    new CourseYearWeighting(course, sprStartAcademicYear, yearOfStudy, 0)
  }
}
