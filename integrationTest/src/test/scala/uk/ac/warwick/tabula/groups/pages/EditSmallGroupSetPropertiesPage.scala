package uk.ac.warwick.tabula.groups.pages

import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.Select
import org.scalatest.selenium.WebBrowser
import uk.ac.warwick.tabula.BreadcrumbsMatcher

trait EditSmallGroupSetProgressWizardLinks {
  self: WebBrowser =>

  implicit def webDriver: WebDriver

  def goToEditProperties(): Unit = {
    click on linkText("Properties")
  }

  def goToEditGroups(): Unit = {
    click on linkText("Groups")
  }

  def goToEditStudents(): Unit = {
    click on linkText("Students")
  }

  def goToEditEvents(): Unit = {
    click on linkText("Events")
  }

  def goToAllocate(): Unit = {
    click on linkText("Allocation")
  }

}

class EditSmallGroupSetPropertiesPage(implicit val webDriver: WebDriver) extends WebBrowser with BreadcrumbsMatcher with EditSmallGroupSetProgressWizardLinks {

  def isCurrentPage(moduleName: String): Boolean = {
    breadCrumbsMatchID7(Seq("Test Services", moduleName.toUpperCase))
    val heading = find(cssSelector(".id7-main-content h1")).get
    heading.text should startWith("Edit small groups")
    heading.text.startsWith("Edit small groups")
  }

  def submitAndExit(): Unit = {
    click on cssSelector("input.btn-primary[value='Save and exit']")
  }

  def save(): Unit = {
    click on cssSelector("input.btn-primary[value='Save']")
  }

}

class EditSmallGroupSetGroupsPage(implicit val webDriver: WebDriver) extends WebBrowser with BreadcrumbsMatcher with EditSmallGroupSetProgressWizardLinks {

  def isCurrentPage(moduleName: String): Boolean = {
    breadCrumbsMatchID7(Seq("Test Services", moduleName.toUpperCase))
    val heading = find(cssSelector(".id7-main-content h1")).get
    heading.text should startWith("Edit small groups")
    currentUrl should endWith("/groups")
    currentUrl.endsWith("/groups")
  }

  def submitAndExit(): Unit = {
    click on cssSelector("input.btn-primary[value='Save and exit']")
  }

  def save(): Unit = {
    click on cssSelector("input.btn-primary[value='Save']")
  }

}

class EditSmallGroupSetStudentsPage(implicit val webDriver: WebDriver) extends WebBrowser with BreadcrumbsMatcher with EditSmallGroupSetProgressWizardLinks {

  def isCurrentPage(moduleName: String): Boolean = {
    breadCrumbsMatchID7(Seq("Test Services", moduleName.toUpperCase))
    val heading = find(cssSelector(".id7-main-content h1")).get
    heading.text should startWith("Edit small groups")
    currentUrl should endWith("/students")
    currentUrl.endsWith("/students")
  }

  def submitAndExit(): Unit = {
    click on cssSelector("input.btn-primary[value='Save and exit']")
  }

  def save(): Unit = {
    click on cssSelector("input.btn-primary[value='Save']")
  }

}

class EditSmallGroupSetEventsPage(implicit val webDriver: WebDriver) extends WebBrowser with BreadcrumbsMatcher with EditSmallGroupSetProgressWizardLinks {

  def isCurrentPage(moduleName: String): Boolean = {
    breadCrumbsMatchID7(Seq("Test Services", moduleName.toUpperCase))
    val heading = find(cssSelector(".id7-main-content h1")).get
    heading.text should startWith("Edit small groups")
    currentUrl should endWith("/events")
    currentUrl.endsWith("/events")
  }

  def submitAndExit(): Unit = {
    click on cssSelector("input.btn-primary[value='Save and exit']")
  }

  def save(): Unit = {
    click on cssSelector("input.btn-primary[value='Save']")
  }

}

class AllocateStudentsToGroupsPage(implicit val webDriver: WebDriver) extends WebBrowser with BreadcrumbsMatcher with EditSmallGroupSetProgressWizardLinks {
  def isCurrentPage(moduleName: String): Boolean = {
    breadCrumbsMatchID7(Seq("Test Services", moduleName.toUpperCase))
    val heading = find(cssSelector(".id7-main-content h1")).get
    heading.text should startWith("Edit small groups")
    currentUrl should endWith("/allocate")
    currentUrl.endsWith("/allocate")
  }

  def findAllUnallocatedStudents: Iterator[Element] = {
    findAll(cssSelector("ul.student-list li"))
  }

  // use native selenium select, because the scalatest SingleSel doesn't allow enumerating its values
  def findFilterDropdown(filterAttribute: String): Option[Select] = {
    val filterDropdowns = findAll(cssSelector("div.filter select"))
    filterDropdowns.find(_.underlying.getAttribute("data-filter-attr") == filterAttribute).map(e => new Select(e.underlying))
  }

}
