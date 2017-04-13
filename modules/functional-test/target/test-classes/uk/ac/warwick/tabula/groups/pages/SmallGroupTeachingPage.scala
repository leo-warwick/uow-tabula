package uk.ac.warwick.tabula.groups.pages

import org.openqa.selenium.{By, WebDriver, WebElement}
import uk.ac.warwick.tabula.{BreadcrumbsMatcher, EventuallyAjax, FunctionalTestAcademicYear, FunctionalTestProperties}
import org.scalatest.selenium.Page
import org.scalatest.selenium.WebBrowser

import scala.collection.JavaConverters._
import org.scalatest.concurrent.Eventually
import org.scalatest.Matchers


class SmallGroupTeachingPage(val departmentCode:String, val academicYear: String)(implicit val webDriver:WebDriver)
	extends Page with WebBrowser with	BreadcrumbsMatcher with EventuallyAjax with Matchers  with GroupSetList {

	val url: String = "%s/groups/admin/department/%s/%s".format(FunctionalTestProperties.SiteRoot, departmentCode, academicYear)

	def isCurrentPage: Boolean = {
		currentUrl should include ("/groups/admin/department/" + departmentCode + "/" + academicYear)
		pageTitle == "Tabula - Small Group Teaching"
	}

	def getBatchOpenButton: WebElement = {
		val manageButton = find(linkText("Manage")).get.underlying
		manageButton.click()
		val manageDropdownContainer = find(cssSelector("div.dept-toolbar")).get.underlying
		val openButton = manageDropdownContainer.findElement(By.partialLinkText("Open"))
		eventually {
			openButton.isDisplayed should be {true}
		}
		openButton
	}
}

class GroupSetInfoSummarySection(val underlying: WebElement, val moduleCode: String)(implicit webDriver: WebDriver) extends Eventually with Matchers {

	val groupsetId: String = {
		underlying.getAttribute("id").replaceFirst("set-","")
	}

	def goToEditProperties: EditSmallGroupSetPropertiesPage = {
		underlying.findElement(By.partialLinkText("Edit")).click()
		val editGroupset = underlying.findElement(By.partialLinkText("Properties"))
		eventually {
			editGroupset.isDisplayed should be {true}
		}
		editGroupset.click()
		val propsPage = new EditSmallGroupSetPropertiesPage()
		// HACK: the module name is the module code in uppercase in the test data. Should really pass it around separately
		propsPage.isCurrentPage(moduleCode.toUpperCase)
		propsPage
	}

	def goToEditGroups: EditSmallGroupSetGroupsPage = {
		val propsPage = goToEditProperties
		propsPage.goToEditGroups()

		val groupsPage = new EditSmallGroupSetGroupsPage()
		// HACK: the module name is the module code in uppercase in the test data. Should really pass it around separately
		groupsPage.isCurrentPage(moduleCode.toUpperCase)
		groupsPage
	}

	def goToEditStudents: EditSmallGroupSetStudentsPage = {
		val propsPage = goToEditProperties
		propsPage.goToEditStudents()

		val studentsPage = new EditSmallGroupSetStudentsPage()
		// HACK: the module name is the module code in uppercase in the test data. Should really pass it around separately
		studentsPage.isCurrentPage(moduleCode.toUpperCase)
		studentsPage
	}

	def goToEditEvents: EditSmallGroupSetEventsPage = {
		val propsPage = goToEditProperties
		propsPage.goToEditEvents()

		val eventsPage = new EditSmallGroupSetEventsPage()
		// HACK: the module name is the module code in uppercase in the test data. Should really pass it around separately
		eventsPage.isCurrentPage(moduleCode.toUpperCase)
		eventsPage
	}

	def goToAllocate: AllocateStudentsToGroupsPage = {
		val propsPage = goToEditProperties
		propsPage.goToAllocate()

		val allocatePage = new AllocateStudentsToGroupsPage()
		allocatePage.isCurrentPage(moduleCode.toUpperCase)
		allocatePage
	}

	def isShowingOpenButton: Boolean = {
		underlying.findElement(By.partialLinkText("Actions")).click()
		!underlying.findElements(By.partialLinkText("Open")).isEmpty
	}

	def getOpenButton: WebElement = {
		underlying.findElement(By.partialLinkText("Actions")).click()
		underlying.findElement(By.partialLinkText("Open"))
	}
}

class ModuleGroupSetInfoSummarySection(val underlying: WebElement, val moduleCode: String)(implicit webDriver: WebDriver) extends Eventually with Matchers {

	val groupsetId: String = {
		val classes = underlying.getAttribute("class").split(" ")
		classes.find(_.startsWith("groupset-")).get.replaceFirst("groupset-","")
	}

	def getSignupButton: WebElement = {
		underlying.findElement(By.className("sign-up-button"))
	}

	def findLeaveButtonFor(groupName:String): Option[WebElement] = {
		underlying.findElements(By.tagName("h4")).asScala.find(e => e.getText.trim.startsWith(groupName + " ")).flatMap(

			groupNameHeading=>{
				val parent = groupNameHeading.findElement(By.xpath(".."))
				//groupNameHeading.findElement(By.xpath("../form/input[@type='submit']"))}
				val maybeButton = parent.findElements(By.cssSelector("form input.btn")) // zero or 1-element java.util.List
				if (maybeButton.size()==0){
					None
				}else{
					Some(maybeButton.get(0))
				}
			}
		)
	}

	def showsGroupLockedIcon(): Boolean = {
		underlying.findElements(By.className("fa-lock")).asScala.nonEmpty

	}

	def showsGroup(groupName:String): Boolean = underlying.findElements(By.tagName("h4")).asScala.exists(e => e.getText.trim.startsWith(groupName + " "))

	def findSelectGroupCheckboxFor(groupName:String ): WebElement = {
		val groupNameHeading = underlying.findElements(By.tagName("h4")).asScala.filter(e=>e.getText.trim.startsWith(groupName + " ")).head
		// ugh. Might be worth investigating ways of using JQuery selector/traversals in selenium instead of this horror:
		groupNameHeading.findElement(By.xpath("../../div[contains(@class,'pull-left')]/input"))
	}
}

class BatchOpenPage(val departmentCode: String, val academicYear: FunctionalTestAcademicYear)(implicit webDriver: WebDriver) extends Page with WebBrowser with Eventually with Matchers {
	val url: String = FunctionalTestProperties.SiteRoot + s"/groups/admin/department/$departmentCode/groups/open"

	def isCurrentPage: Boolean = {
		currentUrl should include (s"/groups/admin/department/$departmentCode/${academicYear.startYear.toString}/groups/selfsignup/open")
		find(cssSelector(".id7-main-content h1")).get.text.startsWith(s"Open groups for ${academicYear.toString}")
	}

	def checkboxForGroupSet(groupset: GroupSetInfoSummarySection): WebElement = {
		findAll(tagName("input")).filter(_.underlying.getAttribute("value") == groupset.groupsetId).next().underlying
	}

	def submit() {
		findAll(tagName("input")).filter(_.underlying.getAttribute("type") == "submit").next().underlying.click()
	}
}

trait GroupSetList {
	this: WebBrowser with EventuallyAjax with Matchers =>

	def getGroupsetInfo(moduleCode: String, groupsetName: String)(implicit webdriver:WebDriver): Option[GroupSetInfoSummarySection] = {

		// wait for sets to load ajaxically
		eventuallyAjax {
			Option(className("small-group-sets-list").webElement.findElement(By.className("mod-code"))
				.getText.trim == s"${moduleCode.toUpperCase}") should be ('defined)
		}

		val setInfoElements = findAll(className("set-info")).filter { el =>
			el.underlying.findElement(By.className("mod-code")).getText.trim == moduleCode.toUpperCase &&
				el.underlying.findElement(By.className("group-name")).getText.trim == groupsetName
		}

		if (setInfoElements.isEmpty) {
			None
		}	else {
			Some(setInfoElements.next().underlying).map { section =>
				if (section.getAttribute("class").indexOf("collapsible") != -1 && section.getAttribute("class").indexOf("expanded") == -1) {
					click on section.findElement(By.className("section-title"))

					eventuallyAjax {
						section.getAttribute("class") should include ("expanded")
					}
				}

				new GroupSetInfoSummarySection(section, moduleCode)
			}
		}
	}
}

trait ModuleAndGroupSetList {
	this: WebBrowser with EventuallyAjax with Matchers =>

	def getGroupsetInfo(moduleCode: String, groupsetName: String)(implicit webdriver: WebDriver): Option[ModuleGroupSetInfoSummarySection] = {
		getModuleInfo(moduleCode).flatMap { module =>
			if (module.getAttribute("class").indexOf("collapsible") != -1 && module.getAttribute("class").indexOf("expanded") == -1) {
				click on module.findElement(By.className("section-title"))
				eventuallyAjax {
					module.getAttribute("class") should include("expanded")
				}
			}

			val groupSet = module.findElements(By.className("item-info")).asScala.find(_.findElement(By.className("name")).getText.trim == groupsetName)
			groupSet.map { new ModuleGroupSetInfoSummarySection(_, moduleCode) }
		}
	}

	private def getModuleInfo(moduleCode: String)(implicit webdriver: WebDriver): Option[WebElement] = {
		val moduleInfoElements = findAll(className("module-info")).filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase)
		if (moduleInfoElements.isEmpty) {
			None
		} else {
			Some(moduleInfoElements.next().underlying)
		}
	}
}