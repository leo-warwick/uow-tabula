package uk.ac.warwick.tabula

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test

import uk.ac.warwick.util.queue.conversion.ItemType

class FeaturesTest extends TestBase {

		@Test def jackson() {
				val features = new FeaturesImpl
				val mapper = new ObjectMapper

				val json = mapper.writeValueAsString(features)
				assert(Option(json).isDefined)

				val features2 = mapper.readValue(json, classOf[FeaturesImpl])
				assert(features != features2)
				assert(features.emailStudents === features2.emailStudents)
		}

		@Test def update() {
				val features1 = new FeaturesImpl
				val features2 = new FeaturesImpl

				// Toggle
				features1.emailStudents = !features1.emailStudents

				val message = new FeaturesMessage(features1)
				message.emailStudents should be (features1.emailStudents)

				features2.update(message)
				features2.emailStudents should be (features1.emailStudents)
		}

}