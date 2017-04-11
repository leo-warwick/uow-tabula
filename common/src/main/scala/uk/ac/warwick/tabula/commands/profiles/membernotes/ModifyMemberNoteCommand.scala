package uk.ac.warwick.tabula.commands.profiles.membernotes

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{CommandInternal, UploadedFile}
import uk.ac.warwick.tabula.data.HibernateHelpers
import uk.ac.warwick.tabula.data.model.{AbstractMemberNote, ExtenuatingCircumstances, FileAttachment, MemberNote}
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MemberNoteServiceComponent}
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class ModifyMemberNoteCommandInternal extends CommandInternal[AbstractMemberNote] {

	self: ModifyMemberNoteCommandRequest with ModifyAbstractMemberNoteCommandState
		with FileAttachmentServiceComponent with MemberNoteServiceComponent =>

	override def applyInternal(): AbstractMemberNote = {
		copyTo(abstractMemberNote)

		if (abstractMemberNote.attachments != null) {
			val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
			val filesToRemove: mutable.Buffer[FileAttachment] = abstractMemberNote.attachments.asScala -- filesToKeep
			abstractMemberNote.attachments = JArrayList[FileAttachment](filesToKeep)
			fileAttachmentService.deleteAttachments(filesToRemove)
		}

		if (!file.attached.isEmpty) {
			for (attachment <- file.attached.asScala) {
				abstractMemberNote.addAttachment(attachment)
			}
		}

		HibernateHelpers.initialiseAndUnproxy(abstractMemberNote) match {
			case memberNote: MemberNote => memberNoteService.saveOrUpdate(memberNote)
			case circumstances: ExtenuatingCircumstances => memberNoteService.saveOrUpdate(circumstances)
		}

		abstractMemberNote
	}

}

trait ModifyMemberNoteCommandBindListener extends BindListener {

	self: ModifyMemberNoteCommandRequest =>

	override def onBind(result: BindingResult) {
		file.onBind(result)
	}
}

trait ModifyAbstractMemberNoteCommandState {
	def abstractMemberNote: AbstractMemberNote
	val attachmentTypes: Seq[String] = Seq[String]()
}

trait ModifyMemberNoteCommandState extends ModifyAbstractMemberNoteCommandState {
	def memberNote: MemberNote
	override def abstractMemberNote: AbstractMemberNote = memberNote
}

trait ModifyExtenuatingCircumstancesCommandState extends ModifyAbstractMemberNoteCommandState {
	def circumstances: ExtenuatingCircumstances
	override def abstractMemberNote: AbstractMemberNote = circumstances
}

trait ModifyMemberNoteCommandRequest {
	var title: String = _
	var note: String = _
	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = JArrayList()

	def copyTo(memberNote: AbstractMemberNote) {
		memberNote.note = note
		memberNote.title = title
		memberNote.lastUpdatedDate = DateTime.now
	}
}

trait ModifyExtenuatingCircumstancesCommandRequest extends ModifyMemberNoteCommandRequest {
	var startDate: LocalDate = _
	var endDate: LocalDate = _

	override def copyTo(memberNote: AbstractMemberNote): Unit = {
		memberNote match {
			case circumstance: ExtenuatingCircumstances =>
				circumstance.startDate = startDate
				circumstance.endDate = endDate
			case _ =>
		}

		super.copyTo(memberNote)
	}
}
