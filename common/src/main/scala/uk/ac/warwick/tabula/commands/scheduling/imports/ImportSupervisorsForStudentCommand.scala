package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.DegreeType.Postgraduate
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.scheduling.SupervisorImporter
import uk.ac.warwick.tabula.services.{ProfileService, RelationshipService}

class ImportSupervisorsForStudentCommand(var studentCourseDetails: StudentCourseDetails)
	extends Command[Unit] with Unaudited with Logging {
	PermissionCheck(Permissions.ImportSystemData)

	var supervisorImporter: SupervisorImporter = Wire.auto[SupervisorImporter]
	var profileService: ProfileService = Wire.auto[ProfileService]
	var relationshipService: RelationshipService = Wire.auto[RelationshipService]

	def applyInternal() {
		if (studentCourseDetails.currentRoute != null && studentCourseDetails.currentRoute.degreeType == Postgraduate) {
			transactional() {
				importSupervisors()
			}
		}
	}

	override def describe(d: Description): Unit = d.property("sprCode" -> studentCourseDetails.sprCode)

	def importSupervisors() {
		relationshipService
			.getStudentRelationshipTypesWithRdxType // only look for relationship types that are in RDX
			.filter { relType => // where the department settings specify that SITS should be the source
				val source = Option(studentCourseDetails.department).map { _.getStudentRelationshipSource(relType) }.getOrElse(relType.defaultSource)
				source == StudentRelationshipSource.SITS
			}
			.foreach { relationshipType =>
				val supervisorUniIds = supervisorImporter.getSupervisorUniversityIds(studentCourseDetails.scjCode, relationshipType)
				val supervisors = supervisorUniIds.flatMap { case (supervisorUniId, percentage) =>
					val m = profileService.getMemberByUniversityId(supervisorUniId)
					if (m.isEmpty) {
						logger.warn("Can't save supervisor " + supervisorUniId + " for " + studentCourseDetails.sprCode + " - not a member in Tabula db")
					}
					m.map { m => (m, percentage) }
				}

				relationshipService.replaceStudentRelationshipsWithPercentages(relationshipType, studentCourseDetails, supervisors)
			}
	}
}
