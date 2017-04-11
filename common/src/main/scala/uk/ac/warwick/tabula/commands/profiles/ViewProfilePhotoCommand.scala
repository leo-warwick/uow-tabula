package uk.ac.warwick.tabula.commands.profiles

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.joda.time.Days
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.{Member, StudentRelationship}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.fileserver.{CachePolicy, RenderableFile}
import uk.ac.warwick.tabula.web.Mav

object ViewProfilePhotoCommand {
	def apply(member: Member) = new ViewProfilePhotoCommand(member) with PhotosWarwickMemberPhotoUrlGeneratorComponent
}

abstract class ViewProfilePhotoCommand(val member: Member)
	extends Command[Mav] with ReadOnly with Unaudited with ServesPhotosFromExternalApplication {

	this: MemberPhotoUrlGeneratorComponent =>

	PermissionCheck(Permissions.Profiles.Read.Photo, mandatory(member))

	override def applyInternal(): Mav = {
		Mav(s"redirect:${photoUrl(Option(member))}")
	}

	override def describe(d: Description): Unit = d.member(member)
}

class ViewStudentRelationshipPhotoCommand(val member: Member, val relationship: StudentRelationship)
	extends Command[Mav] with ReadOnly with Unaudited with ServesPhotosFromExternalApplication with PhotosWarwickMemberPhotoUrlGeneratorComponent {

	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(relationship.relationshipType), member)

	override def applyInternal(): Mav = {
		Mav(s"redirect:${photoUrl(relationship.agentMember)}")
	}

	override def describe(d: Description): Unit = d.member(member).property("relationship" -> relationship)

}

object DefaultPhoto extends RenderableFile {
	private def read() = {
		val is = getClass.getResourceAsStream("/no-photo.jpg")
		val os = new ByteArrayOutputStream

		FileCopyUtils.copy(is, os)
		os.toByteArray
	}

	// TODO is keeping this in memory the right thing to do? It's only 3kb
	private val NoPhoto = read()

	override def inputStream = new ByteArrayInputStream(NoPhoto)
	override def filename = "no-photo.jpg"
	override def contentType = "image/jpg"
	override def contentLength = Some(NoPhoto.length)

	override def cachePolicy = CachePolicy(expires = Some(Days.ONE))
}