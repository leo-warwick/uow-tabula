package uk.ac.warwick.tabula

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, ISODateTimeFormat}

/**
 * Date format strings and formatter objects that can be used
 * throughout the application. Typically we'll only have a few
 * different date formats and use them consistently, for consistency.
 */
object DateFormats {
	/** String format as expected by Javascript date time picker */
	final val DateTimePickerPattern = "dd-MMM-yyyy HH:mm:ss"
	final val DatePickerPattern = "dd-MMM-yyyy"
	final val TimePickerPattern = "HH:mm:ss"
	final val NotificationDateOnlyPattern = "d MMMM yyyy"
	final val NotificationDateTimePattern: String = NotificationDateOnlyPattern + " 'at' HH:mm:ss"
	final val CSVDatePattern = "dd/MM/yyyy"       // we need the pattern for SpreadsheetHelpers
	final val CSVDateTimePattern = "dd/MM/yyyy HH:mm"

	/**
	 * ISO 8601 date format. Has date, hours, minutes, timezone, no millis.
	 * We don't really need seconds either but there's no simple method to
	 * make a formatter with no seconds but still having a timezone.
	 */
	final val IsoDateTime: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis
	final val IsoDate: DateTimeFormatter = ISODateTimeFormat.date

	/** Date-time format used in Formsbuilder-style CSV export */
	final val CSVDateTime: DateTimeFormatter = DateTimeFormat.forPattern(CSVDateTimePattern)

	/** Date format used in spreadsheets */
	final val CSVDate: DateTimeFormatter = DateTimeFormat.forPattern(CSVDatePattern)

	/** Date format used in emails */
	final val NotificationDateOnly: DateTimeFormatter = DateTimeFormat.forPattern(NotificationDateOnlyPattern)
	final val NotificationDateTime: DateTimeFormatter = DateTimeFormat.forPattern(NotificationDateTimePattern)


	final val DateTimePickerFormatter: DateTimeFormatter = DateTimeFormat.forPattern(DateTimePickerPattern)
	final val DatePickerFormatter: DateTimeFormatter = DateTimeFormat.forPattern(DatePickerPattern)
	final val TimePickerFormatter: DateTimeFormatter = DateTimeFormat.forPattern(TimePickerPattern)

}

