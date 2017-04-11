package uk.ac.warwick.tabula.commands.groups.admin

import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports.{JArrayList, _}
import uk.ac.warwick.tabula.commands.groups.admin.DownloadRegistersAsPdfHelper.{DisplayCheck, DisplayName, SortOrder}
import uk.ac.warwick.tabula.commands.profiles.{MemberPhotoUrlGeneratorComponent, PhotosWarwickMemberPhotoUrlGeneratorComponent}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventOccurrence, SmallGroupSet}
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.pdf.{CombinesPdfs, FreemarkerXHTMLPDFGeneratorWithFileStorageComponent, PDFGeneratorWithFileStorageComponent}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, TextRendererComponent}
import scala.collection.JavaConverters._

trait AutowiringDownloadRegistersAsPdfCommandHelper
	extends AutowiringTermServiceComponent
		with AutowiringSmallGroupServiceComponent
		with AutowiringFileDaoComponent
		with FreemarkerXHTMLPDFGeneratorWithFileStorageComponent
		with CombinesPdfs
		with AutowiredTextRendererComponent
		with PhotosWarwickMemberPhotoUrlGeneratorComponent
		with AutowiringUserLookupComponent
		with AutowiringProfileServiceComponent
		with AutowiringUserSettingsServiceComponent

object DownloadRegistersAsPdfHelper {

	object DisplayName {
		val Name = "name"
		val Id = "id"
		val Both = "both"
	}

	object DisplayCheck {
		val Checkbox = "checkbox"
		val SignatureLine = "line"
	}

	object SortOrder {
		val Module = "module"
		val Tutor = "tutor"
	}

	final val registerTemplate = "/WEB-INF/freemarker/groups/attendance/register-pdf.ftl"

	type Dependencies = DownloadRegistersAsPdfCommandRequest with DownloadRegistersAsPdfCommandState
		with GetsOccurrences with PDFGeneratorWithFileStorageComponent with CombinesPdfs
		with TextRendererComponent with MemberPhotoUrlGeneratorComponent with FileDaoComponent
		with UserLookupComponent with ProfileServiceComponent with UserSettingsServiceComponent

}

trait DownloadRegistersAsPdfCommandRequest {
	var startDate: DateTime = DateTime.now().withTimeAtStartOfDay()
	var endDate: DateTime = DateTime.now().withTimeAtStartOfDay().plusWeeks(1)
	var smallGroupSets: JList[SmallGroupSet] = JArrayList()
	def smallGroupSetIds: Seq[String] = smallGroupSets.asScala.map(_.id)
	var showPhotos = true
	var displayName = DisplayName.Name
	var displayCheck = DisplayCheck.Checkbox
	var sortOrder = SortOrder.Module
	var studentSortFields: JList[String] = JArrayList()
	var studentSortOrders: JList[String] = JArrayList()
}

trait GetsOccurrences {
	def getOccurrences: Seq[SmallGroupEventOccurrence]
}