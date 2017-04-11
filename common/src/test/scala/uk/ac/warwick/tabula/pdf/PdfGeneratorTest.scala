package uk.ac.warwick.tabula.pdf

import uk.ac.warwick.tabula.TestBase
import java.io.{ByteArrayOutputStream, File, FileOutputStream}
import uk.ac.warwick.tabula.commands.profiles.{PhotosWarwickConfig, PhotosWarwickConfigComponent, PhotosWarwickMemberPhotoUrlGenerator, MemberPhotoUrlGeneratorComponent}
import uk.ac.warwick.tabula.web.views.{TextRenderer, TextRendererComponent}

class PdfGeneratorTest extends TestBase{


	trait MockMemberPhotoUrlGeneratorComponent extends MemberPhotoUrlGeneratorComponent {
		val photoUrlGenerator = new PhotosWarwickMemberPhotoUrlGenerator with PhotosWarwickConfigComponent {
			def photosWarwickConfiguration = PhotosWarwickConfig("photos.warwick.ac.uk", "tabula", "somekey")
		}
	}

	val pdfGenerator: PdfGenerator = new FreemarkerXHTMLPDFGeneratorComponent with MockMemberPhotoUrlGeneratorComponent with TextRendererComponent {
		def textRenderer:TextRenderer = new TextRenderer {
			def renderTemplate(templateId: String, model: Any): String = {
				templateId match {
					case "minimal"=>minimalXhtml
				}
			}
		}
	}.pdfGenerator

	@Test
	def renderXHTML(){
		val baos = new ByteArrayOutputStream()
		pdfGenerator.renderTemplate("minimal",Map(),baos)
		val of = new FileOutputStream(new File("/tmp/test.pdf"))
		val pdfBytes =baos.toByteArray
		of.write(pdfBytes)
		of.close()
		val pdf = new String(pdfBytes)
		pdf should not be null
		pdf should include("%PDF-1.4")
	}

	val minimalXhtml: String =
		"""<?xml version="1.0" encoding="UTF-8"?>
			<!DOCTYPE html
			PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
			"DTD/xhtml1-transitional.dtd">
			<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
			<head>
			<style type="text/css">
				body {font-family: "Helvetica Neue", Helvetica, Arial, sans-serif ;
							color: green
							}
      </style>
			<title>Test document</title>
			</head>
			<body>
			<p>Hello world</p>
			</body>
			</html>
		""".stripMargin
}

