package uk.ac.warwick.tabula.data
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.FileAttachment

class FileAttachmentTest extends TestBase {

	/**
	 * HFC-136
	 * Filenames can be prefixed with whitespace, preventing download
	 */
	@Test
	def untrimmedFilenames {
		new FileAttachment(" 0123456.docx").name should be ("0123456.docx")
		new FileAttachment(null).name should be (null)
	}

	@Test
	def badWindowsFilenames {
		new FileAttachment(""" \Fun: good or bad? <You|decide>/.docx""").name should be ("Fun good or bad Youdecide.docx")
	}

	@Test
	def removeSemicolons() {
		new FileAttachment("We've had our fun; now let's go home.xlsx").name should be ("We've had our fun now let's go home.xlsx")
	}

	@Test
	def tidySpaces() {
		new FileAttachment("Too     many spaces.pdf").name should be ("Too many spaces.pdf")
		new FileAttachment("I\thate\ttabs.txt").name should be ("I hate tabs.txt")
		new FileAttachment("Think%20you're%20a%20script%20kiddie, then?.js").name should be ("Think you're a script kiddie, then.js")
	}

	@Test
	def tidyNonAscii() {
		new FileAttachment("看看我的鱼.pdf").name should be ("download.pdf")
		new FileAttachment("看看我的鱼.球").name should be ("download.unknowntype")
		new FileAttachment("Aperçu.pdf").name should be ("Aperu.pdf")
	}

	@Test
	def tidyExtensions() {
		new FileAttachment("No spaces after this. pdf").name should be ("No spaces after this.pdf")
		new FileAttachment("No extension here.").name should be ("No extension here.unknowntype")
		new FileAttachment(".docx").name should be ("download.docx")
		new FileAttachment(".").name should be ("download.unknowntype")
		new FileAttachment(".hidden.unix.file.txt").name should be ("hidden.unix.file.txt")
	}
}