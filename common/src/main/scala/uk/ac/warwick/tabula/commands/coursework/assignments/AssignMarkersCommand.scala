package uk.ac.warwick.tabula.commands.coursework.assignments

import org.springframework.validation.BindingResult
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.coursework.docconversion.MarkerAllocationExtractor
import uk.ac.warwick.tabula.services.coursework.docconversion.MarkerAllocationExtractor.{NoMarker, SecondMarker, FirstMarker, ParsedRow}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.{AutowiringAssessmentServiceComponent, AssessmentServiceComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.{AutowiringUserGroupDaoComponent, UserGroupDaoComponent}


object AssignMarkersCommand {
	def apply(module: Module, assessment: Assessment) =
		new AssignMarkersCommand(module, assessment)
		with ComposableCommand[Assessment]
		with AssignMarkersPermission
		with AssignMarkersDescription
		with AssignMarkersCommandState
		with AutowiringAssessmentServiceComponent
		with AutowiringUserGroupDaoComponent
}

class AssignMarkersCommand(val module: Module, val assessment: Assessment)
	extends CommandInternal[Assessment] with BindListener {

	self: AssignMarkersCommandState with AssessmentServiceComponent with UserGroupDaoComponent =>

	var alloctaionExtractor: MarkerAllocationExtractor = Wire[MarkerAllocationExtractor]
	var file: UploadedFile = new UploadedFile

	val markingWorflow: MarkingWorkflow = Option(assessment.markingWorkflow).getOrElse(throw new ItemNotFoundException())
	var firstMarkerMapping : JMap[String, JList[String]] = markingWorflow.firstMarkers.knownType.members.map({ marker =>
		val list : JList[String] = JArrayList()
		(marker, list)
	}).toMap.asJava

	var secondMarkerMapping : JMap[String, JList[String]] = markingWorflow.secondMarkers.knownType.members.map({ marker =>
		val list : JList[String] = JArrayList()
		(marker, list)
	}).toMap.asJava

	case class Allocation(marker:Option[User], students: Seq[User])
	@transient var sheetFirstMarkers : Seq[Allocation] = Nil
	@transient var sheetSecondMarkers : Seq[Allocation] = Nil
	@transient var sheetErrors : Seq[ParsedRow] = Nil
	@transient var unallocatedStudents : Seq[User] = Nil

	def applyInternal(): Assessment = {

		if (assessment.firstMarkers != null) {
			assessment.firstMarkers.clear()
		} else {
			assessment.firstMarkers = JArrayList()
		}

		assessment.firstMarkers.addAll(firstMarkerMapping.asScala.map { case (markerId, studentIds) =>
			val group = UserGroup.ofUsercodes
			group.includedUserIds = studentIds.asScala
			userGroupDao.saveOrUpdate(group)
			FirstMarkersMap(assessment, markerId, group)
		}.toSeq.asJava)

		if (assessment.secondMarkers != null) {
			assessment.secondMarkers.clear()
		} else {
			assessment.secondMarkers = JArrayList()
		}

		assessment.secondMarkers.addAll(secondMarkerMapping.asScala.map { case (markerId, studentIds) =>
			val group = UserGroup.ofUsercodes
			group.includedUserIds = studentIds.asScala
			userGroupDao.saveOrUpdate(group)
			SecondMarkersMap(assessment, markerId, group)
		}.toSeq.asJava)

		assessment match {
			case assignment: Assignment => assessmentService.save(assignment)
			case exam: Exam => assessmentService.save(exam)
		}

		assessment
	}

	def extractDataFromFile(file: FileAttachment, result: BindingResult): Unit = {
		val rowData = alloctaionExtractor.extractMarkersFromSpreadsheet(file.dataStream, workflow)

		def rowsToMarkerMap(rows: Seq[ParsedRow]) = {
			rows
				.filter(_.errors.isEmpty)
				.groupBy(a => a.marker)
				.map{ case (marker, row) => Allocation(marker, row.flatMap(_.student))}
				.toSeq
		}

		sheetFirstMarkers = rowData.get(FirstMarker).map(rowsToMarkerMap).getOrElse(Nil)
		sheetSecondMarkers = rowData.get(SecondMarker).map(rowsToMarkerMap).getOrElse(Nil)
		sheetErrors = rowData.values.flatten.filterNot(_.errors.isEmpty).toSeq
		unallocatedStudents = rowData.getOrElse(NoMarker, Nil).filter(_.errors.isEmpty).flatMap(_.student)
	}

	def validateUploadedFile(result: BindingResult) {
		val fileNames = file.fileNames map (_.toLowerCase)
		val invalidFiles = fileNames.filter(s => !MarkerAllocationExtractor.AcceptedFileExtensions.exists(s.endsWith))

		if (invalidFiles.size > 0) {
			if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString("")), "")
			else result.rejectValue("", "file.wrongtype", Array(invalidFiles.mkString(", ")), "")
		}
	}

	override def onBind(result: BindingResult) {
		validateUploadedFile(result)

		if (!result.hasErrors) {
			transactional() {
				file.onBind(result)
				if (!file.attached.isEmpty) {
					extractDataFromFile(file.attached.asScala.head, result)
				}
			}
		}
	}

}

trait AssignMarkersPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AssignMarkersCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Update, module)
	}

}

trait AssignMarkersDescription extends Describable[Assessment] {

	self: AssignMarkersCommandState =>

	override lazy val eventName = "AssignMarkers"

	override def describe(d: Description) {
		d.assessment(assessment)
	}

}

trait AssignMarkersCommandState {
	def module: Module
	def assessment: Assessment
	def workflow: MarkingWorkflow = assessment.markingWorkflow
}
