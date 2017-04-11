package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.{ItemNotFoundException, Mockito, TestBase}

import scala.language.reflectiveCalls

class ViewProfilePhotoCommandTest extends TestBase with Mockito {

	val testConfig = PhotosWarwickConfig("photos.warwick.ac.uk", "tabula", "somekey")

	@Test(expected=classOf[ItemNotFoundException])
	def memberDoesNotExist() {
		ViewProfilePhotoCommand(null: Member)
	}

	@Test
	def memberPhoto() {
		val member = new StudentMember()
		member.universityId = "1170836"
		val command = new ViewProfilePhotoCommand(member) with MemberPhotoUrlGeneratorComponent {
			val photoUrlGenerator = new PhotosWarwickMemberPhotoUrlGenerator with PhotosWarwickConfigComponent {
				def photosWarwickConfiguration: PhotosWarwickConfig = testConfig
			}
		}
		val mav = command.applyInternal()
		mav.viewName should be ("redirect:https://photos.warwick.ac.uk/tabula/photo/71e36c13ab75fc9f422b1b404245b1a3/1170836")
	}

}
