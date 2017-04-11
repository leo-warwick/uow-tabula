package uk.ac.warwick.tabula.pdf

import java.net.URL

import com.itextpdf.text.{Document, Image}
import org.w3c.dom.Element
import org.xhtmlrenderer.extend.{ReplacedElement, ReplacedElementFactory, UserAgentCallback}
import org.xhtmlrenderer.layout.LayoutContext
import org.xhtmlrenderer.render.BlockBox
import org.xhtmlrenderer.simple.extend.FormSubmissionListener
import uk.ac.warwick.tabula.commands.profiles.{MemberPhotoUrlGenerator, MemberPhotoUrlGeneratorComponent, PhotosWarwickMemberPhotoUrlGeneratorComponent, ServesPhotosFromExternalApplication}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.web.views.TextRendererComponent
import org.xhtmlrenderer.pdf.{ITextFSImage, ITextImageElement, ITextRenderer}
import java.io.{ByteArrayOutputStream, OutputStream}

import com.google.common.io.ByteSource
import com.itextpdf.text.pdf.{PdfCopy, PdfReader}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.FileDaoComponent
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.FileAttachment

import scala.util.Try

trait PdfGenerator {
	def renderTemplate(templateId: String, model: Any, out: OutputStream)
}

trait PDFGeneratorComponent {
	def pdfGenerator: PdfGenerator
}

trait FreemarkerXHTMLPDFGeneratorComponent extends PDFGeneratorComponent {
	self: TextRendererComponent with MemberPhotoUrlGeneratorComponent =>

	var topLevelUrl: String = Wire.property("${toplevel.url}")

	def pdfGenerator: PdfGenerator = new PdfGeneratorImpl()
	val parentUrlGenerator: MemberPhotoUrlGenerator = photoUrlGenerator

	class PdfGeneratorImpl extends PdfGenerator {

		def renderTemplate(templateId: String, model: Any, out:OutputStream): Unit = {
			val xthml = textRenderer.renderTemplate(templateId, model)
			val renderer = new ITextRenderer
			val elementFactory = new ProfileImageReplacedElementFactory(renderer.getSharedContext.getReplacedElementFactory)
				with MemberPhotoUrlGeneratorComponent
			renderer.getSharedContext.setReplacedElementFactory(elementFactory)
			renderer.setDocumentFromString(xthml.replace("&#8194;", " "), topLevelUrl)
			renderer.layout()
			renderer.createPDF(out)
		}
	}

}

trait PdfGeneratorWithFileStorage extends PdfGenerator {
	def renderTemplateAndStore(templateId: String, fileName: String, model: Any): FileAttachment
}

trait PDFGeneratorWithFileStorageComponent extends PDFGeneratorComponent {
	override def pdfGenerator: PdfGeneratorWithFileStorage
}

trait FreemarkerXHTMLPDFGeneratorWithFileStorageComponent extends FreemarkerXHTMLPDFGeneratorComponent with PDFGeneratorWithFileStorageComponent {

	self: FileDaoComponent with TextRendererComponent with MemberPhotoUrlGeneratorComponent =>

	override def pdfGenerator: PdfGeneratorWithFileStorage = new PdfGeneratorWithFileStorageImpl()

	class PdfGeneratorWithFileStorageImpl extends PdfGeneratorImpl with PdfGeneratorWithFileStorage {
		override def renderTemplateAndStore(templateId: String, fileName: String = "feedback.pdf", model: Any): FileAttachment = {
			val tempOutputStream = new ByteArrayOutputStream()
			renderTemplate(templateId, model, tempOutputStream)

			val bytes = tempOutputStream.toByteArray

			// Create file
			val pdfFileAttachment = new FileAttachment
			pdfFileAttachment.name = fileName
			pdfFileAttachment.uploadedData = ByteSource.wrap(bytes)
			transactional() { fileDao.saveTemporary(pdfFileAttachment) }
			pdfFileAttachment
		}
	}

}

trait CombinesPdfs {

	self: FileDaoComponent =>

	def combinePdfs(pdfs: Seq[FileAttachment], fileName: String): FileAttachment = {
		PdfReader.unethicalreading = true
		val output = new ByteArrayOutputStream
		val document = new Document()
		val copy = new PdfCopy(document, output)
		document.open()
		pdfs.foreach(attachment => {
			val reader = new PdfReader(attachment.dataStream)

			(1 to reader.getNumberOfPages).foreach(page => {
				copy.addPage(copy.getImportedPage(reader, page))
			})
			copy.freeReader(reader)
			reader.close()
		})
		document.close()
		val pdf = new FileAttachment
		pdf.name = fileName
		pdf.uploadedData = ByteSource.wrap(output.toByteArray)
		transactional() { fileDao.saveTemporary(pdf) }
	}
}

class ProfileImageReplacedElementFactory(delegate: ReplacedElementFactory) extends ReplacedElementFactory
	with ServesPhotosFromExternalApplication with PhotosWarwickMemberPhotoUrlGeneratorComponent with AutowiringProfileServiceComponent {

	this: MemberPhotoUrlGeneratorComponent =>

	size = THUMBNAIL_SIZE // TODO consider allowing different sizes

	override def createReplacedElement(c: LayoutContext, box: BlockBox, uac: UserAgentCallback, cssWidth: Int, cssHeight: Int): ReplacedElement = {
		Option(box.getElement).map { element =>
			Some(element)
				.filter { el => "img".equals(el.getNodeName) && el.hasAttribute("data-universityid") }
				.flatMap { el => profileService.getMemberByUniversityId(el.getAttribute("data-universityid"), disableFilter=true) }
				.flatMap { m =>
					val url = new URL(photoUrl(Some(m)))
					val image = Try(Image.getInstance(url)).getOrElse(Image.getInstance(DEFAULT_IMAGE))
					Option(new ITextFSImage(image))
				}
				.map { fsImage =>
					if ((cssWidth != -1) || (cssHeight != -1)) fsImage.scale(cssWidth, cssHeight)
					new ITextImageElement(fsImage)
				}
				.getOrElse {
					delegate.createReplacedElement(c, box, uac, cssWidth, cssHeight)
				}
		}.orNull
	}

	override def remove(e: Element): Unit = delegate.remove(e)
	override def setFormSubmissionListener(listener: FormSubmissionListener): Unit = delegate.setFormSubmissionListener(listener)
	override def reset(): Unit = delegate.reset()
}