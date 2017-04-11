package uk.ac.warwick.tabula.commands.coursework.assignments

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.services.coursework.docconversion.{MarkItem, MarksExtractorComponent}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{FoundUser, LazyLists, NoUser}
import uk.ac.warwick.tabula.services.{SubmissionServiceComponent, ProfileServiceComponent, GeneratesGradesFromMarks, UserLookupComponent}
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._
import scala.collection.mutable

trait ValidatesMarkItem {

	self: UserLookupComponent with AddMarksCommandState =>

	def checkMarkUpdated(mark: MarkItem)

	def checkMarker(mark: MarkItem, errors: Errors, hasErrors: Boolean): Boolean = hasErrors

	def validateMarkItem(mark: MarkItem, errors: Errors, newPerson: Boolean): Boolean = {

		var hasErrors = false
		// validate id
		if (mark.universityId.hasText) {
			if (!UniversityId.isValid(mark.universityId)) {
				errors.rejectValue("universityId", "uniNumber.invalid")
				hasErrors = true
			} else if (!newPerson) {
				errors.rejectValue("universityId", "uniNumber.duplicate.mark")
				hasErrors = true
			} else {
				userLookup.getUserByWarwickUniId(mark.universityId) match {
					case FoundUser(u) =>
					case NoUser(u) =>
						errors.rejectValue("universityId", "uniNumber.userNotFound", Array(mark.universityId), "")
						hasErrors = true
				}
				checkMarkUpdated(mark: MarkItem)
				hasErrors = checkMarker(mark, errors, hasErrors)
			}
		} else {
			errors.rejectValue("universityId", "NotEmpty")
			hasErrors = true
		}
		// validate mark (must be int between 0 and 100)
		if (mark.actualMark.hasText) {
			try {
				val asInt = mark.actualMark.toInt
				if (asInt < 0 || asInt > 100) {
					errors.rejectValue("actualMark", "actualMark.range")
					hasErrors = true
				}
			} catch {
				case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
					errors.rejectValue("actualMark", "actualMark.format")
					hasErrors = true
			}
		} else if (module.adminDepartment.assignmentGradeValidation && mark.actualGrade.hasText) {
			errors.rejectValue("actualMark", "actualMark.validateGrade.adjustedGrade")
			hasErrors = true
		}

		// validate grade is department setting is true
		if (!hasErrors && mark.actualGrade.hasText && module.adminDepartment.assignmentGradeValidation) {
			val validGrades = gradeGenerator.applyForMarks(Map(mark.universityId -> mark.actualMark.toInt))(mark.universityId)
			if (validGrades.nonEmpty && !validGrades.exists(_.grade == mark.actualGrade)) {
				errors.rejectValue("actualGrade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
				hasErrors = true
			}
		}

		if (!mark.actualMark.hasText && !mark.actualGrade.hasText) {
			// If a row has no mark or grade, we will quietly ignore it
			hasErrors = true
		}
		!hasErrors
	}
}

trait PostExtractValidation {

	self: AddMarksCommandState with ValidatesMarkItem with UserLookupComponent =>

	def postExtractValidation(errors: Errors) {
		val uniIdsSoFar: mutable.Set[String] = mutable.Set()

		if (marks != null && !marks.isEmpty) {
			for (i <- 0 until marks.size()) {
				val mark = marks.get(i)
				val newPerson = if (mark.universityId != null){
					if(mark.user == null) mark.user = userLookup.getUserByWarwickUniId(mark.universityId)
					uniIdsSoFar.add(mark.universityId)
				} else {
					false
				}
				errors.pushNestedPath("marks[" + i + "]")
				mark.isValid = validateMarkItem(mark, errors, newPerson)
				errors.popNestedPath()
			}
		}
	}
}

object AddMarksCommandBindListener {
	final val MAX_MARKS_ROWS: Int = 5000
}

trait AddMarksCommandBindListener extends BindListener {

	self: AddMarksCommandState with MarksExtractorComponent =>

	override def onBind(result:BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
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
							marks.addAll(marksExtractor.readXSSFExcelFile(file.dataStream))
							if (marks.size() > AddMarksCommandBindListener.MAX_MARKS_ROWS) {
								result.rejectValue("file", "file.tooManyRows", Array(AddMarksCommandBindListener.MAX_MARKS_ROWS.toString), "")
								marks.clear()
							}
						} catch {
							case e: InvalidFormatException =>
								result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
						}
					}
				}
			}
		}
	}
}

trait AddMarksCommandState {
	def module: Module
	def assessment: Assessment
	def gradeGenerator: GeneratesGradesFromMarks

	val validAttachmentStrings = Seq(".xlsx")
	var file: UploadedFile = new UploadedFile
	var marks: JList[MarkItem] = LazyLists.create()
}

trait FetchDisabilities {

	self: AddMarksCommandState with ProfileServiceComponent with SubmissionServiceComponent =>

	def fetchDisabilities: Map[String, Disability] = {
		assessment match {
			case assignment: Assignment =>
				marks.asScala.map{ markItem => markItem.universityId -> {
					if (submissionService.getSubmissionByUsercode(assignment, markItem.user.getUserId).exists(_.useDisability)) {
						profileService.getMemberByUniversityId(markItem.universityId).flatMap {
							case student: StudentMember => Option(student)
							case _ => None
						}.flatMap(_.disability)
					} else {
						None
					}
				}}.toMap.filterNot{case(_, option) => option.isEmpty}.mapValues(_.get)
			case _ => Map()
		}
	}
}