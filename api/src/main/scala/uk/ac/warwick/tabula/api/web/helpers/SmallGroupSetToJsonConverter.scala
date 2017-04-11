package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewSetMethods

trait SmallGroupSetToJsonConverter {
	self: SmallGroupToJsonConverter with AssessmentMembershipInfoToJsonConverter =>

	def jsonSmallGroupSetObject(viewSet: ViewSetMethods): Map[String, Any] = {
		val set = viewSet.set

		val basicInfo = Map(
			"id" -> set.id,
			"module" -> Map(
				"code" -> set.module.code.toUpperCase,
				"name" -> set.module.name,
				"adminDepartment" -> Map(
					"code" -> set.module.adminDepartment.code.toUpperCase,
					"name" -> set.module.adminDepartment.name
				)
			),
			"name" -> set.name,
			"archived" -> set.archived,
			"format" -> set.format.code,
			"allocationMethod" -> set.allocationMethod.dbValue,
			"releasedToTutors" -> set.releasedToTutors,
			"releasedToStudents" -> set.releasedToStudents,
			"studentsCanSeeTutorName" -> set.studentsCanSeeTutorName,
			"studentsCanSeeOtherMembers" -> set.studentsCanSeeOtherMembers,
			"collectAttendance" -> set.collectAttendance,
			"emailTutorsOnChange" -> set.emailTutorsOnChange,
			"emailStudentsOnChange" -> set.emailStudentsOnChange
		)

		val specificAllocationMethodInfo = set.allocationMethod match {
			case SmallGroupAllocationMethod.StudentSignUp => Map(
				"allowSelfGroupSwitching" -> set.allowSelfGroupSwitching,
				"openForSignups" -> set.openForSignups
			)

			case SmallGroupAllocationMethod.Linked => Map(
				"linkedDepartmentGroupSet" -> Option(set.linkedDepartmentSmallGroupSet).map { _.id }.orNull
			)

			case _ => Map()
		}

		val membershipInfo = set.membershipInfo
		val studentMembershipInfo = jsonAssessmentMembershipInfoObject(membershipInfo, set.upstreamAssessmentGroups)

		val groupInfo = Map(
			"groups" -> viewSet.groups.map(g => jsonSmallGroupObject(g.group))
		)

		basicInfo ++ specificAllocationMethodInfo ++ studentMembershipInfo ++ groupInfo
	}
}
