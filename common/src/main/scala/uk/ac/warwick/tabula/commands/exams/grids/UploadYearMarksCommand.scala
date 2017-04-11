package uk.ac.warwick.tabula.commands.exams.grids

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.UploadYearMarksCommand.ProcessedYearMark
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.{Department, FileAttachment, StudentCourseYearDetails}
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.coursework.docconversion.{AutowiringYearMarksExtractorComponent, YearMarkItem, YearMarksExtractorComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, SprCode}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.math.BigDecimal.RoundingMode

object UploadYearMarksCommand {

	final val MAX_MARKS_ROWS: Int = 5000

	case class ProcessedYearMark(
		scjCode: String,
		mark: BigDecimal,
		academicYear: AcademicYear,
		yearMarkItem: YearMarkItem,
		scyd: Option[StudentCourseYearDetails],
		errors: Seq[String]
	)

	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new UploadYearMarksCommandInternal(department, academicYear, user)
			with ComposableCommand[Seq[StudentCourseYearDetails]]
			with AutowiringYearMarksExtractorComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with UploadYearMarksCommandBindListener
			with UploadYearMarksDescription
			with UploadYearMarksPermissions
			with UploadYearMarksCommandState
			with UploadYearMarksCommandRequest
}


class UploadYearMarksCommandInternal(val department: Department, val academicYear: AcademicYear, user: CurrentUser)
	extends CommandInternal[Seq[StudentCourseYearDetails]] {

	self: UploadYearMarksCommandState with StudentCourseYearDetailsDaoComponent =>

	override def applyInternal(): ArrayBuffer[StudentCourseYearDetails] = {
		processedYearMarks.flatMap(item =>
			if (item.scyd.isDefined && item.errors.isEmpty) {
				item.scyd.get.agreedMark = item.mark.underlying
				item.scyd.get.agreedMarkUploadedDate = null
				item.scyd.get.agreedMarkUploadedBy = user.apparentUser
				studentCourseYearDetailsDao.saveOrUpdate(item.scyd.get)
				item.scyd
			} else {
				None
			}
		)
	}

}

trait UploadYearMarksCommandBindListener extends BindListener {

	self: UploadYearMarksCommandRequest with UploadYearMarksCommandState with YearMarksExtractorComponent
		with StudentCourseYearDetailsDaoComponent =>

	override def onBind(result: BindingResult) {
		val fileNames = file.fileNames.map(_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !validAttachmentStrings.exists(s.endsWith))

		if (invalidFiles.nonEmpty) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty) {
					processFiles(file.attached.asScala)
				}

				def processFiles(files: Seq[FileAttachment]) {
					for (file <- files.filter(_.hasData)) {
						try {
							marks.addAll(yearMarksExtractor.readXSSFExcelFile(file.dataStream))
							if (marks.size() > UploadYearMarksCommand.MAX_MARKS_ROWS) {
								result.rejectValue("file", "file.tooManyRows", Array(UploadYearMarksCommand.MAX_MARKS_ROWS.toString), "")
								marks.clear()
							}
						} catch {
							case e: InvalidFormatException =>
								result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
						}
					}
				}
			}

			postProcessYearMarks()
		}
	}

	private def postProcessYearMarks(): Unit = {
		// Deal with rows with invalid academic years
		val (validAcademicYearItems, invalidAcademicYearItems) = marks.asScala.partition(item => item.academicYear.maybeText.map(academicYearString => {
			try {
				AcademicYear.parse(academicYearString)
				true
			} catch {
				case e: Exception =>
					false
			}
		}).getOrElse(true))

		// Pair the items with the associated real academic year. If it isn't defined, use the command's
		val academicYearItems = validAcademicYearItems.map(item => (item, item.academicYear.maybeText.map(AcademicYear.parse).getOrElse(academicYear)))

		// Partition studentIds into Uni IDs and SCJ codes, the query for all of them from the DB
		val (lookupByUniID, lookupByScjCode) = academicYearItems.partition(item => SprCode.getUniversityId(item._1.studentId) == item._1.studentId)
		val studentsByScjCode = studentCourseYearDetailsDao.findByScjCodeAndAcademicYear(
			lookupByScjCode.map(item => (item._1.studentId, item._2)).toSeq
		)
		val studentsByUniId = studentCourseYearDetailsDao.findByUniversityIdAndAcademicYear(
			lookupByUniID.map(item => (item._1.studentId, item._2)).toSeq
		)

		// Translate into ProcessedYearMarks
		val parsedScjCodeItems = lookupByScjCode.map{ case (item, academicYear) =>
			val scyd = studentsByScjCode.get((item.studentId, academicYear))
			val studentErrors = validateStudent(item.studentId, scyd, isSCJCode = true)
			val (mark, markErrors) = validateMark(item.mark)
			ProcessedYearMark(scyd.map(_.studentCourseDetails.scjCode).getOrElse(item.studentId), mark, academicYear, item, scyd, studentErrors ++ markErrors)
		}
		val parsedUniIdItems = lookupByUniID.map{ case (item, academicYear) =>
			val scyd = studentsByUniId.get((item.studentId, academicYear))
			val studentErrors = validateStudent(item.studentId, scyd, isSCJCode = false)
			val (mark, markErrors) = validateMark(item.mark)
			ProcessedYearMark(scyd.map(_.studentCourseDetails.scjCode).getOrElse(item.studentId), mark, academicYear, item, scyd, studentErrors ++ markErrors)
		}

		processedYearMarks ++= parsedUniIdItems
		processedYearMarks ++= parsedScjCodeItems
		processedYearMarks ++= invalidAcademicYearItems.map(item =>
			ProcessedYearMark(item.studentId, null, null, item, None, Seq(s"Academic year ${item.academicYear} is invalid"))
		)
	}

	private def validateMark(markString: String): (BigDecimal, Seq[String]) = {
		try {
			val asBigDecimal = BigDecimal(markString).setScale(1, RoundingMode.HALF_UP)
			if (asBigDecimal < 0) {
				(null, Seq(s"Mark must be 0 or more")) // And surely less than 100, but apprently not /facepalm
			} else {
				(asBigDecimal, Seq())
			}
		} catch {
			case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
				(null, Seq(s"Could not parse mark of $markString"))
		}
	}

	private def validateStudent(studentId: String, scyd: Option[StudentCourseYearDetails], isSCJCode: Boolean): Seq[String] = {
		def departmentAndParents(department: Department): Stream[Department] = Stream(department) ++ (if (department.hasParent) departmentAndParents(department.parent) else Stream())
		if (scyd.isEmpty) {
			Seq(s"Could not find a student with ${if (isSCJCode) "SCJ code" else "University ID"} $studentId for academic year ${academicYear.toString}")
		} else if (!departmentAndParents(scyd.get.enrolmentDepartment).contains(department)) {
			Seq(s"You do not have permission to upload marks for this student. Their enrolment department is ${scyd.get.enrolmentDepartment.rootDepartment.name}")
		} else {
			Seq()
		}
	}
}

trait UploadYearMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: UploadYearMarksCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait UploadYearMarksDescription extends Describable[Seq[StudentCourseYearDetails]] {

	self: UploadYearMarksCommandState =>

	override lazy val eventName = "UploadYearMarks"

	override def describe(d: Description) {
		d.department(department).properties(Map(
			"academicYear" -> academicYear.toString
		))
	}
}

trait UploadYearMarksCommandState {
	def department: Department
	def academicYear: AcademicYear
	val validAttachmentStrings = Seq(".xlsx")
	var processedYearMarks: collection.mutable.ArrayBuffer[ProcessedYearMark] = collection.mutable.ArrayBuffer()
}

trait UploadYearMarksCommandRequest {
	var file: UploadedFile = new UploadedFile
	var marks: JList[YearMarkItem] = LazyLists.create[YearMarkItem]()
}
