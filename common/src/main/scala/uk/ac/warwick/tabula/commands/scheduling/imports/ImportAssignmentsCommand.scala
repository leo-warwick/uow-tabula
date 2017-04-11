package uk.ac.warwick.tabula.commands.scheduling.imports

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{AcademicYear, SprCode}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling.AssignmentImporter
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}

import scala.collection.JavaConverters._
import scala.util.Try

object ImportAssignmentsCommand {
	def apply() = new ComposableCommandWithoutTransaction[Unit]
		with ImportAssignmentsCommand
		with ImportAssignmentsDescription
		with Daoisms

	def applyAllYears() = new ComposableCommandWithoutTransaction[Unit]
		with ImportAssignmentsAllYearsCommand
		with ImportAssignmentsDescription
		with Daoisms {
		override lazy val eventName = "ImportAssignmentsAllYears"
	}

	case class Result(
		assignmentsFound: Int,
		assignmentsChanged: Int,
		groupsFound: Int,
		groupsChanged: Int)
}


trait ImportAssignmentsCommand extends CommandInternal[Unit] with RequiresPermissionsChecking with Logging with SessionComponent {

	def permissionsCheck(p:PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

	var assignmentImporter: AssignmentImporter = Wire[AssignmentImporter]
	var assessmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

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
		val modules = transactional(readOnly = true) {
			moduleAndDepartmentService.getModulesByCodes(assessmentComponents.map(_.moduleCodeBasic).distinct)
				.groupBy(_.code).mapValues(_.head)
		}
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
			transactional() {
				session.flush()
				groups foreach session.evict
			}
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

			val groupsToEmpty = transactional(readOnly = true) {
				assessmentMembershipService.getUpstreamAssessmentGroupsNotIn (
					ids = notEmptyGroupIds.filter { _.hasText }.toSeq,
					academicYears = assignmentImporter.yearsToImport
				)
			}

			logger.info("Emptying members for unseen groups")
			val numEmptied = transactional() {
				assessmentMembershipService.emptyMembers(groupsToEmpty)
			}
			logger.info(s"Emptied $numEmptied users from ${groupsToEmpty.size} unseen groups")


		}
	}

	/**
	 * This sequence of ModuleRegistrations represents the members of an assessment
	 * group, so save them (and reconcile it with any existing members we have in the
	 * database).
	 * The students in a group does NOT vary by sequence, so the memebership should be set on ALL the groups.
	 * Then set the properties of members of each group by sequence.
	 */
	def save(registrations: Seq[UpstreamModuleRegistration]): Seq[UpstreamAssessmentGroup] = {
		registrations.headOption.map { head =>
			// Get all the Assessment Components we have in the DB, even if marked as not in use, as we might have previous years groups to populate
			val assessmentComponents = assessmentMembershipService.getAssessmentComponents(head.moduleCode, inUseOnly = false)
				.filter(_.assessmentGroup == head.assessmentGroup)
			val assessmentGroups = head.toUpstreamAssessmentGroups(assessmentComponents.map(_.sequence).distinct)
				.map(assessmentGroup => assessmentMembershipService.replaceMembers(assessmentGroup, registrations))

			// Now sort out properties
			val hasSequence = registrations.filter(r => r.sequence != null)
			assessmentGroups.foreach(group => {
				// Find the registations for this exact group (including sequence)
				val theseRegistrations = hasSequence.filter(_.toExactUpstreamAssessmentGroup.isEquivalentTo(group))
				if (theseRegistrations.nonEmpty) {
					val registrationsByStudent = theseRegistrations.groupBy(_.sprCode)
					// Where there are multiple values for each of the properties (seat number, mark, and grade) we need to flatten them to a single value.
					// Where there is ambiguity, set the value to None
					val propertiesMap: Map[String, UpstreamAssessmentGroupMemberProperties] = registrationsByStudent.map { case (sprCode, studentRegistrations) => sprCode -> {
						if (studentRegistrations.size == 1) {
							new UpstreamAssessmentGroupMemberProperties {
								position = Try(studentRegistrations.head.seatNumber.toInt).toOption
								actualMark = Try(BigDecimal(studentRegistrations.head.actualMark)).toOption
								actualGrade = studentRegistrations.head.actualGrade.maybeText
								agreedMark = Try(BigDecimal(studentRegistrations.head.agreedMark)).toOption
								agreedGrade = studentRegistrations.head.agreedGrade.maybeText
							}
						} else {
							def validInts(strings: Seq[String]): Seq[Int] = strings.filter(s => Try(s.toInt).isSuccess).map(_.toInt)
							def validBigDecimals(strings: Seq[String]): Seq[BigDecimal] = strings.filter(s => Try(BigDecimal(s)).isSuccess).map(BigDecimal(_))
							def validStrings(strings: Seq[String]): Seq[String] = strings.filter(s => s.maybeText.isDefined)
							def resolveDuplicates[A](props: Seq[A], description: String): Option[A] = {
								if (props.distinct.size > 1) {
									logger.warn("Found multiple %ss (%s) for %s for Assessment Group %s. %s will be null".format(
										description,
										props.mkString(", "),
										sprCode,
										group.toString,
										description.capitalize
									))
									None
								} else {
									props.headOption
								}
							}

							new UpstreamAssessmentGroupMemberProperties {
								position = resolveDuplicates(validInts(studentRegistrations.map(_.seatNumber)), "seat number")
								actualMark = resolveDuplicates(validBigDecimals(studentRegistrations.map(_.actualMark)), "actual mark")
								actualGrade = resolveDuplicates(validStrings(studentRegistrations.map(_.actualGrade)), "actual grade")
								agreedMark = resolveDuplicates(validBigDecimals(studentRegistrations.map(_.agreedMark)), "agreed mark")
								agreedGrade = resolveDuplicates(validStrings(studentRegistrations.map(_.agreedGrade)), "agreed grade")
							}
						}
					}}

					propertiesMap.foreach { case (sprCode, properties) => group.members.asScala.find(_.universityId == SprCode.getUniversityId(sprCode)).foreach { member =>
						member.position = properties.position
						member.actualMark = properties.actualMark
						member.actualGrade = properties.actualGrade
						member.agreedMark = properties.agreedMark
						member.agreedGrade = properties.agreedGrade
						assessmentMembershipService.save(member)
					}}
				}
			})
			assessmentGroups
		}.getOrElse(Seq())
	}


	def saveGroups(groups: Seq[UpstreamAssessmentGroup]): Unit = transactional() {
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
			for (gradeBoundary <- logSize(boundaries)) {
				assessmentMembershipService.save(gradeBoundary)
			}
		}
	}

}


trait ImportAssignmentsDescription extends Describable[Unit] {
	def describe(d: Description) {}
}

trait ImportAssignmentsAllYearsCommand extends ImportAssignmentsCommand {

	override def applyInternal() {
		assignmentImporter.yearsToImport = Seq(AcademicYear(2012))
		doGroups()
		doGroupMembers()
		assignmentImporter.yearsToImport = Seq(AcademicYear(2013))
		doGroups()
		doGroupMembers()
		assignmentImporter.yearsToImport = Seq(AcademicYear(2014))
		doGroups()
		doGroupMembers()
		assignmentImporter.yearsToImport = Seq(AcademicYear(2015))
		doGroups()
		doGroupMembers()
		assignmentImporter.yearsToImport = Seq(AcademicYear(2016))
		doGroups()
		doGroupMembers()
		assignmentImporter.yearsToImport = AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(0, 1)
	}

}