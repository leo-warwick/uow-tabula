package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.services.{FileAttachmentService, MaintenanceModeService}

import scala.collection.JavaConverters._
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.NoBind
import org.springframework.validation.BindingResult

/**
 * Encapsulates initially-uploaded MultipartFiles with a reference to
 * FileAttachments to hold on to the ID of a file on subsequent submissions,
 * so that you don't have to re-upload a file again.
 *
 * When an HTML5 multiple file upload control is used, Spring can bind this
 * to an array or collection of MultipartFile objects. This is why UploadedFile
 * has a collection of MultipartFiles, rather than using a collection of UploadedFile
 * objects.
 *
 * Works well with the filewidget macro in forms.ftl. Remember to set the
 * multipart/form-data encoding type on your form.
 */
class UploadedFile extends BindListener {
	@transient var fileAttachmentService = Wire[FileAttachmentService]
	@transient var maintenanceModeService = Wire[MaintenanceModeService]

	@NoBind var disallowedFilenames = commaSeparated(Wire[String]("${uploads.disallowedFilenames}"))
	@NoBind var disallowedPrefixes = commaSeparated(Wire[String]("${uploads.disallowedPrefixes}"))

	// files bound from an upload request, prior to being persisted by `onBind`.
	var upload: JList[MultipartFile] = JArrayList()

	// files that have been persisted - can be represented in forms by ID
	var attached: JList[FileAttachment] = JArrayList()

	def uploadedFileNames: Seq[String] = upload.asScala.map(_.getOriginalFilename).filterNot(_ == "")
	def attachedFileNames: Seq[String] = attached.asScala.map(_.getName)
	def fileNames = uploadedFileNames ++ attachedFileNames

	def isMissing = !isExists
	def isExists = hasUploads || hasAttachments

	def size: Int =
		if (hasAttachments) attached.size
		else if (hasUploads) permittedUploads.size
		else 0

	def attachedOrEmpty: JList[FileAttachment] = Option(attached).getOrElse(JArrayList())
	def uploadOrEmpty: JList[MultipartFile] = permittedUploads

	def hasAttachments = attached != null && !attached.isEmpty
	def hasUploads = !permittedUploads.isEmpty

	/** Uploads excluding those that are empty or have bad names. */
	def permittedUploads: JList[MultipartFile] = {
		upload.asScala.filterNot { s =>
			s.isEmpty ||
			(disallowedFilenames contains s.getOriginalFilename) ||
			(disallowedPrefixes exists s.getOriginalFilename.startsWith)
		}.asJava
	}

	def isUploaded = hasUploads

	def individualFileSizes: Seq[(String, Long)] =
		upload.asScala.map(u => (u.getOriginalFilename, u.getSize)) ++
			attached.asScala.map(a => (a.getName, a.length.getOrElse(0L)))

	/**
	 * Performs persistence of uploads such as converting MultipartFiles
	 * into FileAttachments saves to the database. When first saved, these
	 * FileAttachments are marked as "temporary" until persisted by whatever
	 * command needs them. This method will throw an exception
	 */
	override def onBind(result: BindingResult) {
		if (maintenanceModeService.enabled) {
			throw maintenanceModeService.exception(None)
		}

		val bindResult = for (item <- attached.asScala) yield {
			if (item != null && !item.temporary) {
				result.reject("binding.reSubmission")
				false
			} else true
		}

		// Early exit if we've failed binding
		if (!bindResult.contains(false)) {
			if (hasUploads) {
				// convert MultipartFiles into FileAttachments
				val newAttachments = fileAttachmentService.persistTemporary(permittedUploads.asScala)

				// remove converted files from upload to avoid duplicates
				upload.clear()
				attached.addAll(newAttachments.asJava)
			} else {
				// sometimes we manually add FileAttachments with uploaded data to persist
				for (item <- attached.asScala if item.uploadedData != null)
					fileAttachmentService.saveTemporary(item)
			}
		}
	}

	private def commaSeparated(csv: String) =
		if (csv == null) Nil
		else csv.split(",").toList
}
