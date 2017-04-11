package uk.ac.warwick.tabula.commands.exams.grids

import org.apache.poi.util.Units
import org.apache.poi.xwpf.usermodel.{Document, XWPFDocument}
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.{STRelFromH, STRelFromV}
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr

trait AddConfidentialWatermarkToDocument {

	def addWatermark(doc: XWPFDocument): Unit = {
		// Create header
		val header = doc.createHeaderFooterPolicy().createHeader(STHdrFtr.DEFAULT)
		val r = header.createParagraph().createRun()
		// Add the image
		r.addPicture(
			getClass.getResourceAsStream("/confidential-a4-watermark.png"),
			Document.PICTURE_TYPE_PNG,
			"confidential-a4-watermark.png",
			Units.toEMU(Units.pixelToPoints(595)),
			Units.toEMU(Units.pixelToPoints(841))
		)
		val drawing = r.getCTR.getDrawingArray(0)
		// Swap the inline image to an anchored image so it can float behind the text
		val inline = drawing.getInlineArray(0)
		val anchor = drawing.addNewAnchor()
		// Most of these don't do anything but are required elements (LibreOffice is fine without but Word blows up)
		anchor.setAllowOverlap(true)
		anchor.setLayoutInCell(false)
		anchor.setLocked(false)
		// This one matters
		anchor.setBehindDoc(true)
		anchor.setRelativeHeight(251658240)
		anchor.setSimplePos2(false)
		anchor.setDistR(114300)
		anchor.setDistL(114300)
		anchor.setDistB(0)
		anchor.setDistT(0)
		val simplePos = anchor.addNewSimplePos()
		simplePos.setX(0)
		simplePos.setY(0)
		val positionH = anchor.addNewPositionH()
		positionH.setPosOffset(0)
		positionH.setRelativeFrom(STRelFromH.COLUMN)
		val positionV = anchor.addNewPositionV()
		positionV.setPosOffset(447040)
		positionV.setRelativeFrom(STRelFromV.PAGE)
		anchor.setExtent(inline.getExtent)
		anchor.addNewWrapNone()
		anchor.setDocPr(inline.getDocPr)
		anchor.setGraphic(inline.getGraphic)
		drawing.removeInline(0)
	}

}
