package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Restrictions._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormField

trait AssessmentDaoComponent {
	val assessmentDao: AssessmentDao
}

trait AutowiringAssessmentDaoComponent extends AssessmentDaoComponent {
	val assessmentDao: AssessmentDao = Wire[AssessmentDao]
}

trait AssessmentDao {

	def getAssignmentById(id: String): Option[Assignment]
	def getExamById(id: String): Option[Exam]

	def save(assignment: Assignment): Unit
	def save(exam: Exam): Unit

	def deleteFormField(field: FormField): Unit

	def getAssignmentsWithFeedback(usercode: String): Seq[Assignment]
	def getAssignmentsWithFeedback(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment]

	def getAssignmentsWithSubmission(usercode: String): Seq[Assignment]
	def getAssignmentsWithSubmission(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment]
	def getSubmissionsForAssignmentsBetweenDates(usercode: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission]

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment]

	def getExamByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Exam]

	def getAssignments(department: Department, year: AcademicYear): Seq[Assignment]

	def recentAssignment(department: Department): Option[Assignment]

	val MaxAssignmentsByName = 15

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment]

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment]

	def getAssignmentsClosingBetween(startInclusive: DateTime, endExclusive: DateTime): Seq[Assignment]

	def getExamsByModules(modules: Seq[Module], academicYear: AcademicYear): Map[Module, Seq[Exam]]
}

@Repository
class AssessmentDaoImpl extends AssessmentDao with Daoisms {

	def getAssignmentById(id: String): Option[Assignment] = getById[Assignment](id)
	def getExamById(id: String): Option[Exam] = getById[Exam](id)

	def save(assignment: Assignment): Unit = session.saveOrUpdate(assignment)
	def save(exam: Exam): Unit = session.saveOrUpdate(exam)

	def deleteFormField(field: FormField) {
		session.delete(field)
	}

	def getAssignmentsWithFeedback(usercode: String): Seq[Assignment] =
		session.newQuery[Assignment]("""select a from Assignment a
				join a.feedbacks as f
				where f.usercode = :usercode
				and f.released=true""")
			.setString("usercode", usercode)
			.distinct.seq

	def getAssignmentsWithFeedback(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment] = {
		val c = session.newCriteria[AssignmentFeedback]
			.createAlias("assignment", "assignment")
			.add(is("usercode", usercode))
			.add(is("released", true))
		  .add(is("assignment.deleted", false))
		  .add(is("assignment._hiddenFromStudents", false))
			.setFetchMode("assignment", FetchMode.JOIN)

		(academicYearOption match {
			case Some(academicYear) =>
				c.add(is("assignment.academicYear", academicYear)).seq
			case _ => c.seq
		}).map(_.assignment)
	}

	def getAssignmentsWithSubmission(usercode: String): Seq[Assignment] =
		session.newQuery[Assignment]("""select a from Assignment a
				join a.submissions as s
				where s.usercode = :usercode""")
			.setString("usercode", usercode)
			.distinct.seq

	def getAssignmentsWithSubmission(usercode: String, academicYearOption: Option[AcademicYear]): Seq[Assignment] = {
		val c = session.newCriteria[Submission]
			.createAlias("assignment", "assignment")
			.add(is("usercode", usercode))
			.add(is("assignment.deleted", false))
			.add(is("assignment._hiddenFromStudents", false))
			.setFetchMode("assignment", FetchMode.JOIN)

		(academicYearOption match {
			case Some(academicYear) =>
				c.add(is("assignment.academicYear", academicYear)).seq
			case _ => c.seq
		}).map(_.assignment)
	}

	def getSubmissionsForAssignmentsBetweenDates(usercode: String, startInclusive: DateTime, endExclusive: DateTime): Seq[Submission] =
		session.newCriteria[Submission]
			.createAlias("assignment", "assignment")
			.add(is("usercode", usercode))
			.add(ge("assignment.closeDate", startInclusive))
			.add(lt("assignment.closeDate", endExclusive))
			.seq

	def getAssignmentByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Assignment] =
		session.newQuery[Assignment]("from Assignment where name=:name and academicYear=:year and module=:module and deleted=0")
			.setString("name", name)
			.setParameter("year", year)
			.setEntity("module", module)
			.seq

	def getExamByNameYearModule(name: String, year: AcademicYear, module: Module): Seq[Exam] =
		session.newQuery[Exam]("from Exam where name=:name and academicYear=:year and module=:module and deleted=0")
			.setString("name", name)
			.setParameter("year", year)
			.setEntity("module", module)
			.seq

	def getAssignments(department: Department, year: AcademicYear): Seq[Assignment] = {
		val assignments = session.newCriteria[Assignment]
			.createAlias("module", "m")
			.add(is("m.adminDepartment", department))
			.add(is("academicYear", year))
			.add(is("deleted", false))
			.add(is("_archived", false))
			.seq
		assignments
	}

	def recentAssignment(department: Department): Option[Assignment] = {
		session.newCriteria[Assignment]
			.createAlias("module", "m")
			.add(is("m.adminDepartment", department))
			.add(isNotNull("createdDate"))
			.addOrder(desc("createdDate"))
			.setMaxResults(1)
			.uniqueResult
	}

	def getAssignmentsByName(partialName: String, department: Department): Seq[Assignment] = {

		session.newQuery[Assignment]("""select a from Assignment a
				where a.module.adminDepartment = :dept
				and lower(a.name) like :nameLike
				order by createdDate desc
																 """)
			.setParameter("dept", department)
			.setString("nameLike", "%" + partialName.toLowerCase + "%")
			.setMaxResults(MaxAssignmentsByName).seq
	}

	def findAssignmentsByNameOrModule(query: String): Seq[Assignment] = {
		session.newQuery[Assignment]("""select a from Assignment
				a where lower(a.name) like :nameLike
				or lower(a.module.code) like :nameLike
				order by createdDate desc
																 """)
			.setString("nameLike", "%" + query.toLowerCase + "%")
			.setMaxResults(MaxAssignmentsByName).seq
	}

	def getAssignmentsClosingBetween(startInclusive: DateTime, endExclusive: DateTime): Seq[Assignment] =
		session.newCriteria[Assignment]
			.add(is("openEnded", false))
			.add(ge("closeDate", startInclusive))
			.add(lt("closeDate", endExclusive))
			.add(is("_archived", false))
			.addOrder(asc("closeDate"))
			.seq

	def getExamsByModules(modules: Seq[Module], academicYear: AcademicYear): Map[Module, Seq[Exam]] = {
		safeInSeq(() => {
			session.newCriteria[Exam]
				.add(is("academicYear", academicYear))
				.add(isNot("deleted", true))
		}, "module", modules).groupBy(_.module)
	}
}
