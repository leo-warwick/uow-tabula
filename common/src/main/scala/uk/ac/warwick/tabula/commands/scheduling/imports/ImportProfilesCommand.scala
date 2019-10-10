package uk.ac.warwick.tabula.commands.scheduling.imports

import org.hibernate.StaleObjectStateException
import org.joda.time.DateTime
import org.springframework.validation.BindException
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandWithoutTransaction, Description, TaskBenchmarking}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, MemberDao, StudentCourseDetailsDao, StudentCourseYearDetailsDao}
import uk.ac.warwick.tabula.helpers.scheduling.ImportCommandFactory
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.elasticsearch.ProfileIndexService
import uk.ac.warwick.tabula.services.scheduling._
import uk.ac.warwick.userlookup.{AnonymousUser, User}

import scala.util.Try

class ImportProfilesCommand extends CommandWithoutTransaction[Unit] with Logging with Daoisms with SitsAcademicYearAware with TaskBenchmarking {

  type UniversityId = String

  PermissionCheck(Permissions.ImportSystemData)

  var madService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
  var profileImporter: ProfileImporter = Wire[ProfileImporter]
  var profileService: ProfileService = Wire[ProfileService]
  var userLookup: UserLookupService = Wire[UserLookupService]
  var moduleRegistrationImporter: ModuleRegistrationImporter = Wire[ModuleRegistrationImporter]
  var accreditedPriorLearningImporter: AccreditedPriorLearningImporter = Wire[AccreditedPriorLearningImporter]
  var studentCourseDetailsNoteImporter: StudentCourseDetailsNoteImporter = Wire[StudentCourseDetailsNoteImporter]
  var moduleRegistrationService: ModuleRegistrationService = Wire[ModuleRegistrationService]
  var smallGroupService: SmallGroupService = Wire[SmallGroupService]
  var profileIndexService: ProfileIndexService = Wire[ProfileIndexService]
  var memberDao: MemberDao = Wire[MemberDao]
  var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]
  var studentCourseYearDetailsDao: StudentCourseYearDetailsDao = Wire[StudentCourseYearDetailsDao]

  var deptCode: String = _
  var componentMarkYears: Seq[AcademicYear] = AcademicYear.allCurrent() :+ AcademicYear.now().next

  val BatchSize = 100

  def applyInternal() {
    if (features.profiles) {
      benchmarkTask("Import members") {
        doMemberDetails(transactional(readOnly = true) {
          madService.getDepartmentByCode(deptCode)
        }.getOrElse(
          throw new IllegalArgumentException(s"Could not find department with code $deptCode")
        ))
      }
      logger.info("Import completed")
    }
  }

  /** Import basic info about all members in Membership, batched 250 at a time (small batch size is mostly for web sign-on's benefit) */

  def doMemberDetails(department: Department) {
    logger.info("Importing member details")
    val importCommandFactory = new ImportCommandFactory

    // only import notes for engineering (query shouldn't bring back any notes other than engineering ones but no point in running this more than once
    if (department.code == "es") {
      benchmarkTask("Update student course detail notes") {
        transactional() {
          updateStudentCourseDetailsNotes()
        }
      }
    }

    logSize(profileImporter.membershipInfoByDepartment(department)).grouped(BatchSize).zipWithIndex.toSeq.foreach { case (membershipInfos, batchNumber) =>
      benchmarkTask(s"Import member details for department=${department.code}, batch=#${batchNumber + 1}") {
        val users: Map[UniversityId, User] =
          if (department.code == ProfileImporter.applicantDepartmentCode)
            membershipInfos.map { m =>
              val user = new AnonymousUser
              user.setUserId(m.member.universityId)
              user.setWarwickId(m.member.universityId)
              m.member.universityId -> new AnonymousUser()
            }.toMap
          else benchmarkTask("Fetch user details") {
            logger.info(s"Fetching user details for ${membershipInfos.size} ${department.code} usercodes from websignon (batch #${batchNumber + 1})")

            val usersByWarwickIds =
              userLookup.getUsersByWarwickUniIds(membershipInfos.map(_.member.universityId))
                .collect { case (universityId, FoundUser(u)) => universityId -> u }

            membershipInfos.map { m =>
              val (usercode, warwickId) = (m.member.usercode, m.member.universityId)

              m.member.universityId -> usersByWarwickIds.getOrElse(warwickId, userLookup.getUserByUserId(usercode))
            }.toMap
          }

        logger.info(s"Fetching member details for ${membershipInfos.size} ${department.code} members (batch #${batchNumber + 1})")
        val importMemberCommands = benchmarkTask("Fetch member details") {
          transactional() {
            profileImporter.getMemberDetails(membershipInfos, users, importCommandFactory)
          }
        }

        logger.info(s"Updating members for department=${department.code}, batch=#${batchNumber + 1}")
        benchmarkTask("Update members") {
          // each apply has its own transaction
          transactional() {
            importMemberCommands.foreach(cmd => Try(cmd.apply()).recover {
              case e: StaleObjectStateException =>
                logger.error(s"Tried to import ${cmd.universityId} in department ${department.code} but member was already imported")
                logger.error(e.getMessage)
              case e =>
                logger.error(e.getMessage)
                throw e
            })
            session.flush()
          }
        }

        benchmarkTask("Update visa fields on StudentCourseYearDetails records") {
          transactional() {
            updateVisa(importMemberCommands)
          }
        }

        benchmarkTask("Update hall of residence for student") {
          transactional() {
            updateAddress(importMemberCommands)
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

        benchmarkTask("Handle deceased students") {
          transactional() {
            handleDeceasedStudents(importMemberCommands)
          }
        }

        transactional() {
          val members = importMemberCommands.map(_.universityId).distinct.flatMap(u => memberDao.getByUniversityId(u))
          members.foreach(member => {
            member.lastImportDate = DateTime.now
            memberDao.saveOrUpdate(member)
          })

          profileIndexService.indexItemsWithoutNewTransaction(members)
        }
      }
    }
  }

  private def toStudentMembers(rowCommands: Seq[ImportMemberCommand]): Seq[StudentMember] = {
    memberDao.getAllWithUniversityIds(rowCommands.collect { case s: ImportStudentRowCommandInternal => s }.map(_.universityId))
      .collect { case s: StudentMember => s }
  }

  private def toStudentOrApplicantMembers(rowCommands: Seq[ImportMemberCommand]): Seq[Member] = {
    memberDao.getAllWithUniversityIds(rowCommands.collect { case s@(_: ImportStudentRowCommandInternal | _: ImportOtherMemberCommand) => s }.map(_.universityId))
      .collect { case s@(_: StudentMember | _: ApplicantMember) => s }
  }

  def updateModuleRegistrationsAndSmallGroups(membershipInfo: Seq[MembershipInformation], users: Map[UniversityId, User]): Seq[ModuleRegistration] = {
    logger.info("Fetching module registrations")

    val importModRegCommands = benchmarkTask("Get module registrations details for users") {
      moduleRegistrationImporter.getModuleRegistrationDetails(membershipInfo, users)
    }

    logger.info("Saving or updating module registrations")

    val newModuleRegistrations = benchmarkTask("Save or update module registrations") {
      importModRegCommands.flatMap(_.apply())
    }

    val usercodesProcessed: Seq[String] = membershipInfo.map(_.member.usercode)

    logger.info("Removing old module registrations")

    benchmarkTask("Delete old module registrations") {
      deleteOldModuleRegistrations(usercodesProcessed, newModuleRegistrations)
    }

    session.flush()
    session.clear()

    newModuleRegistrations
  }

  def updateAccreditedPriorLearning(membershipInfo: Seq[MembershipInformation], users: Map[UniversityId, User]): Seq[AccreditedPriorLearning] = {

    val importAccreditedPriorLearningCommands = accreditedPriorLearningImporter.getAccreditedPriorLearning(membershipInfo, users)

    importAccreditedPriorLearningCommands.flatMap(_.apply())
  }

  def updateStudentCourseDetailsNotes(): Seq[StudentCourseDetailsNote] = {
    studentCourseDetailsNoteImporter.getStudentCourseDetailsNotes.flatMap(_.apply())
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

    toStudentMembers(rowCommands).foreach(student => ImportTier4ForStudentCommand(student, getCurrentSitsAcademicYear).apply())

    session.flush()
    session.clear()
  }


  def updateAddress(rowCommands: Seq[ImportMemberCommand]) {
    logger.info("Updating address")

    toStudentOrApplicantMembers(rowCommands).foreach(member => ImportAddressCommand(member).apply())

    session.flush()
    session.clear()
  }

  def rationaliseRelationships(rowCommands: Seq[ImportMemberCommand]): Unit = {
    logger.info("Updating relationships")

    toStudentMembers(rowCommands).foreach { student =>
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
    }

    session.flush()
    session.clear()
  }

  def handleDeceasedStudents(rowCommands: Seq[ImportMemberCommand]): Unit = {
    toStudentMembers(rowCommands).filter(_.deceased).foreach { student => HandleDeceasedStudentCommand(student).apply() }

    session.flush()
    session.clear()
  }

  def refresh(universityId: String, userId: Option[String]): Option[Member] = {
    transactional() {
      val user = userLookup.getUserByWarwickUniId(universityId) match {
        case FoundUser(u) => u
        case _ => userId.map(userLookup.getUserByUserId).getOrElse(new AnonymousUser)
      }

      val importCommandFactory = new ImportCommandFactory

      // Either info from uow_current_members in FIM or from Tabula if the member doesn't exist in FIM
      val membershipInfo = profileImporter.membershipInfoForIndividual(universityId).orElse(
        memberDao.getByUniversityIdStaleOrFresh(universityId).map(MembershipInformation.apply)
      )

      membershipInfo match {
        case Some(membInfo: MembershipInformation) =>

          // retrieve details for this student from SITS and store the information in Tabula
          val importMemberCommands = profileImporter.getMemberDetails(List(membInfo), Map(universityId -> user), importCommandFactory)
          if (importMemberCommands.isEmpty) logger.warn("Refreshing student " + membInfo.member.universityId + " but found no data to import.")
          val members = importMemberCommands.map(_.apply())

          session.flush()

          updateVisa(importMemberCommands)
          updateAddress(importMemberCommands)

          // re-import module registrations and delete old module and group registrations:
          val newModuleRegistrations = updateModuleRegistrationsAndSmallGroups(List(membInfo), Map(universityId -> user))
          updateComponentMarks(List(membInfo))
          updateAccreditedPriorLearning(List(membInfo), Map(universityId -> user))
          rationaliseRelationships(importMemberCommands)

          handleDeceasedStudents(importMemberCommands)

          val freshMembers = members.flatMap { m => profileService.getMemberByUniversityId(m.universityId) }

          // TAB-1435 refresh profile index
          profileIndexService.indexItemsWithoutNewTransaction(freshMembers)

          freshMembers.foreach(member => {
            member.lastImportDate = DateTime.now
            memberDao.saveOrUpdate(member)
          })

          for (thisMember <- members) session.evict(thisMember)
          for (modReg <- newModuleRegistrations) session.evict(modReg)

          logger.info("Data refreshed for " + universityId)
          members.headOption
        case None =>
          logger.warn("Student is not in uow_current_members in membership or an existing Tabula member - not updating")
          None
      }

      // update missingFromSitsSince field
      updateMissingForIndividual(universityId)
    }
  }

  def updateMissingForStaffOrApplicant(member: Member): Member = {
    val missingFromImport: Boolean = member match {
      case _: ApplicantMember => profileImporter.getApplicantMemberFromSits(member.universityId).isEmpty
      case _: StaffMember => profileImporter.getUniversityIdsPresentInMembership(Set(member.universityId)).isEmpty
      case _ => throw new IllegalArgumentException("This function is only supposed to handle Applicant and Staff member.")
    }
    if (!member.stale && missingFromImport) {
      // The member has gone missing
      member.missingFromImportSince = DateTime.now
      memberDao.saveOrUpdate(member)
    } else if (member.stale && !missingFromImport) {
      // The member has re-appeared
      member.missingFromImportSince = null
      memberDao.saveOrUpdate(member)
    } else if (member.stale && missingFromImport && member.activeNow) {
      // TAB-7196 - Normally users are marked as withdrawn or inactive in an upstream system (FIM or SITS) and that status is then imported into Tabula
      // Occasionally they are removed before that happens which means that the Member would incorrectly appear as "Active" in Tabula forever
      member.inUseFlag = "Inactive"
      memberDao.saveOrUpdate(member)
    }
    member
  }

  def updateMissingForIndividual(universityId: String): Option[Member] = {
    profileService.getMemberByUniversityIdStaleOrFresh(universityId).flatMap {
      case member@(_: StaffMember | _: ApplicantMember) => Some(updateMissingForStaffOrApplicant(member))
      case stu: StudentMember =>
        val sitsRows = profileImporter.sitsStudentRows(Seq(universityId))
        val universityIdsSeen = sitsRows.map(_.universityId.getOrElse("")).distinct
        val scjCodesSeen = sitsRows.map(_.scjCode).distinct
        val studentCourseYearKeysSeen = sitsRows.map(row => new StudentCourseYearKey(row.scjCode, row.sceSequenceNumber)).distinct

        // update missingFromImportSince on member
        if (stu.stale && universityIdsSeen.contains(stu.universityId)) {
          stu.missingFromImportSince = null
          memberDao.saveOrUpdate(stu)
        }
        else if (!stu.stale && !universityIdsSeen.contains(stu.universityId)) {
          var missingSince = stu.missingFromImportSince
          stu.missingFromImportSince = DateTime.now
          missingSince = stu.missingFromImportSince
          memberDao.saveOrUpdate(stu)
        } else if (stu.stale && !universityIdsSeen.contains(stu.universityId) && stu.activeNow) {
          // TAB-7196 - Normally students are marked as withdrawn in SITS and that status is then imported into Tabula
          // Occasionally they are removed before that happens which means that the Member would appear as "Active" in Tabula forever
          stu.inUseFlag = "Inactive"
          memberDao.saveOrUpdate(stu)
        }

        for (scd <- stu.freshOrStaleStudentCourseDetails) {
          // on studentCourseDetails
          if (scd.missingFromImportSince != null && scjCodesSeen.contains(scd.scjCode)) {
            scd.missingFromImportSince = null
            studentCourseDetailsDao.saveOrUpdate(scd)
          } else if (!scd.stale && !scjCodesSeen.contains(scd.scjCode)) {
            scd.missingFromImportSince = DateTime.now
            studentCourseDetailsDao.saveOrUpdate(scd)
          }

          // and on studentCourseYearDetails
          for (scyd <- scd.freshOrStaleStudentCourseYearDetails) {
            val key = new StudentCourseYearKey(scd.scjCode, scyd.sceSequenceNumber)
            if (scyd.missingFromImportSince != null && studentCourseYearKeysSeen.contains(key)) {
              scyd.missingFromImportSince = null
              studentCourseYearDetailsDao.saveOrUpdate(scyd)
            } else if (!scyd.stale && !studentCourseYearKeysSeen.contains(key)) {
              scyd.missingFromImportSince = DateTime.now
              studentCourseYearDetailsDao.saveOrUpdate(scyd)
            }
          }
        }
        Some(stu)
      case _ => None
    }
  }

  def deleteOldModuleRegistrations(usercodes: Seq[String], newModuleRegistrations: Seq[ModuleRegistration]) {
    val existingModuleRegistrations = moduleRegistrationService.getByUsercodesAndYear(usercodes, getCurrentSitsAcademicYear)
    for (existingMR <- existingModuleRegistrations.filterNot(mr => newModuleRegistrations.contains(mr))) {
      existingMR.studentCourseDetails.removeModuleRegistration(existingMR)
      session.delete(existingMR)

      if (features.autoGroupDeregistration) {
        smallGroupService.removeFromSmallGroups(existingMR)
      }
    }
  }

  def updateComponentMarks(membershipInfo: Seq[MembershipInformation]): Unit = {
    val studentMembers = membershipInfo.map(_.member).filter(_.userType == Student)

    if (studentMembers.nonEmpty) {
      logger.info("Updating component marks")
      ImportAssignmentsCommand.applyForMembers(studentMembers, componentMarkYears).apply()

      session.flush()
      session.clear()
    } else logger.info("No students - so not updating component marks")
  }

  def describe(d: Description): Unit = d.property("deptCode" -> deptCode)

  // Makes the related event easier to spot in the logs
  override def describeResult(d: Description, result: Unit): Unit = d.property("deptCode" -> deptCode)
}
