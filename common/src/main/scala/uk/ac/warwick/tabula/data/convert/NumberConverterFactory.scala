package uk.ac.warwick.tabula.data.convert

import org.springframework.core.convert.converter._
import org.springframework.util.NumberUtils
import java.beans.PropertyEditorSupport

final class NumberConverterFactory extends ConverterFactory[String, Number] {
	def getConverter[A <: Number](targetType: Class[A]) = new NumberConverterFactory.StringToNumber[A](targetType)
}

object NumberConverterFactory {
	class StringToNumber[A <: Number](targetType: Class[A]) extends Converter[String, A] {
		def convert(source: String): A = {
			val cleaned = source.replace(",","")
			if (cleaned.length() == 0) null.asInstanceOf[A] //yep.
			else NumberUtils.parseNumber(cleaned, targetType)
		}
	}
}
