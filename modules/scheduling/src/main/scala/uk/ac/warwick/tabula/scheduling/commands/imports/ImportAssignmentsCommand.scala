package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.SprCode
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.services.AssignmentImporter
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.util.Try

object ImportAssignmentsCommand {
	def apply() = new ComposableCommand[Unit]
		with ImportAssignmentsCommand
		with ImportAssignmentsDescription

	case class Result(
		assignmentsFound: Int,
		assignmentsChanged: Int,
		groupsFound: Int,
		groupsChanged: Int)
}


trait ImportAssignmentsCommand extends CommandInternal[Unit] with RequiresPermissionsChecking with Logging {

	def permissionsCheck(p:PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

	var assignmentImporter = Wire[AssignmentImporter]
	var assessmentMembershipService = Wire[AssessmentMembershipService]
	var moduleAndDepartmentService = Wire[ModuleAndDepartmentService]

	val ImportGroupSize = 100

	def applyInternal() {
		benchmark("ImportAssessment") {
			doAssignments()
			logger.debug("Imported AssessmentComponents. Importing assessment groups...")
			doGroups()
			doGroupMembers()
			logger.debug("Imported assessment groups. Importing grade boundaries...")
			doGradeBoundaries()
		}
	}

	def doAssignments() {
		val assessmentComponents = logSize(assignmentImporter.getAllAssessmentComponents)
		val modules = moduleAndDepartmentService.getModulesByCodes(assessmentComponents.map(_.moduleCodeBasic).distinct)
			.groupBy(_.code).mapValues(_.head)
		for (assignments <- assessmentComponents.grouped(ImportGroupSize)) {
			transactional() {
				for (assignment <- assignments) {
					if (assignment.name == null) {
						// Some SITS data is bad, but try to carry on.
						assignment.name = "Assessment Component"
					}

					modules.get(assignment.moduleCodeBasic.toLowerCase).foreach(module => assignment.module = module)
					assessmentMembershipService.save(assignment)
				}
			}
		}
	}

	def doGroups() {
		// Split into chunks so we commit transactions periodically.
		for (groups <- logSize(assignmentImporter.getAllAssessmentGroups).grouped(ImportGroupSize)) {
			saveGroups(groups)
			assessmentMembershipService.flushAndEvict(groups)
		}
	}

	/**
	 * This calls the importer method that iterates over ALL module registrations.
	 * The results are ordered such that it can hold a list of items until it
	 * detects one that belongs to a different group, at which point it saves
	 * what it's got and starts a new list. This way we don't have to load many
	 * things into memory at once.
	 */
	def doGroupMembers() {
		benchmark("Import all group members") {
			var registrations = List[UpstreamModuleRegistration]()
			var notEmptyGroupIds = Set[String]()

			var count = 0
			assignmentImporter.allMembers { r =>
				if (registrations.nonEmpty && r.differentGroup(registrations.head)) {
					// This element r is for a new group, so save this group and start afresh
					transactional() {
						save(registrations)
							.foreach { uag => notEmptyGroupIds = notEmptyGroupIds + uag.id }
					}
					registrations = Nil
				}
				registrations = registrations :+ r

				count += 1
				if (count % 1000 == 0) {
					logger.info("Processed " + count + " group members")
				}

			}

			// TAB-1265 Don't forget the very last bunch.
			if (registrations.nonEmpty) {
				transactional() {
					save(registrations)
						.foreach { uag => notEmptyGroupIds = notEmptyGroupIds + uag.id }
				}
			}

			// empty unseen groups - this is done in transactional batches

			val groupsToEmpty = transactional() {
				assessmentMembershipService.getUpstreamAssessmentGroupsNotIn (
					ids = notEmptyGroupIds.filter { _.hasText }.toSeq,
					academicYears = assignmentImporter.yearsToImport
				)
			}

			logger.info("Emptying members for unseen groups")
			val numEmptied = assessmentMembershipService.emptyMembers(groupsToEmpty)
			logger.info(s"Emptied $numEmptied users from ${groupsToEmpty.size} unseen groups")


		}
	}

	/**
	 * This sequence of ModuleRegistrations represents the members of an assessment
	 * group, so save them (and reconcile it with any existing members we have in the
	 * database).
	 * The students in a group does NOT vary by sequence, so the memebership should be set on ALL the groups (ignoring seat number).
	 * Then seat the appropriate seat numbers based on the sequence.
	 */
	def save(registrations: Seq[UpstreamModuleRegistration]): Seq[UpstreamAssessmentGroup] = {
		registrations.headOption.map { head =>
			val assessmentComponents = assessmentMembershipService.getAssessmentComponents(head.moduleCode).filter(_.assessmentGroup == head.assessmentGroup)
			val assessmentGroups = head.toUpstreamAssessmentGroups(assessmentComponents.map(_.sequence).distinct)
				.map(assessmentGroup => assessmentMembershipService.replaceMembers(assessmentGroup, registrations))

			// Now sort out seat number/s
			val withSeats = registrations.filter(r => r.sequence != null && Try(r.seatNumber.toInt).isSuccess)
			assessmentGroups.foreach(group => {
				// Find the registations for this exact group (including sequence)
				val theseRegistrations = withSeats.filter(_.toExactUpstreamAssessmentGroup.isEquivalentTo(group))
				if (theseRegistrations.nonEmpty) {
					val seatMap = theseRegistrations.groupBy(_.sprCode).flatMap{case(sprCode, studentRegistrations) =>
						if (studentRegistrations.map(_.seatNumber).distinct.size > 1) {
							logger.warn("Found multiple seat numbers (%s) for %s for Assessment Group %s. Seat number will be null".format(
								studentRegistrations.map(_.seatNumber).mkString(", "),
								sprCode,
								group.toString
							))
							None
						} else {
							Option((SprCode.getUniversityId(sprCode), studentRegistrations.head.seatNumber.toInt))
						}
					}
					assessmentMembershipService.updateSeatNumbers(group, seatMap)
				}
			})
			assessmentGroups
		}.getOrElse(Seq())
	}


	def saveGroups(groups: Seq[UpstreamAssessmentGroup]) = transactional() {
		logger.debug("Importing " + groups.size + " assessment groups")
		benchmark("Import " + groups.size + " groups") {
			for (group <- groups) {
				assessmentMembershipService.save(group)
			}
		}
	}

	def doGradeBoundaries() {
		transactional() {
			val boundaries = assignmentImporter.getAllGradeBoundaries
			boundaries.groupBy(_.marksCode).keys.foreach(assessmentMembershipService.deleteGradeBoundaries)
			for (gradeBoundary <- logSize(assignmentImporter.getAllGradeBoundaries)) {
				assessmentMembershipService.save(gradeBoundary)
			}
		}
	}

}


trait ImportAssignmentsDescription extends Describable[Unit] {
	def describe(d: Description) {}
}