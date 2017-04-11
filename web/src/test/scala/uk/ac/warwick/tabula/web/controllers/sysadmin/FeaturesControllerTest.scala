package uk.ac.warwick.tabula.web.controllers.sysadmin

import uk.ac.warwick.tabula.{FeaturesMessage, TestBase, Features}
import org.scalatest.mock.MockitoSugar
import uk.ac.warwick.util.queue.Queue
import uk.ac.warwick.util.queue.conversion.{AnnotationJsonObjectConverter, JsonMessageConverter}
import uk.ac.warwick.tabula.services.MaintenanceModeMessage
import uk.ac.warwick.tabula.JavaImports.JArrayList

class FeaturesControllerTest extends TestBase with MockitoSugar {
	@Test def access {
		val features = Features.empty
		val controller = new FeaturesController
		controller.features = features
		controller.queue = mock[Queue]

		controller.afterPropertiesSet

		controller.currentValues should not be ('empty)

		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (false)

		features.emailStudents = true

		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (true)

		withUser("cuscav") {
			controller.update("emailStudents", false)
		}

		controller.currentValues.find { _.name == "emailStudents" }.get.value should be (false)
	}

	@Test def featuresMessageConversion() {

		val json = new AnnotationJsonObjectConverter
		val features = Features.empty
		features.emailStudents = true
		val featuresMessage = new FeaturesMessage(features)
		val string = json.toJson(featuresMessage).toString()
		string should not be ("{}")

	}
}
