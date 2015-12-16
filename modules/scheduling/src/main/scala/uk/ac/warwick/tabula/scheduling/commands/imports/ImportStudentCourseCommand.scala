package uk.ac.warwick.tabula.scheduling.commands.imports

import org.hibernate.exception.ConstraintViolationException
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, BeanWrapperImpl}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description, Unaudited}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{MemberDao, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.scheduling.helpers.{ImportCommandFactory, PropertyCopying, SitsStudentRow}
import uk.ac.warwick.tabula.scheduling.services.{AwardImporter, CourseImporter}
import uk.ac.warwick.tabula.services.{AwardService, CourseAndRouteService, RelationshipService}

class ImportStudentCourseCommand(row: SitsStudentRow, stuMem: StudentMember, importCommandFactory: ImportCommandFactory)
	extends Command[StudentCourseDetails] with Logging
	with Unaudited with PropertyCopying {

	var memberDao = Wire[MemberDao]
	var relationshipService = Wire[RelationshipService]
	var studentCourseDetailsDao = Wire[StudentCourseDetailsDao]
	var courseAndRouteService = Wire[CourseAndRouteService]
	var awardService = Wire[AwardService]
	var courseImporter = Wire[CourseImporter]
	var awardImporter = Wire[AwardImporter]

	override def applyInternal(): StudentCourseDetails = {

		logger.debug("Importing student course details for " + row.scjCode)

		val studentCourseDetailsExisting = studentCourseDetailsDao.getByScjCodeStaleOrFresh(row.scjCode)

		val (isTransient, studentCourseDetails) = studentCourseDetailsExisting match {
			case Some(studentCourseDetails: StudentCourseDetails) => (false, studentCourseDetails)
			case _ => (true, new StudentCourseDetails(stuMem, row.scjCode))
		}

		if (!importCommandFactory.rowTracker.scjCodesSeen.contains(studentCourseDetails.scjCode)) {
			updateStudentCourseDetails(studentCourseDetails, isTransient)
		}

		updateStudentCourseYearDetails(studentCourseDetails)

		importCommandFactory.rowTracker.scjCodesSeen.add(studentCourseDetails.scjCode)

		studentCourseDetails
	}


	def updateStudentCourseYearDetails(studentCourseDetails: StudentCourseDetails) {
		// Update the db:
		val studentCourseYearDetails = importCommandFactory.createImportStudentCourseYearCommand(row, studentCourseDetails).apply()

		// then bring the in-memory data up to speed:
		studentCourseDetails.attachStudentCourseYearDetails(studentCourseYearDetails)
	}

	def updateStudentCourseDetails(studentCourseDetails: StudentCourseDetails, isTransient: Boolean) {
		val studentCourseDetailsBean = new BeanWrapperImpl(studentCourseDetails)

		val hasChanged = copyStudentCourseProperties(new BeanWrapperImpl(row), studentCourseDetailsBean) | markAsSeenInSits(studentCourseDetailsBean)

		if (isTransient || hasChanged) {
			try {
				logger.debug("Saving changes for " + studentCourseDetails)

				if (row.mostSignificant) {
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
		if (row.sprStatusCode != null && row.sprStatusCode.startsWith("P")) {
			// they are permanently withdrawn
			endRelationships()
		}
		else {
			if (row.endDate == null || row.endDate.isAfter(DateTime.now.toLocalDate)) {
				captureTutor(studentCourseDetails)
			}

			if (row.scjCode != null && row.scjStatusCode != null && !row.scjStatusCode.startsWith("P"))
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
		copyObjectProperty("department", row.departmentCode, studentCourseDetailsBean, toDepartment(row.departmentCode)) |
		copyObjectProperty("route", row.routeCode, studentCourseDetailsBean, courseAndRouteService.getRouteByCode(row.routeCode)) |
		copyObjectProperty("course", row.courseCode, studentCourseDetailsBean, courseImporter.getCourseByCodeCached(row.courseCode)) |
		copyObjectProperty("award", row.awardCode, studentCourseDetailsBean, awardImporter.getAwardByCodeCached(row.awardCode)) |
		copyObjectProperty("statusOnRoute", row.sprStatusCode, studentCourseDetailsBean, toSitsStatus(row.sprStatusCode))
		copyObjectProperty("statusOnCourse", row.scjStatusCode, studentCourseDetailsBean, toSitsStatus(row.scjStatusCode))
	}

	def captureTutor(studentCourseDetails: StudentCourseDetails) = {
		val dept = studentCourseDetails.department
		if (dept == null)
			logger.warn("Trying to capture tutor for " + row.sprCode + " but department is null.")

		// Mark Hadley in Physics says "I don't think the University uses the term 'tutor' for PGRs"
		// so by default excluding PGRs from the personal tutor import:
		else if (row.courseCode != null && row.courseCode.length() > 0 && CourseType.fromCourseCode(row.courseCode) != CourseType.PGR) {
			// is this student in a department that is set to import tutor data from SITS?
			relationshipService
				.getStudentRelationshipTypeByUrlPart("tutor") // TODO this is awful
				.filter { relType => dept.getStudentRelationshipSource(relType) == StudentRelationshipSource.SITS }
				.foreach { relationshipType =>
					// only save the personal tutor if we can match the ID with a staff member in Tabula
					memberDao.getByUniversityId(row.tutorUniId) match {
						case Some(mem: Member) =>
							logger.info("Got a personal tutor from SITS! SprCode: " + row.sprCode + ", tutorUniId: " + row.tutorUniId)

							relationshipService.replaceStudentRelationships(relationshipType, studentCourseDetails, Seq(mem))
						case _ =>
							logger.warn(
								"SPR code: "
									+ row.sprCode
									+ ": no staff member found for uni ID "
									+ row.tutorUniId
									+ " - not importing this personal tutor from SITS"
							)
					}
				}
		}
	}


	def endRelationships() {
		if (row.endDate != null) {
			val endDateFromSits = row.endDate.toDateTimeAtCurrentTime
			val threeMonthsAgo = DateTime.now().minusMonths(3)
			if (endDateFromSits.isBefore(threeMonthsAgo)) {
				relationshipService.getAllCurrentRelationships(stuMem)
					.filter { relationship => relationship.studentCourseDetails.sprCode == row.sprCode }
					.foreach { relationship =>
							relationship.endDate = endDateFromSits
							relationshipService.saveOrUpdate(relationship)
						}
			}
		}
	}

	override def describe(d: Description) = d.property("scjCode" -> row.scjCode)
}


