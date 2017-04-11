package uk.ac.warwick.tabula.commands.profiles

import org.apache.commons.codec.digest.DigestUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.util.web.Uri

trait MemberPhotoUrlGenerator {

	def generateUrl(member: Member): Uri

}

class PhotosWarwickMemberPhotoUrlGenerator extends MemberPhotoUrlGenerator {
	self: PhotosWarwickConfigComponent =>

	override def generateUrl(member: Member): Uri = {
		val universityId = member.universityId

		val photoKey = DigestUtils.md5Hex(photosWarwickConfiguration.preSharedKey + universityId)

		Uri.parse(s"https://${photosWarwickConfiguration.host}/${photosWarwickConfiguration.applicationId}/photo/$photoKey/$universityId")
	}
}

case class PhotosWarwickConfig(host: String, applicationId: String, preSharedKey: String)

trait PhotosWarwickConfigComponent {
	def photosWarwickConfiguration: PhotosWarwickConfig
}

trait AutowiringPhotosWarwickConfigComponent extends PhotosWarwickConfigComponent {
	val photosWarwickConfiguration = PhotosWarwickConfig(
		host = Wire.property("${photos.host}"),
		applicationId = Wire.property("${photos.applicationId}"),
		preSharedKey = Wire.property("${photos.preSharedKey}")
	)
}

trait MemberPhotoUrlGeneratorComponent {
	def photoUrlGenerator: MemberPhotoUrlGenerator
}

trait PhotosWarwickMemberPhotoUrlGeneratorComponent extends MemberPhotoUrlGeneratorComponent {
	val photoUrlGenerator = new PhotosWarwickMemberPhotoUrlGenerator with AutowiringPhotosWarwickConfigComponent
}