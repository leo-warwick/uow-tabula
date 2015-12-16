package uk.ac.warwick.tabula.scheduling.commands.imports

import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, TaskBenchmarking}
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model.{StudentMember, _}
import uk.ac.warwick.tabula.data.{MemberDao, ModuleRegistrationDaoImpl, StudentCourseDetailsDao, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.helpers.{ImportCommandFactory, ImportRowTracker}
import uk.ac.warwick.tabula.scheduling.services.{AccreditedPriorLearningImporter, MembershipInformation, ModuleRegistrationImporter, ProfileImporter, SitsAcademicYearAware}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileIndexService, ProfileService, SmallGroupService, UserLookupService}
import uk.ac.warwick.userlookup.{AnonymousUser, User}

class ImportProfilesCommand extends Command[Unit] with Logging with SitsAcademicYearAware with TaskBenchmarking {

	type UniversityId = String

	PermissionCheck(Permissions.ImportSystemData)

	var madService = Wire[ModuleAndDepartmentService]
	var profileImporter = Wire[ProfileImporter]
	var profileService = Wire[ProfileService]
	var userLookup = Wire[UserLookupService]
	var moduleRegistrationImporter = Wire[ModuleRegistrationImporter]
	var accreditedPriorLearningImporter = Wire[AccreditedPriorLearningImporter]
	var moduleRegistrationDao = Wire[ModuleRegistrationDaoImpl]
	var smallGroupService = Wire[SmallGroupService]
	var profileIndexService = Wire[ProfileIndexService]
	var memberDao = Wire[MemberDao]
	var studentCourseDetailsDao = Wire[StudentCourseDetailsDao]
	var studentCourseYearDetailsDao = Wire[StudentCourseYearDetailsDao]

	var deptCode: String = _

	val BatchSize = 100

	def applyInternal() {
		if (features.profiles) {
			benchmarkTask("Import members") {
				doMemberDetails(madService.getDepartmentByCode(deptCode))
			}
			logger.info("Import completed")
		}
	}

	/** Import basic info about all members in Membership, batched 250 at a time (small batch size is mostly for web sign-on's benefit) */

	def doMemberDetails(department : Option[Department]) {
		logger.info("Importing member details")
		val importCommandFactory = new ImportCommandFactory
		val importStart = DateTime.now

		val departments = department match {
			case Some(d) => Seq(d)
			case None => madService.allDepartments
		}

		departments.foreach { department =>
			logSize(profileImporter.membershipInfoByDepartment(department)).grouped(BatchSize).zipWithIndex.toSeq.par.foreach { case (membershipInfos, batchNumber) =>
				benchmarkTask(s"Import member details for department=${department.code}, batch=#${batchNumber + 1}") {
					logger.info(s"Fetching user details for ${membershipInfos.size} ${department.code} usercodes from websignon")

					val users: Map[UniversityId, User] =
						benchmarkTask("Fetch user details") {
							membershipInfos.map { m =>
								val (usercode, warwickId) = (m.member.usercode, m.member.universityId)

								val user = userLookup.getUserByWarwickUniIdUncached(warwickId, skipMemberLookup = true) match {
									case FoundUser(u) => u
									case _ => userLookup.getUserByUserId(usercode)
								}

								m.member.universityId -> user
							}.toMap
						}

					logger.info(s"Fetching member details for ${membershipInfos.size} ${department.code} members from Membership")
					val importMemberCommands = benchmarkTask("Fetch member details") {
						transactional() {
							profileImporter.getMemberDetails(membershipInfos, users, importCommandFactory)
						}
					}

					logger.info("Updating members")
					benchmarkTask("Update members") {
					// each apply has its own transaction
						transactional() {
							importMemberCommands map { _.apply() }
							memberDao.flush()
						}
					}

					benchmarkTask("Update visa fields on StudentCourseYearDetails records") {
						transactional() {
							updateVisa(importMemberCommands)
						}
					}

					benchmarkTask("Update module registrations and small groups") {
						transactional() {
							updateModuleRegistrationsAndSmallGroups(membershipInfos, users)
						}
					}

					benchmarkTask("Update accredited prior learning") {
						transactional() {
							updateAccreditedPriorLearning(membershipInfos, users)
						}
					}

					benchmarkTask("Rationalise relationships") {
						transactional() {
							rationaliseRelationships(importMemberCommands)
						}
					}

					transactional() {
						importMemberCommands.map(_.universityId).distinct.flatMap(u => memberDao.getByUniversityId(u)).foreach(member => {
							member.lastImportDate = DateTime.now
							memberDao.saveOrUpdate(member)
						})
					}
				}
			}
		}

		if (department.isEmpty)
			benchmarkTask("Stamp missing rows") {
				stampMissingRows(importCommandFactory.rowTracker, importStart)
		}
	}

	def stampMissingRows(importRowTracker: ImportRowTracker, importStart: DateTime) {
		transactional() {
			logger.warn("Timestamping missing rows.  Found "
					+ importRowTracker.universityIdsSeen.size + " students, "
					+ importRowTracker.scjCodesSeen.size + " studentCourseDetails, "
					+ importRowTracker.studentCourseYearDetailsSeen.size + " studentCourseYearDetails.")

			// make sure any rows we've got for this student in the db which we haven't seen in this import are recorded as missing
			logger.warn("Timestamping missing students")
			memberDao.stampMissingFromImport(importRowTracker.newStaleUniversityIds, importStart)

			logger.warn("Timestamping missing studentCourseDetails")
			studentCourseDetailsDao.stampMissingFromImport(importRowTracker.newStaleScjCodes, importStart)

			logger.warn("Timestamping missing studentCourseYearDetails")
			studentCourseYearDetailsDao.stampMissingFromImport(importRowTracker.newStaleScydIds, importStart)
		}
	}

	def updateModuleRegistrationsAndSmallGroups(membershipInfo: Seq[MembershipInformation], users: Map[UniversityId, User]): Seq[ModuleRegistration] = {
		logger.info("Fetching module registrations")

		val importModRegCommands = benchmarkTask("Get module registrations details for users") {
			moduleRegistrationImporter.getModuleRegistrationDetails(membershipInfo, users)
		}

		logger.info("Saving or updating module registrations")

		val newModuleRegistrations = benchmarkTask("Save or update module registrations") {
			importModRegCommands flatMap { _.apply() }
		}

		val usercodesProcessed: Seq[String] = membershipInfo map { _.member.usercode }

		logger.info("Removing old module registrations")

		benchmarkTask("Delete old module registrations") {
			deleteOldModuleRegistrations(usercodesProcessed, newModuleRegistrations)
		}

		memberDao.flush()
		memberDao.clear()

		newModuleRegistrations
	}

	def updateAccreditedPriorLearning(membershipInfo: Seq[MembershipInformation], users: Map[UniversityId, User]): Seq[AccreditedPriorLearning] = {

		val importAccreditedPriorLearningCommands = accreditedPriorLearningImporter.getAccreditedPriorLearning(membershipInfo, users)

		importAccreditedPriorLearningCommands flatMap {
			_.apply()
		}
	}


	// For each student in the batch, find out if they have used a CAS (Confirmation of Acceptance to Study) letter
	// (which is required to obtain a Tier 4 visa), and whether they are recorded as having a Tier 4 visa.
	//
	// This is called only after a batch of student rows are processed, and all SCYDs populated.
	// Although the visa relates to a person, CAS is associated with a particular course and time, so we store it against
	// StudentCourseYearDetails in Tabula.

	// Because SITS dates aren't always reliably updated, we just take a snapshot of visa state at the point of import
	// and, since TAB-2517, apply to all SCYDs from the current SITS year onwards. If in future, a student's visa state
	// changes, then all SCYDs from that point onwards can be updated, so we retain data at no worse than
	// academic year granularity.
	def updateVisa(rowCommands: Seq[ImportMemberCommand]) {
		logger.info("Updating visa status")

		val members = rowCommands.flatMap {
			command => memberDao.getByUniversityId(command.universityId)
		}

		members.collect {
			case student: StudentMember => ImportTier4ForStudentCommand(student, getCurrentSitsAcademicYear).apply()
		}

		memberDao.flush()
		memberDao.clear()
	}

	def rationaliseRelationships(rowCommands: Seq[ImportMemberCommand]): Unit = {
		logger.info("Updating relationships")

		val members = rowCommands.map(_.universityId).distinct.flatMap(u => memberDao.getByUniversityId(u))

		members.foreach {
			case student: StudentMember =>
				val expireCommand = ExpireRelationshipsOnOldCoursesCommand(student)
				val expireCommandErrors = new BindException(expireCommand, "expireCommand")
				expireCommand.validate(expireCommandErrors)
				if (!expireCommandErrors.hasErrors) {
					logger.info(s"Expiring old relationships for ${student.universityId}")
					expireCommand.apply()
				} else {
					logger.info(s"Skipping expiry of relationships for ${student.universityId} - ${expireCommandErrors.getMessage}")
				}
				val migrateCommand = MigrateMeetingRecordsFromOldRelationshipsCommand(student)
				val migrateCommandErrors = new BindException(migrateCommand, "migrateCommand")
				migrateCommand.validate(migrateCommandErrors)
				if (!migrateCommandErrors.hasErrors) {
					logger.info(s"Migrating meetings from old relationships for ${student.universityId}")
					migrateCommand.apply()
				}
			case _ =>
		}

		memberDao.flush()
		memberDao.clear()
	}

	def refresh(universityId: String, userId: Option[String]) {
		transactional() {
			val user = userLookup.getUserByWarwickUniIdUncached(universityId, skipMemberLookup = true) match {
				case FoundUser(u) => u
				case _ => userId.map(userLookup.getUserByUserId).getOrElse(new AnonymousUser)
			}

			val importCommandFactory = new ImportCommandFactory

			profileImporter.membershipInfoForIndividual(universityId) match {
				case Some(membInfo: MembershipInformation) =>

					// retrieve details for this student from SITS and store the information in Tabula
					val importMemberCommands = profileImporter.getMemberDetails(List(membInfo), Map(universityId -> user), importCommandFactory)
					if (importMemberCommands.isEmpty) logger.warn("Refreshing student " + membInfo.member.universityId + " but found no data to import.")
					val members = importMemberCommands map { _.apply() }

					// update missingFromSitsSince field in this student's member and course records:
					updateMissingForIndividual(universityId, importCommandFactory.rowTracker)

					memberDao.flush()

					updateVisa(importMemberCommands)

					// re-import module registrations and delete old module and group registrations:
					val newModuleRegistrations = updateModuleRegistrationsAndSmallGroups(List(membInfo), Map(universityId -> user))
					updateAccreditedPriorLearning(List(membInfo), Map(universityId -> user))
					rationaliseRelationships(importMemberCommands)

					val freshMembers = members.flatMap { m => profileService.getMemberByUniversityId(m.universityId) }
					// TAB-1435 refresh profile index
					profileIndexService.indexItemsWithoutNewTransaction(freshMembers)

					freshMembers.foreach(member => {
						member.lastImportDate = DateTime.now
						memberDao.saveOrUpdate(member)
					})

					for (thisMember <- members) memberDao.evict(thisMember)
					for (modReg <- newModuleRegistrations) moduleRegistrationDao.evict(modReg)

					logger.info("Data refreshed for " + universityId)
				case None => logger.warn("Student is no longer in uow_current_members in membership - not updating")
			}
		}
	}

	def updateMissingForIndividual(universityId: String, importRowTracker: ImportRowTracker): Unit = {
		profileService.getMemberByUniversityIdStaleOrFresh(universityId).foreach { member =>
			updateMissingForIndividual(member, importRowTracker)
		}
	}

	/*
	 * Called after refreshing an individual student.
	 *
	 * For any rows for this member which were previously not seen in the import but have now re-emerged,
	 * record that fact by setting missingFromSitsImportSince to null.
	 *
	 * Also, for any rows for this member which were previously seen but which are now missing,
	 * record that fact by setting missingFromSiteImportSince to now.
	 */
	def updateMissingForIndividual(member: Member, importRowTracker: ImportRowTracker) {
		member match {
			case stu: StudentMember =>

				// update missingFromImportSince on member
				if (stu.missingFromImportSince != null && importRowTracker.universityIdsSeen.contains(stu.universityId)) {
					stu.missingFromImportSince = null
					memberDao.saveOrUpdate(stu)
				}
				else if (stu.missingFromImportSince == null && !importRowTracker.universityIdsSeen.contains(stu.universityId)) {
					var missingSince = stu.missingFromImportSince
					stu.missingFromImportSince = DateTime.now
					missingSince = stu.missingFromImportSince

					memberDao.saveOrUpdate(stu)
				}

				for (scd <- stu.freshOrStaleStudentCourseDetails) {

					// on studentCourseDetails
					if (scd.missingFromImportSince != null
							&& importRowTracker.scjCodesSeen.contains(scd.scjCode)) {
						scd.missingFromImportSince = null
						studentCourseDetailsDao.saveOrUpdate(scd)
					}
					else if (scd.missingFromImportSince == null
							&& !importRowTracker.scjCodesSeen.contains(scd.scjCode)) {
						scd.missingFromImportSince = DateTime.now
						studentCourseDetailsDao.saveOrUpdate(scd)
					}

					// and on studentCourseYearDetails
					for (scyd <- scd.freshOrStaleStudentCourseYearDetails) {
						val key = new StudentCourseYearKey(scd.scjCode, scyd.sceSequenceNumber)
						if (scyd.missingFromImportSince != null
								&& importRowTracker.studentCourseYearDetailsSeen.contains(key)) {
							scyd.missingFromImportSince = null
							studentCourseYearDetailsDao.saveOrUpdate(scyd)
						}
						else if (scyd.missingFromImportSince == null
								&& !importRowTracker.studentCourseYearDetailsSeen.contains(key)) {
							scyd.missingFromImportSince = DateTime.now
							studentCourseYearDetailsDao.saveOrUpdate(scyd)
						}
					}
				}
			case _ =>
		}
	}

	def deleteOldModuleRegistrations(usercodes: Seq[String], newModuleRegistrations: Seq[ModuleRegistration]) {
		val existingModuleRegistrations = moduleRegistrationDao.getByUsercodesAndYear(usercodes, getCurrentSitsAcademicYear)
		for (existingMR <- existingModuleRegistrations.filterNot(mr => newModuleRegistrations.contains(mr))) {
			existingMR.studentCourseDetails.removeModuleRegistration(existingMR)
			moduleRegistrationDao.delete(existingMR)

			if (features.autoGroupDeregistration) {
				smallGroupService.removeFromSmallGroups(existingMR)
			}
		}
	}

	def describe(d: Description) = d.property("deptCode" -> deptCode)
}
