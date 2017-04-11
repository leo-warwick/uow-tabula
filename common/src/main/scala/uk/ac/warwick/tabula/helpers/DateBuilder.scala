package uk.ac.warwick.tabula.helpers

import scala.collection.JavaConversions._
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import freemarker.template.TemplateMethodModelEx
import freemarker.template.TemplateModel
import freemarker.template.utility.DeepUnwrap
import uk.ac.warwick.tabula.JavaImports._

object DateBuilder {

	private val dayAndDateFormat = DateTimeFormat.forPattern("EEE d")
	private val monthAndYearFormat = DateTimeFormat.forPattern(" MMMM yyyy")
	private val shortMonthAndYearFormat = DateTimeFormat.forPattern(" MMM yyyy")
	private val formatterMap = new DateTimeFormatterCache

	private val _relativeWords = Map(
		'yesterday -> "Yesterday",
		'today -> "Today",
		'tomorrow -> "Tomorrow")
	private val relativeWords = Map(
		true -> _relativeWords,
		false -> _relativeWords.mapValues(_.toLowerCase))

	/* called with just a DateTime - use the default arguments */
	def format(date: ReadableDateTime): String =
		format(date=date,
			includeSeconds=false,
			includeAt=true,
			includeTimezone=false,
			capitalise=true,
			relative=true,
			split=false,
			shortMonth=false,
			includeTime=true)

	/* everything is specified, including whether minutes should be included */
	def format(date: ReadableDateTime,
			includeSeconds: Boolean,
			includeAt: Boolean,
			includeTimezone: Boolean,
			capitalise: Boolean,
			relative: Boolean,
			split: Boolean,
			shortMonth: Boolean,
			includeTime: Boolean): String = {
		if (includeTime) {
			val pattern = new StringBuilder
			pattern.append("HH:mm")
			if (includeSeconds) pattern.append(":ss")
			if (includeTimezone) pattern.append(" (z)")
			//if (includeAt) pattern.append(" 'at'")
			(formatterMap.retrieve(pattern.toString()) print date).trim() + (if (split) "<br />" else "&#8194;") + datePart(date, capitalise, relative, shortMonth)
		} else {
			datePart(date, capitalise, relative, shortMonth)
		}
	}

	def ordinal(day: Int): String = day % 10 match {
		case _ if (day >= 10 && day <= 20) => "th"
		case 1 => "st"
		case 2 => "nd"
		case 3 => "rd"
		case _ => "th"
	}

	def datePart(date: ReadableDateTime, capitalise: Boolean, relative: Boolean, shortMonth: Boolean): String = {
		val today = LocalDate.now.toDateTimeAtStartOfDay
		val thatDay = new LocalDate(date.getMillis, date.getChronology).toDateTimeAtStartOfDay

		lazy val absoluteDate = (dayAndDateFormat print date) +
			"<sup>" + ordinal(date.getDayOfMonth) + "</sup>" +
			(if (shortMonth) (shortMonthAndYearFormat print date)
			else (monthAndYearFormat print date))

		if (!relative) absoluteDate
		else if (today isEqual thatDay) relativeWords(capitalise)('today)
		else if (today.minusDays(1) isEqual thatDay) relativeWords(capitalise)('yesterday)
		else if (today.plusDays(1) isEqual thatDay) relativeWords(capitalise)('tomorrow)
		else absoluteDate
	}
}

class DateBuilder extends TemplateMethodModelEx {
	import DateBuilder.format

	/** For Freemarker */
	override def exec(list: JList[_]): String = {
		val args = list.toSeq.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }

		val date = args.head match {
			case partial: LocalDate => partial.toDateTimeAtStartOfDay
			case local:LocalDateTime => local.toDateTime
			case instant: ReadableDateTime => instant
			case _ => throw new IllegalArgumentException("Bad date argument")
		}

		args.tail match {
			case Seq(secs: JBoolean, at: JBoolean, tz: JBoolean, caps: JBoolean, relative: JBoolean, split: JBoolean, shortMonth: JBoolean, includeTime: JBoolean) =>
				format(date=date,
					includeSeconds=secs,
					includeAt=at,
					includeTimezone=tz,
					capitalise=caps,
					relative=relative,
					split=split,
					shortMonth=shortMonth,
					includeTime=includeTime)
			case _ => throw new IllegalArgumentException("Bad args")
		}
	}
}
