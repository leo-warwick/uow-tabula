package uk.ac.warwick.tabula.web.controllers.attendance

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.BreadCrumb

trait AttendanceBreadcrumbs {
	val Breadcrumbs = AttendanceBreadcrumbs
}

object AttendanceBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

	object Manage {

		case class HomeForYear(academicYear: AcademicYear) extends Abstract {
			val title = "Manage"
			val url = Some(Routes.Manage.homeForYear(academicYear))
		}

		case class DepartmentForYear(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title: String = department.name
			val url = Some(Routes.Manage.departmentForYear(department, academicYear))
		}

		case class EditPoints(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Edit points"
			val url = Some(Routes.Manage.editPoints(department, academicYear))
		}

	}

	object View {

		case class HomeForYear(academicYear: AcademicYear) extends Abstract {
			val title = "View and record"
			val url = Some(Routes.View.homeForYear(academicYear))
		}

		case class DepartmentForYear(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title: String = department.name
			val url = Some(Routes.View.departmentForYear(department, academicYear))
		}

		case class Students(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Students"
			val url = Some(Routes.View.students(department, academicYear))
		}

		case class Student(department: model.Department, academicYear: AcademicYear, student: StudentMember) extends Abstract {
			val title: String = student.fullName.getOrElse("")
			val url = Some(Routes.View.student(department, academicYear, student))
		}

		case class Points(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Points"
			val url = Some(Routes.View.points(department, academicYear))
		}

		case class Agents(department: model.Department, academicYear: AcademicYear, relationshipType: StudentRelationshipType) extends Abstract {
			val title: String = relationshipType.agentRole.capitalize + "s"
			val url = Some(Routes.View.agents(department, academicYear, relationshipType))
		}

	}

	object Agent {

		case class RelationshipForYear(relationshipType: StudentRelationshipType, academicYear: AcademicYear) extends Abstract {
			val title: String = relationshipType.studentRole.capitalize + "s"
			val url = Some(Routes.Agent.relationshipForYear(relationshipType, academicYear))
		}
		case class Student(relationshipType: StudentRelationshipType, academicYear: AcademicYear, student: StudentMember) extends Abstract {
			val title: String = student.fullName.getOrElse("")
			val url = Some(Routes.Agent.student(relationshipType, academicYear, student))
		}

	}

	object Profile {
		case class ProfileForYear(student: StudentMember, academicYear: AcademicYear) extends Abstract {
			val title: String = student.fullName.getOrElse("")
			val url = Some(Routes.Profile.profileForYear(student, academicYear))
		}
	}
}