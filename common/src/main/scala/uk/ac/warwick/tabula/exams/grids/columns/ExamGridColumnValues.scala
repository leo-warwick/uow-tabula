package uk.ac.warwick.tabula.exams.grids.columns

import org.apache.poi.xssf.usermodel.{XSSFCell, XSSFCellStyle}
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridExporter
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.JavaImports._

sealed abstract class ExamGridColumnValueType(val label: String, val description: String)

object ExamGridColumnValueType {
	case object Overall extends ExamGridColumnValueType("O", "Overall")
	case object Assignment extends ExamGridColumnValueType("A", "Assignment")
	case object Exam extends ExamGridColumnValueType("E", "Exam")

	def toMap(overall: ExamGridColumnValue, assignments: Seq[ExamGridColumnValue], exams: Seq[ExamGridColumnValue]): Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]] =
		Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]](
			Overall -> Seq(overall),
			Assignment -> assignments,
			Exam -> exams
		)

	def toMap(overall: ExamGridColumnValue): Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]] =
		Map[ExamGridColumnValueType, Seq[ExamGridColumnValue]](
			Overall -> Seq(overall),
			Assignment -> Seq(ExamGridColumnValueString("")),
			Exam -> Seq(ExamGridColumnValueString(""))
		)
}

sealed trait ExamGridColumnValue {
	protected def getValueStringForRender: String
	protected def applyCellStyle(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit
	val isActual: Boolean
	def toHTML: String
	def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit
}

object ExamGridColumnValue {
	def merge(values: Seq[ExamGridColumnValue]): ExamGridColumnValue = {
		values match {
			case _ if values.isEmpty => ExamGridColumnValueString("")
			case _ if values.size == 1 => values.head
			case _ =>
				// First see if they're ALL actual marks or not
				values.tail.forall(_.isActual == values.head.isActual) match {
					case false =>
						// If only some are actual we can't apply a common style, so just return a plain merged string
						ExamGridColumnValueString(values.map(_.getValueStringForRender).mkString(","))
					case true =>
						// If they're ALL actual or not, check other cell styles
						values.head match {
							case _: ExamGridColumnValueFailed if values.tail.forall(_.isInstanceOf[ExamGridColumnValueFailed]) =>
								ExamGridColumnValueFailedString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
							case _: ExamGridColumnValueOvercat if values.tail.forall(_.isInstanceOf[ExamGridColumnValueOvercat]) =>
								ExamGridColumnValueOvercatString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
							case _: ExamGridColumnValueOverride if values.tail.forall(_.isInstanceOf[ExamGridColumnValueOverride]) =>
								ExamGridColumnValueOverrideString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
							case _: ExamGridColumnValueMissing if values.tail.forall(_.isInstanceOf[ExamGridColumnValueMissing]) =>
								ExamGridColumnValueMissing()
							case _ =>
								ExamGridColumnValueString(values.map(_.getValueStringForRender).mkString(","), isActual = values.head.isActual)
						}
				}
		}
	}
}

object ExamGridColumnValueDecimal {
	def apply(value: BigDecimal, isActual: Boolean = false) = new ExamGridColumnValueDecimal(value, isActual)
}
class ExamGridColumnValueDecimal(value: BigDecimal, val isActual: Boolean = false) extends ExamGridColumnValue {
	protected final def getValueForRender: JBigDecimal = value.underlying.stripTrailingZeros()
	override protected final def getValueStringForRender: String = getValueForRender.toPlainString
	override protected def applyCellStyle(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.ActualMark))
		}
	}
	override def toHTML: String =
		if (isActual) "<span class=\"exam-grid-actual-mark\">%s</span>".format(getValueStringForRender)
		else getValueStringForRender
	override final def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(getValueForRender.doubleValue)
		applyCellStyle(cell, cellStyleMap)
	}
}

object ExamGridColumnValueString {
	def apply(value: String, isActual: Boolean = false) = new ExamGridColumnValueString(value, isActual)
}
class ExamGridColumnValueString(value: String, val isActual: Boolean = false) extends ExamGridColumnValue {
	override protected final def getValueStringForRender: String = value
	override protected def applyCellStyle(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.ActualMark))
		}
	}
	override def toHTML: String = value
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellValue(value)
		applyCellStyle(cell, cellStyleMap)
	}
}


trait ExamGridColumnValueFailed {

	self: ExamGridColumnValue =>

	override def toHTML: String = "<span class=\"exam-grid-fail %s\">%s</span>".format(
		if (isActual) "exam-grid-actual-mark" else "",
		getValueStringForRender
	)

	override protected def applyCellStyle(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.FailAndActualMark))
		} else {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Fail))
		}
	}
}

case class ExamGridColumnValueFailedDecimal(value: BigDecimal, override val isActual: Boolean = false)
	extends ExamGridColumnValueDecimal(value) with ExamGridColumnValueFailed

case class ExamGridColumnValueFailedString(value: String, override val isActual: Boolean = false)
	extends ExamGridColumnValueString(value) with ExamGridColumnValueFailed


trait ExamGridColumnValueOvercat {

	self: ExamGridColumnValue =>

	override def toHTML: String = "<span class=\"exam-grid-overcat\">%s</span>".format(getValueStringForRender)
	override protected def applyCellStyle(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		if (isActual) {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.OvercatAndActualMark))
		} else {
			cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Overcat))
		}
	}
}

case class ExamGridColumnValueOvercatDecimal(value: BigDecimal, override val isActual: Boolean = false)
	extends ExamGridColumnValueDecimal(value) with ExamGridColumnValueOvercat

case class ExamGridColumnValueOvercatString(value: String, override val isActual: Boolean = false)
	extends ExamGridColumnValueString(value) with ExamGridColumnValueOvercat


trait ExamGridColumnValueOverride {

	self: ExamGridColumnValue =>

	override def toHTML: String = "<span class=\"exam-grid-override\">%s</span>".format(getValueStringForRender)
	override protected def applyCellStyle(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.Overridden))
	}
}

case class ExamGridColumnValueOverrideDecimal(value: BigDecimal, override val isActual: Boolean = false)
	extends ExamGridColumnValueDecimal(value) with ExamGridColumnValueOverride

case class ExamGridColumnValueOverrideString(value: String, override val isActual: Boolean = false)
	extends ExamGridColumnValueString(value) with ExamGridColumnValueOverride


case class ExamGridColumnValueMissing(message: String = "") extends ExamGridColumnValueString("X", isActual = true) {
	override def toHTML: String = "<span class=\"exam-grid-actual-mark %s\" %s>X</span>".format(
		if (message.hasText) "use-tooltip" else "",
		if (message.hasText) "title=\"%s\" data-container=\"body\"".format(message) else ""
	)
	override protected def applyCellStyle(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {
		cell.setCellStyle(cellStyleMap(GenerateExamGridExporter.ActualMark))
	}
}

case class ExamGridColumnValueStringHtmlOnly(value: String) extends ExamGridColumnValueString(value) {
	override def toHTML: String = super.toHTML
	override def populateCell(cell: XSSFCell, cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]): Unit = {}
}
