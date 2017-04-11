package uk.ac.warwick.tabula.services.healthchecks

import org.joda.time.DateTime
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.helpers.StringUtils._

trait ServiceHealthcheckProvider {
	var latest: Option[ServiceHealthcheck] = None

	protected def update(results: ServiceHealthcheck): Unit = {
		latest = Some(results)
	}

	def run(): Unit
}

object ServiceHealthcheck {

	abstract class Status(val asString: String)
	object Status {
		case object Okay extends Status("okay")
		case object Warning extends Status("warning")
		case object Error extends Status("error")
	}

	class PerformanceData[A](val name: String, val value: A, val warningThreshold: Option[A], val errorThreshold: Option[A], val minimumValue: Option[A], val maximumValue: Option[A]) {
		def asString: String =
			if (warningThreshold.nonEmpty && errorThreshold.nonEmpty) {
				if (minimumValue.nonEmpty && maximumValue.nonEmpty) {
					s"$name=$value;${warningThreshold.get};${errorThreshold.get};${minimumValue.get};${maximumValue.get}"
				} else {
					s"$name=$value;${warningThreshold.get};${errorThreshold.get}"
				}
			} else {
				s"$name=$value"
			}
	}

	object PerformanceData {
		def apply[A](name: String, value: A): PerformanceData[A] =
			new PerformanceData(name, value, None, None, None, None)

		def apply[A](name: String, value: A, warningThreshold: A, errorThreshold: A): PerformanceData[A] =
			new PerformanceData(name, value, Some(warningThreshold), Some(errorThreshold), None, None)

		def apply[A](name: String, value: A, warningThreshold: A, errorThreshold: A, minimumValue: A, maximumValue: A): PerformanceData[A] =
			new PerformanceData(name, value, Some(warningThreshold), Some(errorThreshold), Some(minimumValue), Some(maximumValue))
	}

}

case class ServiceHealthcheck(
	name: String,
	status: ServiceHealthcheck.Status, // okay, warning, error
	testedAt: DateTime,
	message: String = "",
	performanceData: Seq[ServiceHealthcheck.PerformanceData[_]] = Nil
) {
	def asMap: Map[String, Any] =
		Map(
			"name" -> name,
			"status" -> status.asString,
			"testedAt" -> DateFormats.IsoDateTime.print(testedAt),
			"perfData" -> performanceData.toArray.map { _.asString }
		) ++ message.maybeText.map { "message" -> _ }
}
