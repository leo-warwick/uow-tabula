package uk.ac.warwick.tabula.web.views

import java.io.FileReader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.helpers.Logging

/**
 * Checks Freemarker templates for syntax errors. This will include
 * things like an unclosed FTL tag or a local outside of a macro,
 * but NOT things like a typo in a variable name or even macro name.
 * Each template is parsed without any external input, and not executed.
 *
 * This class only parses files in common. It is subclassed in each web module
 * to check that module's files.
 */
class CommonFreemarkerSyntaxTest extends TestBase with Logging {
	@Test
	def parseAll() {
		val resolver = new PathMatchingResourcePatternResolver
		val resources = resolver.getResources("/WEB-INF/freemarker/**/*.ftl")
		val conf = newFreemarkerConfiguration()
		if (resources.isEmpty) fail("No Freemarker templates found!")
		logger.debug(s"Checking syntax for ${resources.length} files")
		for (resource <- resources) {
			new freemarker.template.Template(resource.getFile.getAbsolutePath, new FileReader(resource.getFile), conf)
		}
	}
}
