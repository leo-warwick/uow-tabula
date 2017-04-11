package uk.ac.warwick.tabula.commands.scheduling.imports

import org.hibernate.exception.ConstraintViolationException
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, MemberDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.{ImportCommandFactory, PropertyCopying, SitsStudentRow}
import uk.ac.warwick.tabula.services.scheduling.{AwardImporter, CourseImporter}
import uk.ac.warwick.tabula.services.{AwardService, CourseAndRouteService, RelationshipService}

class ImportStudentCourseCommand(rows: Seq[SitsStudentRow], stuMem: StudentMember, importCommandFactory: ImportCommandFactory)
	extends Command[StudentCourseDetails] with Logging with Daoisms
	with Unaudited with PropertyCopying {

	var memberDao: MemberDao = Wire[MemberDao]
	var relationshipService: RelationshipService = Wire[RelationshipService]
	var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]
	var courseAndRouteService: CourseAndRouteService = Wire[CourseAndRouteService]
	var awardService: AwardService = Wire[AwardService]
	var courseImporter: CourseImporter = Wire[CourseImporter]
	var awardImporter: AwardImporter = Wire[AwardImporter]
	
	val courseRow: SitsStudentRow = rows.head

	override def applyInternal(): StudentCourseDetails = {

		logger.debug("Importing student course details for " + courseRow.scjCode)

		val studentCourseDetailsExisting = studentCourseDetailsDao.getByScjCodeStaleOrFresh(courseRow.scjCode)

		val (isTransient, studentCourseDetails) = studentCourseDetailsExisting match {
			case Some(studentCourseDetails: StudentCourseDetails) => (false, studentCourseDetails)
			case _ => (true, new StudentCourseDetails(stuMem, courseRow.scjCode))
		}
		
		updateStudentCourseDetails(studentCourseDetails, isTransient)

		rows.foreach(row => {
			// Update the db:
			val studentCourseYearDetails = importCommandFactory.createImportStudentCourseYearCommand(row, studentCourseDetails).apply()

			// then bring the in-memory data up to speed:
			studentCourseDetails.attachStudentCourseYearDetails(studentCourseYearDetails)
		})

		studentCourseDetails
	}
	

	def updateStudentCourseDetails(studentCourseDetails: StudentCourseDetails, isTransient: Boolean) {
		val studentCourseDetailsBean = new BeanWrapperImpl(studentCourseDetails)

		val hasChanged = copyStudentCourseProperties(new BeanWrapperImpl(courseRow), studentCourseDetailsBean) | markAsSeenInSits(studentCourseDetailsBean)

		if (isTransient || hasChanged) {
			try {
				logger.debug("Saving changes for " + studentCourseDetails)

				if (courseRow.mostSignificant) {
					stuMem.mostSignificantCourse = studentCourseDetails
					logger.debug("Updating member most significant course to " + studentCourseDetails + " for " + stuMem)
				}

				studentCourseDetails.lastUpdatedDate = DateTime.now
				studentCourseDetailsDao.saveOrUpdate(studentCourseDetails)
			}
			catch {
				case exception: ConstraintViolationException =>
					logger.warn("Couldn't update course details for SCJ "
						+ studentCourseDetails.scjCode + ", SPR " + studentCourseDetails.sprCode
						+ ".  Might be invalid data in SITS - working on the assumption "
						+ "there shouldn't be multiple SPR codes for one current SCJ code")
					exception.printStackTrace()
			}
		}

		updateRelationships(studentCourseDetails)
	}

	def updateRelationships(studentCourseDetails: StudentCourseDetails) {
		// Check the SPR (status on route) code to see if they are permanently withdrawn, and
		// end relationships if so.  The SCJ code may indicate that they are
		// permanently withdrawn from the course, but it may have the same route code as their
		// current course (surprisingly).  In that case we don't want to go ahead and end all
		// relationships for the route code.
		if (courseRow.sprStatusCode != null && courseRow.sprStatusCode.startsWith("P")) {
			// they are permanently withdrawn
			endRelationships()
		}
		else {
			if (courseRow.endDate == null || courseRow.endDate.isAfter(DateTime.now.toLocalDate)) {
				captureTutor(studentCourseDetails)
			}

			if (courseRow.scjCode != null && courseRow.scjStatusCode != null && !courseRow.scjStatusCode.startsWith("P"))
				new ImportSupervisorsForStudentCommand(studentCourseDetails).apply()
		}
	}

	private val basicStudentCourseProperties = Set(
		"sprCode",
		"scjCode",
		"beginDate",
		"endDate",
		"expectedEndDate",
		"courseYearLength",
		"mostSignificant",
		"levelCode",
		"reasonForTransferCode"
	)

	private def copyStudentCourseProperties(rowBean: BeanWrapper, studentCourseDetailsBean: BeanWrapper) = {
		copyBasicProperties(basicStudentCourseProperties, rowBean, studentCourseDetailsBean) |
		copyObjectProperty("department", courseRow.departmentCode, studentCourseDetailsBean, toDepartment(courseRow.departmentCode)) |
		copyObjectProperty("currentRoute", courseRow.routeCode, studentCourseDetailsBean, courseAndRouteService.getRouteByCode(courseRow.routeCode)) |
		copyObjectProperty("course", courseRow.courseCode, studentCourseDetailsBean, courseImporter.getCourseByCodeCached(courseRow.courseCode)) |
		copyObjectProperty("award", courseRow.awardCode, studentCourseDetailsBean, awardImporter.getAwardByCodeCached(courseRow.awardCode)) |
		copyObjectProperty("statusOnRoute", courseRow.sprStatusCode, studentCourseDetailsBean, toSitsStatus(courseRow.sprStatusCode)) |
		copyObjectProperty("statusOnCourse", courseRow.scjStatusCode, studentCourseDetailsBean, toSitsStatus(courseRow.scjStatusCode)) |
		copyAcademicYear("sprStartAcademicYear", courseRow.sprStartAcademicYearString, studentCourseDetailsBean)
	}

	def captureTutor(studentCourseDetails: StudentCourseDetails): Unit = {
		val dept = studentCourseDetails.department
		if (dept == null)
			logger.warn("Trying to capture tutor for " + courseRow.sprCode + " but department is null.")

		// Mark Hadley in Physics says "I don't think the University uses the term 'tutor' for PGRs"
		// so by default excluding PGRs from the personal tutor import:
		else if (courseRow.courseCode != null && courseRow.courseCode.length() > 0 && CourseType.fromCourseCode(courseRow.courseCode) != CourseType.PGR) {
			// is this student in a department that is set to import tutor data from SITS?
			relationshipService
				.getStudentRelationshipTypeByUrlPart("tutor") // TODO this is awful
				.filter { relType => dept.getStudentRelationshipSource(relType) == StudentRelationshipSource.SITS }
				.foreach { relationshipType =>
					// only save the personal tutor if we can match the ID with a staff member in Tabula
					memberDao.getByUniversityIdStaleOrFresh(courseRow.tutorUniId) match {
						case Some(mem: Member) =>
							logger.info("Got a personal tutor from SITS! SprCode: " + courseRow.sprCode + ", tutorUniId: " + courseRow.tutorUniId)

							relationshipService.replaceStudentRelationships(relationshipType, studentCourseDetails, mem, DateTime.now)
						case _ =>
							logger.warn(
								"SPR code: "
									+ courseRow.sprCode
									+ ": no staff member found for uni ID "
									+ courseRow.tutorUniId
									+ " - not importing this personal tutor from SITS"
							)
					}
				}
		}
	}


	def endRelationships() {
		if (courseRow.endDate != null) {
			val endDateFromSits = courseRow.endDate.toDateTimeAtCurrentTime
			val threeMonthsAgo = DateTime.now().minusMonths(3)
			if (endDateFromSits.isBefore(threeMonthsAgo)) {
				relationshipService.getAllCurrentRelationships(stuMem)
					.filter { relationship => relationship.studentCourseDetails.sprCode == courseRow.sprCode }
					.foreach { relationship =>
							relationship.endDate = endDateFromSits
							relationshipService.saveOrUpdate(relationship)
						}
			}
		}
	}

	override def describe(d: Description): Unit = d.property("scjCode" -> courseRow.scjCode)
}
