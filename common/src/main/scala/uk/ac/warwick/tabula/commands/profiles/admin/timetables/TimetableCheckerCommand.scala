package uk.ac.warwick.tabula.commands.profiles.admin.timetables

import com.fasterxml.jackson.databind.ObjectMapper
import dispatch.classic.url
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.timetables._
import uk.ac.warwick.tabula.services.{AutowiringDispatchHttpClientComponent, DispatchHttpClientComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{JsonObjectMapperFactory, RequestFailedException}

import scala.util.{Failure, Success, Try}

object TimetableCheckerCommand {

	def apply() =
		new TimetableCheckerCommandInternal()
			with ComposableCommand[Unit]
			with TimetableCheckerCommandPermissions
			with AutowiringCelcatConfigurationComponent
			with AutowiringDispatchHttpClientComponent
			with Unaudited
}

class TimetableCheckerCommandInternal() extends CommandInternal[Unit] with TimetableCheckerCommandState {
	self: CelcatConfigurationComponent
	with DispatchHttpClientComponent =>
	def applyInternal(): Unit = {

		val jsonMapper: ObjectMapper = new JsonObjectMapperFactory().createInstance

		val wbsConfiguration: CelcatDepartmentConfiguration = celcatConfiguration.wbsConfiguration

		def handler = { (headers: Map[String, Seq[String]], req: dispatch.classic.Request) =>
			req >- { (jsonString) =>
				val jsonObject = if (jsonMapper != null) jsonMapper.readValue(jsonString, classOf[List[Map[String, Any]]])
				jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject)
			}
		}

		val req =
			(url(wbsConfiguration.baseUri) / warwickUniId <<? Map("forcebasic" -> "true"))
				.as_!(wbsConfiguration.credentials.username, wbsConfiguration.credentials.password)

		Try(httpClient.when(_ == 200)(req >:+ handler))
		match {
			case Success(jsonData) => wbsFeed = jsonData
			case Failure(e) => throw new RequestFailedException("The WBS timetabling service could not be reached", e)
		}
	}
}

trait TimetableCheckerCommandState {
	var warwickUniId: String = _
	var wbsFeed: String = _
}

trait TimetableCheckerCommandPermissions extends RequiresPermissionsChecking {
	def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(Permissions.Timetabling.Checker)
	}
}