package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Course, CourseYearWeighting, Department}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy

trait CourseDaoComponent {
	val courseDao: CourseDao
}

trait AutowiringCourseDaoComponent extends CourseDaoComponent {
	val courseDao: CourseDao = Wire[CourseDao]
}

trait CourseDao {
	def saveOrUpdate(course: Course): Unit
	def saveOrUpdate(courseYearWeighting: CourseYearWeighting): Unit

	def delete(courseYearWeighting: CourseYearWeighting): Unit

	def getByCode(code: String): Option[Course]
	def getAllCourseCodes: Seq[String]
	def findByDepartment(department: Department): Seq[Course]

	def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[CourseYearWeighting]
	def findAllCourseYearWeightings(courses: Seq[Course], academicYear: AcademicYear): Seq[CourseYearWeighting]
}

@Repository
class CourseDaoImpl extends CourseDao with Daoisms {

	def saveOrUpdate(course: Course): Unit = session.saveOrUpdate(course)

	def saveOrUpdate(courseYearWeighting: CourseYearWeighting): Unit = session.saveOrUpdate(courseYearWeighting)

	def delete(courseYearWeighting: CourseYearWeighting): Unit = session.delete(courseYearWeighting)

	def getByCode(code: String): Option[Course] =
		session.newQuery[Course]("from Course course where code = :code").setString("code", code).uniqueResult

	def getAllCourseCodes: Seq[String] =
		session.newQuery[String]("select distinct code from Course").seq

	def findByDepartment(department: Department): Seq[Course] =
		session.newCriteria[Course]
			.add(is("_department", department))
			.seq

	def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: Int): Option[CourseYearWeighting] =
		session.newCriteria[CourseYearWeighting]
			.createAlias("course", "course")
			.add(is("course.code", courseCode))
			.add(is("sprStartAcademicYear", academicYear))
			.add(is("yearOfStudy", yearOfStudy))
			.uniqueResult

	def findAllCourseYearWeightings(courses: Seq[Course], academicYear: AcademicYear): Seq[CourseYearWeighting] =
		safeInSeq[CourseYearWeighting](
			() => session.newCriteria[CourseYearWeighting]
				.add(is("sprStartAcademicYear", academicYear)),
			"course",
			courses
		)

}
