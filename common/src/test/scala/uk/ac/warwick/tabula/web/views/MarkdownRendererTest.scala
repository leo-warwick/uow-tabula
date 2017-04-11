package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.TestBase

import scala.concurrent._
import scala.util.{Try, Success, Failure}
import uk.ac.warwick.tabula.helpers.Futures._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit

class MarkdownRendererTest extends TestBase {

	val renderer = new MarkdownRendererImpl {}

	// Trying to reproduce ClassCast error.
	@Test def TAB_2169() {
		val input = """This message confirms that you made a submission for the assignment 'Assessment 2' for EC204 Economics 2.
									|
									|- Submission date: ${submissionDate}
									|- Submission ID: ${submission.id}
									|- University ID: ${user.warwickId}
									|
									|Uploaded attachments:
									|
									|- Whatever
									|- Who cares
								""".stripMargin
		val output = renderer.renderMarkdown(input)
		output should (include("<li>Whatever</li>") and include("<li>University ID: ${user.warwickId}</li>"))

		// Check multithreadedness by rendering a lot at once.
		val results = (1 to 10).map (
			i => Future { renderer.renderMarkdown(input + i) 	}
		)

		for (result <- results) {
			// Blocking wait
			Await.ready(result, Duration(1, TimeUnit.SECONDS))
			result.value.get match {
				case Success(v) => //
				case Failure(e) => fail(e)
			}
		}
	}



}
