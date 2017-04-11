package uk.ac.warwick.tabula.web.controllers.profiles

import uk.ac.warwick.tabula.data.model.{Member, StudentCourseYearDetails, StudentRelationshipType}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.web.Breadcrumbs.Active

trait ProfileBreadcrumbs {
	val Breadcrumbs = ProfileBreadcrumbs
}

object ProfileBreadcrumbs {

	object Profile {

		sealed abstract class ProfileBreadcrumbIdentifier(id: String)
		case object IdentityIdentifier extends ProfileBreadcrumbIdentifier("identity")
		case object TimetableIdentifier extends ProfileBreadcrumbIdentifier("timetable")
		case class RelationshipTypeIdentifier(relationshipType: StudentRelationshipType)
			extends ProfileBreadcrumbIdentifier(relationshipType.urlPart)
		case object AssignmentsIdentifier extends ProfileBreadcrumbIdentifier("assignments")
		case object ModulesIdentifier extends ProfileBreadcrumbIdentifier("modules")
		case object SeminarsIdentifier extends ProfileBreadcrumbIdentifier("seminars")
		case object MarkingIdentifier extends ProfileBreadcrumbIdentifier("marking")
		case object StudentsIdentifier extends ProfileBreadcrumbIdentifier("students")
		case object AttendanceIdentifier extends ProfileBreadcrumbIdentifier("attendance")


		abstract class ProfileBreadcrumb extends BreadCrumb {
			def identifier: ProfileBreadcrumbIdentifier
			def setActive(activeIdentifier: ProfileBreadcrumbIdentifier): BreadCrumb = {
				if (this.identifier == activeIdentifier) {
					Active(this.title, this.url, this.tooltip)
				} else {
					this
				}
			}
		}

		case class Identity(member: Member) extends ProfileBreadcrumb {
			val identifier = IdentityIdentifier
			val title: String = member.userId
			val url = Some(Routes.Profile.identity(member))
			override val tooltip: String = member.fullName.getOrElse("")
		}

		case class IdentityForScyd(scyd: StudentCourseYearDetails) extends ProfileBreadcrumb {
			val identifier = IdentityIdentifier
			val title: String = scyd.studentCourseDetails.student.userId
			val url = Some(Routes.Profile.identity(scyd))
			override val tooltip: String = scyd.studentCourseDetails.student.fullName.getOrElse("")
		}

		case class Timetable(member: Member) extends ProfileBreadcrumb {
			val identifier = TimetableIdentifier
			val title = "Timetable"
			val url = Some(Routes.Profile.timetable(member))
		}

		case class TimetableForScyd(scyd: StudentCourseYearDetails) extends ProfileBreadcrumb {
			val identifier = TimetableIdentifier
			val title = "Timetable"
			val url = Some(Routes.Profile.timetable(scyd))
		}

		case class RelationshipTypeForScyd(scyd: StudentCourseYearDetails, relationshipType: StudentRelationshipType) extends ProfileBreadcrumb {
			val identifier = RelationshipTypeIdentifier(relationshipType)
			val title: String = relationshipType.agentRole.capitalize
			val url = Some(Routes.Profile.relationshipType(scyd, relationshipType))
		}

		case class AssignmentsForScyd(scyd: StudentCourseYearDetails) extends ProfileBreadcrumb {
			val identifier = AssignmentsIdentifier
			val title = "Assignments"
			val url = Some(Routes.Profile.assignments(scyd))
		}

		case class ModulesForScyd(scyd: StudentCourseYearDetails) extends ProfileBreadcrumb {
			val identifier = ModulesIdentifier
			val title = "Modules"
			val url = Some(Routes.Profile.modules(scyd))
		}

		case class SeminarsForScyd(scyd: StudentCourseYearDetails) extends ProfileBreadcrumb {
			val identifier = SeminarsIdentifier
			val title = "Seminars"
			val url = Some(Routes.Profile.seminars(scyd))
		}

		case class Marking(member: Member) extends ProfileBreadcrumb {
			val identifier = MarkingIdentifier
			val title = "Marking"
			val url = Some(Routes.Profile.marking(member))
		}

		case class MarkingForScyd(scyd: StudentCourseYearDetails) extends ProfileBreadcrumb {
			val identifier = MarkingIdentifier
			val title = "Marking"
			val url = Some(Routes.Profile.marking(scyd))
		}

		case class Students(member: Member) extends ProfileBreadcrumb {
			val identifier = StudentsIdentifier
			val title = "Students"
			val url = Some(Routes.Profile.students(member))
		}

		case class Attendance(member: Member) extends ProfileBreadcrumb {
			val identifier = AttendanceIdentifier
			val title = "Attendance"
			val url = Some(Routes.Profile.attendance(member))
		}

		case class AttendanceForScyd(scyd: StudentCourseYearDetails) extends ProfileBreadcrumb {
			val identifier = AttendanceIdentifier
			val title = "Attendance"
			val url = Some(Routes.Profile.attendance(scyd))
		}

	}

}