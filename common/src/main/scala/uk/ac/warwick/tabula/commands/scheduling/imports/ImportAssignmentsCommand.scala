package uk.ac.warwick.tabula.commands.scheduling.imports

import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling.{AssignmentImporterComponent, AutowiringAssignmentImporterComponent, MembershipMember}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

import scala.collection.JavaConverters._
import scala.util.Try

object ImportAssignmentsCommand {
	def apply(): ComposableCommandWithoutTransaction[Unit] = new ComposableCommandWithoutTransaction[Unit]
		with ImportAssignmentsCommand
		with ImportAssignmentsAllMembers
		with ImportAssignmentsDescription
		with AutowiringAssignmentImporterComponent
		with Daoisms

	def applyAllYears(): ComposableCommandWithoutTransaction[Unit] = new ComposableCommandWithoutTransaction[Unit]
		with ImportAssignmentsAllYearsCommand
		with ImportAssignmentsAllMembers
		with ImportAssignmentsDescription
		with AutowiringAssignmentImporterComponent
		with Daoisms {
		override lazy val eventName = "ImportAssignmentsAllYears"
	}

	def applyForMembers(applyMembers: Seq[MembershipMember]): ComposableCommandWithoutTransaction[Unit] = new ComposableCommandWithoutTransaction[Unit]
		with ImportAssignmentsCommand
		with ImportAssignmentsSpecificMembers
		with ImportAssignmentsDescription
		with AutowiringAssignmentImporterComponent
		with Daoisms {
		override val members: Seq[MembershipMember] = applyMembers
	}

	case class Result(
		assignmentsFound: Int,
		assignmentsChanged: Int,
		groupsFound: Int,
		groupsChanged: Int)

}

trait ImportAssignmentsMembersToImport {
	def membersToImport(callback: UpstreamModuleRegistration => Unit): Unit

	def importEverything: Boolean
}

trait ImportAssignmentsAllMembers extends ImportAssignmentsMembersToImport {
	self: AssignmentImporterComponent =>

	override def membersToImport(callback: UpstreamModuleRegistration => Unit): Unit = {
		assignmentImporter.allMembers(callback)
	}

	val importEverything: Boolean = true
}

trait ImportAssignmentsSpecificMembers extends ImportAssignmentsMembersToImport {
	self: AssignmentImporterComponent =>

	val members: Seq[MembershipMember]

	override def membersToImport(callback: UpstreamModuleRegistration => Unit): Unit = {
		assignmentImporter.specificMembers(members)(callback)
	}

	val importEverything: Boolean = false
}

trait ImportAssignmentsCommand extends CommandInternal[Unit] with RequiresPermissionsChecking with Logging with SessionComponent with AssignmentImporterComponent with ImportAssignmentsMembersToImport {
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}

	var assessmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]
	var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

	val ImportGroupSize = 100

	var modifiedAssignments: Set[Assignment] = Set.empty

	def applyInternal() {
		benchmark("ImportAssessment") {
			if (importEverything) {
				doAssignments()
				logger.debug("Imported AssessmentComponents. Importing assessment groups...")
				doGroups()
				doGroupMembers()
				logger.debug("Imported assessment groups. Importing grade boundaries...")
				doGradeBoundaries()

				logger.debug("Removing blank feedback for students who have deregistered...")
				removeBlankFeedbackForDeregisteredStudents()
			} else {
				logger.debug("Importing assessment group members...")
				doGroupMembers()
			}
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
			membersToImport { r =>
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

			if (importEverything) {
				// empty unseen groups - this is done in transactional batches

				val groupsToEmpty = transactional(readOnly = true) {
					assessmentMembershipService.getUpstreamAssessmentGroupsNotIn(
						ids = notEmptyGroupIds.filter(_.hasText).toSeq,
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
  			.map { assessmentGroup =>
					if (importEverything) {
						assessmentMembershipService.replaceMembers(assessmentGroup, registrations)
					} else {
						assessmentMembershipService.getUpstreamAssessmentGroup(assessmentGroup).getOrElse(assessmentGroup)
					}
				}

			// Now sort out properties
			val hasSequence = registrations.filter(r => r.sequence != null)
			assessmentGroups.foreach(group => {
				// Find the registrations for this exact group (including sequence)
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
								resitActualMark = Try(BigDecimal(studentRegistrations.head.resitActualMark)).toOption
								resitActualGrade = studentRegistrations.head.resitActualGrade.maybeText
								resitAgreedMark = Try(BigDecimal(studentRegistrations.head.resitAgreedMark)).toOption
								resitAgreedGrade = studentRegistrations.head.resitAgreedGrade.maybeText
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
								resitActualMark = resolveDuplicates(validBigDecimals(studentRegistrations.map(_.resitActualMark)), "resit actual mark")
								resitActualGrade = resolveDuplicates(validStrings(studentRegistrations.map(_.resitActualGrade)), "resit actual grade")
								resitAgreedMark = resolveDuplicates(validBigDecimals(studentRegistrations.map(_.resitAgreedMark)), "resit agreed mark")
								resitAgreedGrade = resolveDuplicates(validStrings(studentRegistrations.map(_.resitAgreedGrade)), "resit agreed grade")
							}
						}
					}
					}

					propertiesMap.foreach { case (sprCode, properties) => group.members.asScala.find(_.universityId == SprCode.getUniversityId(sprCode)).foreach { member =>
						member.position = properties.position
						member.actualMark = properties.actualMark
						member.actualGrade = properties.actualGrade
						member.agreedMark = properties.agreedMark
						member.agreedGrade = properties.agreedGrade
						member.resitActualMark = properties.resitActualMark
						member.resitActualGrade = properties.resitActualGrade
						member.resitAgreedMark = properties.resitAgreedMark
						member.resitAgreedGrade = properties.resitAgreedGrade
						assessmentMembershipService.save(member)
					}
					}
				}
			})

			modifiedAssignments = modifiedAssignments ++ assessmentComponents.flatMap(_.linkedAssignments)

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

	def removeBlankFeedbackForDeregisteredStudents(): Seq[Feedback] =
		modifiedAssignments.toSeq
			.filter(_.cm2Assignment)
			.flatMap { assignment =>
				transactional() {
					// Find students who are assigned to a marker but are not a member of the assignment
					logger.info(s" checking assignment: " +  assignment.id)
					val memberUsercodes = assessmentMembershipService.determineMembershipUsers(assignment).map(_.getUserId)
					val removedMembers = assignment.allFeedback.map(_.usercode).toSet.diff(memberUsercodes.toSet)

					val removedFeedback = assignment.allFeedback
						.filter(f => removedMembers.contains(f.usercode))
						.collect {
							case feedback if feedback.hasBeenModified || assignment.findSubmission(feedback.usercode).exists(_.submitted) =>
								logger.debug(s"${feedback.usercode} is no longer a member of assignment ${assignment.id} but has submission or feedback")

								null
							case feedback =>
								logger.info(s"Removing feedback for ${feedback.usercode} from assignment ${assignment.id}")

								assignment.feedbacks.remove(feedback)
								feedback.assignment = null
								session.delete(feedback)

								feedback
						}
						.filter(_ != null)

					removedFeedback
				}
			}
}


trait ImportAssignmentsDescription extends Describable[Unit] {
	def describe(d: Description) {}
}

trait ImportAssignmentsAllYearsCommand extends ImportAssignmentsCommand {

	@Value("${tabula.yearZero}") var yearZero: Int = 2000

	override def applyInternal() {
		val next = AcademicYear.now().next.startYear
		for (year <- yearZero until next) {
			assignmentImporter.yearsToImport = Seq(AcademicYear(year))
			doGroups()
			doGroupMembers()
		}
	}

}