package uk.ac.warwick.tabula.data.model

import javax.persistence._
import javax.validation.constraints.NotNull

import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import org.hibernate.annotations.Type
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy

object CourseYearWeighting {
	def find(course: Course, academicYear: AcademicYear, yearOfStudy: YearOfStudy)(weighting: CourseYearWeighting): Boolean = {
		weighting.course == course && weighting.sprStartAcademicYear == academicYear && weighting.yearOfStudy == yearOfStudy
	}
}

@Entity
class CourseYearWeighting extends GeneratedId with Ordered[CourseYearWeighting] {

	def this(course: Course, academicYear: AcademicYear, yearOfStudy: YearOfStudy, weightingAsPercentage: BigDecimal) {
		this()
		this.course = course
		this.sprStartAcademicYear = academicYear
		this.yearOfStudy = yearOfStudy
		this.weighting = weightingAsPercentage / percentageMultiplier
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "courseCode", referencedColumnName="code")
	@NotNull
	var course: Course = _

	@Basic
	@NotNull
	@Column(name="academicYear")
	@Type(`type` = "uk.ac.warwick.tabula.data.model.AcademicYearUserType")
	var sprStartAcademicYear: AcademicYear = _

	@NotNull
	var yearOfStudy: Int = _

	@Column(name="weighting")
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

	def compare(that:CourseYearWeighting): Int = {
		if (this.course.code != that.course.code)
			this.course.code.compare(that.course.code)
		else if (this.sprStartAcademicYear != that.sprStartAcademicYear)
			this.sprStartAcademicYear.compare(that.sprStartAcademicYear)
		else
			this.yearOfStudy.compare(that.yearOfStudy)
	}

}
