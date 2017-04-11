package uk.ac.warwick.tabula.helpers

import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import java.io.InputStreamReader

class ReflectionHelperTest extends TestBase with ReflectionsSetup {

	@Test def allPermissionsTargets() = {
		ReflectionHelper.allPermissionTargets.contains(classOf[Department]) should be {true}
	}

	@Test def notifications() = {
		val map = ReflectionHelper.allNotifications
		map.contains("SubmissionDueGeneral") should be {true}
	}

	@Test def allPermissions() = {
		ReflectionHelper.allPermissions.contains(Permissions.Module.ManageAssignments) should be {true}
	}

	@Test def groupedPermissions() = {
		ReflectionHelper.groupedPermissions("Module").contains(("Module.Create", "Module.Create")) should be {true}
	}

}

class CatchAllUrlStreamHandlerFactory extends URLStreamHandlerFactory {
	override def createURLStreamHandler(protocol: String): CatchAllUrlHandler =
		if ("vfszip".equals(protocol) || "zip".equals(protocol)) new CatchAllUrlHandler
		else null
}

class CatchAllUrlHandler extends URLStreamHandler {
	override def openConnection(url: URL): URLConnection = {
		throw new UnsupportedOperationException()
	}
}

// Not necessary any more, as scanning is done at runtime.
trait ReflectionsSetup