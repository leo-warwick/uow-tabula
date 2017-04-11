package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.model.FileAttachment

trait FileAttachmentToJsonConverter {

	def jsonFileAttachmentObject(attachment: FileAttachment): Map[String, Any] = {
		Map(
			"id" -> attachment.id,
			"name" -> attachment.name,
			"temporary" -> attachment.temporary,
			"dateUploaded" -> DateFormats.IsoDateTime.print(attachment.dateUploaded),
			"hash" -> attachment.hash,
			"uploadedBy" -> attachment.uploadedBy
		)
	}

}
