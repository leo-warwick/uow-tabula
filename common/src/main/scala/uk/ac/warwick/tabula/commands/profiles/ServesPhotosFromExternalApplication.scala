package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.util.web.UriBuilder

trait ServesPhotosFromExternalApplication {
	self: MemberPhotoUrlGeneratorComponent =>

	val THUMBNAIL_SIZE = "thumbnail"
	val TINYTHUMBNAIL_SIZE = "tinythumbnail"
	val ACTUAL_SIZE = "actual"
	val DEFAULT_IMAGE = "/static/images/no-photo.jpg"

	var size: String = ACTUAL_SIZE

	def photoUrl(member: Option[Member]): String = {
		member.map { m =>
			val baseUri = photoUrlGenerator.generateUrl(m)

			size match {
				case THUMBNAIL_SIZE => new UriBuilder(baseUri).addQueryParameter("s", "170").toString
				case TINYTHUMBNAIL_SIZE => new UriBuilder(baseUri).addQueryParameter("s", "40").toString
				case _ => baseUri.toString
			}
		}.getOrElse(DEFAULT_IMAGE) // TODO no resizing
	}

}
