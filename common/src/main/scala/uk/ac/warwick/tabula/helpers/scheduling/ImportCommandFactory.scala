package uk.ac.warwick.tabula.helpers.scheduling

import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportStudentCourseYearCommand, ImportStudentCourseCommand}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.data.{MemberDao, ModeOfAttendanceDao, StudentCourseDetailsDao, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.services.scheduling.{AwardImporter, CourseImporter, ModeOfAttendanceImporter, SitsStatusImporter}
import uk.ac.warwick.tabula.services.{CourseAndRouteService, MaintenanceModeService, ModuleAndDepartmentService, ProfileService, RelationshipService}

class ImportCommandFactory() {

	var test = false

	// This lot are all normally auto-wired within the import commands.  Declared here in order to allow us to
	// overwrite them with mock versions for testing.

	// needed for ImportStudentCourseCommand:
	var memberDao: MemberDao = _
	var relationshipService: RelationshipService = _
	var studentCourseDetailsDao: StudentCourseDetailsDao = _
	var courseAndRouteService: CourseAndRouteService = _
	var courseImporter: CourseImporter = _
	var awardImporter: AwardImporter = _

	// needed for PropertyCopying, extended by ImportStudentCourseCommand:
	var sitsStatusImporter: SitsStatusImporter = _
	var modAndDeptService: ModuleAndDepartmentService = _

	// needed for ImportStudentCourseYearCommand:
	var moaDao: ModeOfAttendanceDao = _
	var profileService: ProfileService = _
	var studentCourseYearDetailsDao: StudentCourseYearDetailsDao = _
	var modeOfAttendanceImporter: ModeOfAttendanceImporter = _

	// needed by Command
	var maintenanceModeService: MaintenanceModeService = _

	def createImportStudentCourseCommand(rows: Seq[SitsStudentRow], stuMem: StudentMember): ImportStudentCourseCommand = {
		val command = new ImportStudentCourseCommand(rows, stuMem, this)

		if (test) {
			// needed directly by ImportStudentCourseCommand
			command.memberDao = memberDao
			command.relationshipService = relationshipService
			command.studentCourseDetailsDao = studentCourseDetailsDao
			command.courseAndRouteService = courseAndRouteService
			command.courseImporter = courseImporter
			command.awardImporter = awardImporter

			// needed by PropertyCopying, extended by ImportStudentCourseCommand
			command.sitsStatusImporter = sitsStatusImporter
			command.moduleAndDepartmentService = modAndDeptService

			// FIXME horrible hack needed by Command, extended by ImportStudentCourseCommand
			command.maintenanceModeService = maintenanceModeService
		}
		command
	}

	def createImportStudentCourseYearCommand(row: SitsStudentRow, studentCourseDetails: StudentCourseDetails): ImportStudentCourseYearCommand = {
		val command = new ImportStudentCourseYearCommand(row, studentCourseDetails)
		if (test) { // FIXME horrible hack
			command.modeOfAttendanceImporter = modeOfAttendanceImporter
			command.profileService = profileService
			command.sitsStatusImporter = sitsStatusImporter
			command.maintenanceModeService = maintenanceModeService
			command.studentCourseYearDetailsDao = studentCourseYearDetailsDao
			command.courseAndRouteService = courseAndRouteService
		}
		command
	}
}
