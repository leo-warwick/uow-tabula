package uk.ac.warwick.tabula.coursework.pages

import org.openqa.selenium.{By, Keys, WebDriver, WebElement}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.Matchers
import org.scalatest.selenium.WebBrowser
import uk.ac.warwick.tabula.{FunctionalTestAcademicYear, FunctionalTestProperties}

import scala.collection.JavaConverters._


class SetupAssignmentsPage(val departmentCode: String)(implicit driver: WebDriver) extends WebBrowser with Matchers with Eventually with IntegrationPatience {
	val thisYear: FunctionalTestAcademicYear = FunctionalTestAcademicYear.currentSITS
	val url = s"${FunctionalTestProperties.SiteRoot}/coursework/admin/department/$departmentCode/setup-assignments?academicYear=${thisYear}"

	def shouldBeCurrentPage() {
		currentUrl should include(s"/coursework/admin/department/$departmentCode/setup-assignments")
		cssSelector("#main-content h1").element.text should be ("Setup assignments")
	}

	def itemRows: Seq[Element] = cssSelector("tr.itemContainer").findAllElements.toSeq

	def getCheckboxForRow(row: Element): WebElement = {
		row.underlying.findElement(By.cssSelector("input[type=checkbox]"))
	}

	def getOptionIdForRow(row: Element): Option[String] = {
		row.underlying.findElements(By.cssSelector(".options-id-label .label")).asScala.headOption.map { _.getText() }
	}

	def setTitleForRow(row: Element, title: String) {
		row.underlying.findElement(By.cssSelector("a.name-edit-link")).click()
		val textField = row.underlying.findElement(By.cssSelector(".editable-inline input[type=text]"))
		textField.clear()
		textField.sendKeys(title)
		textField.sendKeys(Keys.ENTER)

		val span = row.underlying.findElement(By.cssSelector(".editable-name"))
		eventually { span.getText should be (title) }
	}

	def itemCount: Int = itemRows.size

	def clickNext(): Unit = clickButtonWithAction("options")
	def clickBack(): Unit = clickButtonWithAction("refresh-select")
	def clickSubmit(): Unit = clickButtonWithAction("submit")

	// All the navigation buttons have an "action" data attribute.
	private def clickButtonWithAction(action: String) {
		cssSelector(s"button[data-action=${action}]").webElement.click()
	}

}
