package uk.ac.warwick.tabula.cm2

import org.joda.time.{DateTimeConstants, LocalDate}
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.{By, Keys, WebElement}
import org.scalatest.GivenWhenThen
import uk.ac.warwick.tabula.data.model.WorkflowCategory
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import uk.ac.warwick.tabula.{AcademicYear, BrowserTest, DateFormats, LoginDetails}
import uk.ac.warwick.util.workingdays.WorkingDaysHelperImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._

trait CourseworkFixtures extends BrowserTest with FeaturesDriver with FixturesDriver with GivenWhenThen {

  val TEST_MODULE_CODE = "xxx02"
  val TEST_MODULE_CODE1 = "xxx01"

  val TEST_ROUTE_CODE = "xx456"
  val TEST_DEPARTMENT_CODE = "xxx"
  val TEST_COURSE_CODE = "Ux456"
  val moreBefore: () => Unit = () => ()

  before {
    Given("The test department exists")
    go to Path("/fixtures/setup")
    pageSource should include("Fixture setup successful")

    // create 2 assignments with one Premarked
    createCM2Assignment(TEST_MODULE_CODE1, "false", "Assignment CM2-01")
    createCM2Assignment(TEST_MODULE_CODE)

    val assessmentFuture = Future {
      And("There is an assessment component for module xxx01")
      createAssessmentComponent("XXX", "XXX01-16", "Cool essay")

      And("There is an upstream assessment group for xxx01 with students1-4 in it")
      createUpstreamAssessmentGroup("XXX01-16", Seq(P.Student1.warwickId, P.Student2.warwickId, P.Student3.warwickId, P.Student4.warwickId))
    }

    And("student1-4 are members")
    createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "Test Route")
    createCourse(TEST_COURSE_CODE, "Test Course")

    // Make them at the same time.
    val concurrentJobs = Seq(
      assessmentFuture,
      Future {
        createStudentMember(
          P.Student1.usercode,
          routeCode = TEST_ROUTE_CODE,
          courseCode = TEST_COURSE_CODE,
          deptCode = TEST_DEPARTMENT_CODE,
          academicYear = AcademicYear.now().startYear.toString
        )
      },
      Future {
        createStudentMember(
          P.Student2.usercode,
          routeCode = TEST_ROUTE_CODE,
          courseCode = TEST_COURSE_CODE,
          deptCode = TEST_DEPARTMENT_CODE,
          academicYear = AcademicYear.now().startYear.toString
        )
      },
      Future {
        createStudentMember(
          P.Student3.usercode,
          routeCode = TEST_ROUTE_CODE,
          courseCode = TEST_COURSE_CODE,
          deptCode = TEST_DEPARTMENT_CODE,
          academicYear = AcademicYear.now().startYear.toString
        )
      },
      Future {
        createStudentMember(
          P.Student4.usercode,
          routeCode = TEST_ROUTE_CODE,
          courseCode = TEST_COURSE_CODE,
          deptCode = TEST_DEPARTMENT_CODE,
          academicYear = AcademicYear.now().startYear.toString
        )
      }
    )

    // Wait to complete
    for {job <- concurrentJobs} Await.ready(job, Duration(60, duration.SECONDS))

    moreBefore()
  }

  def addModuleManagers(
    moduleCode: String,
    managers: Seq[String] = Seq(),
    assistants: Seq[String] = Seq()): Unit = as(P.Admin1) {
    click on linkText("Test Services")
    verifyPageLoaded {
      // wait for the page to load
      find(cssSelector("div.deptheader")) should be(Symbol("defined"))
    }
    if ((assistants ++ managers).nonEmpty) {
      val module = eventually {
        getModule(moduleCode).get
      }
      click on module.findElement(By.partialLinkText("Manage this module"))
      val editPerms = module.findElement(By.partialLinkText("Module permissions"))
      eventually(editPerms.isDisplayed should be (true))
      click on editPerms

      def pick(table: String, usercodes: Seq[String]): Unit = {
        verifyPageLoaded {
          find(cssSelector(s"$table .pickedUser")) should be(Symbol("defined"))
        }
        usercodes.foreach { u =>
          click on cssSelector(s"$table .pickedUser")
          enter(u)
          val typeahead = cssSelector(".typeahead .active a")
          eventually {
            find(typeahead) should not be None
          }
          click on typeahead
          find(cssSelector(s"$table form.add-permissions")).get.underlying.submit()
        }
      }

      pick(".modulemanager-table", managers)
      pick(".moduleassistant-table", assistants)
    }
  }

  def showModulesWithNoFilteredAssignments(): Unit = {
    if (!checkbox("showEmptyModules").isSelected) {
      click on find(cssSelector("#module-filter > .btn-filter")).get
      checkbox("showEmptyModules").select()
    }
  }

  /* Runs callback with assignment ID */
  def withAssignment(
    moduleCode: String,
    assignmentName: String,
    studentSettings: Seq[String] => Unit = Nil => (),
    submissionSettings: () => Unit = () => (),
    optionSettings: () => Unit = () => (),
    students: Seq[String] = Seq(P.Student1.usercode, P.Student2.usercode),
    loggedUser: LoginDetails = P.Admin1
  )(callback: String => Unit): Unit = as(loggedUser) {

    click on linkText("Test Services")
    verifyPageLoaded {
      // wait for the page to load
      find(cssSelector("div.deptheader")) should be(Symbol("defined"))
    }
    loadCurrentAcademicYearTab()

    showModulesWithNoFilteredAssignments()

    eventually {
      val module = getModule(moduleCode).get
      click on module.findElement(By.partialLinkText("Manage this module"))

      val addAssignment = module.findElement(By.partialLinkText("Create new assignment"))
      eventually(addAssignment.isDisplayed should be (true))
      click on addAssignment
    }

    val prefilledReset = linkText("for this assignment")
    if (prefilledReset.findElement.isDefined) {
      click on prefilledReset
    }

    textField("name").value = assignmentName

    // Roll the open date to the last working day
    val holidayDates: Seq[LocalDate] = new WorkingDaysHelperImpl().getHolidayDates.asScala.toSeq.map(_.asJoda).sorted
    def isWorkingDay(date: LocalDate): Boolean =
      !holidayDates.contains(date) && date.getDayOfWeek != DateTimeConstants.SATURDAY && date.getDayOfWeek != DateTimeConstants.SUNDAY

    var openDate: LocalDate = LocalDate.now().minusDays(1)
    while (!isWorkingDay(openDate)) {
      openDate = openDate.minusDays(1)
    }
    textField("openDate").value = DateFormats.DatePickerFormatter.print(openDate)

    singleSel("workflowCategory").value = WorkflowCategory.NoneUse.code

    cssSelector(s"input[name=createAndAddFeedback]").webElement.click()
    eventually(pageSource contains "Edit feedback settings" should be (true))
    click on linkText("Students").findElement.get.underlying

    eventually {
      textArea("massAddUsers").isDisplayed should be (true)
    }
    if (students.nonEmpty) {
      textArea("massAddUsers").value = students.mkString("\n")
      click on className("add-students-manually")
      eventually(pageSource contains "Your changes will not be recorded until you save this assignment." should be (true))
      eventually {
        pageSource should include(s"${students.size} manually enrolled")
      }
    }
    studentSettings(students)

    cssSelector(s"input[value='Save and continue']").webElement.click()
    submissionSettings()

    cssSelector(s"input[value='Save and continue']").webElement.click()
    optionSettings()

    cssSelector(s"input[value='Save and exit']").webElement.click()

    // Ensure that we've been redirected back
    withClue(pageSource) {
      currentUrl should endWith("/summary")
    }

    val pattern = """.*admin/assignments/(.*)/summary""".r
    val assignmentId = pattern.findAllIn(currentUrl).matchData.toSeq.headOption.map(_.group(1))
    assignmentId.isDefined should be (true)
    callback(assignmentId.get)
  }

  def withAssignmentWithWorkflow(workflowType: MarkingWorkflowType, markers: Seq[LoginDetails]*)(callback: String => Unit): Unit = {

    def withWorkflowSubmissionSettings() = {
      radioButtonGroup("restrictSubmissions").value = "true"
    }

    withAssignment("xxx01", s"${workflowType.description} - single use", submissionSettings = withWorkflowSubmissionSettings) { id =>

      When("I click on the edit button")
      click on partialLinkText("Edit assignment")
      Then("I see the edit details screen")
      eventually(pageSource contains "Edit assignment details" should be (true))

      When("I choose Single use")
      singleSel("workflowCategory").value = WorkflowCategory.SingleUse.code
      Then("I see the options for single use workflows")
      eventually(pageSource contains "Marking workflow type" should be (true))

      When("I select single marking add a markers usercode")
      singleSel("workflowType").value = workflowType.name
      eventually(pageSource contains "Add marker" should be (true))

      Seq("markersA" -> markers.headOption.getOrElse(Nil), "markersB" -> markers.tail.headOption.getOrElse(Nil)).foreach { case (field, m) =>
        m.zipWithIndex.foreach { case (marker, i) =>
          val markerField = findAll(cssSelector(s".$field input.flexi-picker")).toList.apply(i)
          markerField.underlying.sendKeys(marker.usercode)

          // Wait for the flexi picker to find a result, then select it
          eventually {
            findAll(cssSelector(".flexi-picker-result")).toList.count(_.isDisplayed) should be(1)
          }
          markerField.underlying.sendKeys(Keys.TAB)

          click on className(field).webElement.findElement(By.cssSelector("button.btn"))
          eventually {
            findAll(cssSelector(s".$field input.flexi-picker")).toList.count(_.isDisplayed) should be(1)
          }
        }
      }

      And("I submit the form")
      cssSelector(s"input[name=editAndEditDetails]").webElement.click()
      eventually(cssSelector(".confirm.btn-primary").webElement.isDisplayed shouldBe true)
      cssSelector(".confirm.btn-primary").webElement.click()
      Then("I should be back on the summary page")
      eventually(currentUrl should endWith("/summary"))
      callback(id)
    }
  }

  def as[T](user: LoginDetails)(fn: => T): T = {
    currentUser = user
    signIn as user to Path(s"/coursework")
    fn
  }

  def getModule(moduleCode: String): Option[WebElement] = {
    val element = className("filter-results").webElement.findElements(By.cssSelector("div.striped-section.admin-assignment-list "))
    val module = moduleCode.toUpperCase
    val matchingModule = element.asScala.find({
      _.findElement(By.cssSelector("span.mod-code")).getText == module
    })
    matchingModule
  }

  def openAdminPage(): Unit = {
    When("I go the admin page")
    click on linkText("Test Services")

    Then("I should reach the admin coursework page")
    eventually {
      currentUrl should endWith("/department/xxx")
    }
  }

  // assumes that you start at the summary screen for an assignment
  def navigateToMarkerAllocation(): Unit = {
    When("I click on the edit button again")
    click on partialLinkText("Edit assignment")
    Then("I see the edit details screen")
    eventually(pageSource contains "Edit assignment details" should be (true))

    When("I click on the Markers link")
    click on partialLinkText("Markers")
    Then("I see the assign markers screen")
    eventually(pageSource contains "Assign markers" should be (true))
  }

  // assumes that you start at the summary screen for an assignment
  def releaseForMarking(assignmentId: String): Unit = {
    When("I click on the relevant assignment edit button again")
    click on id("main").webElement.findElement(By.cssSelector(s"a[href$$='coursework/admin/assignments/$assignmentId/edit']"))
    Then("I see the edit details screen")
    eventually(pageSource contains "Edit assignment details" should be (true))

    When("I click on the Markers link")
    click on partialLinkText("Markers")
    Then("I see the assign markers screen")
    eventually(pageSource contains "Assign markers" should be (true))

    When("I randomly assign the markers")
    click on partialLinkText("Randomly allocate")
    Then("The two markers should be assigned")
    findAll(cssSelector(".drag-count")) foreach (_.underlying.getText should be("1"))

    When("I submit the marker allocation")
    cssSelector(s"input[name=createAndAddMarkers]").webElement.click()
    Then("I am redirected to the summary screen ")
    eventually(currentUrl should include(s"/admin/assignments/$assignmentId/summary"))

    When("I select all the submissions")
    eventually(click on cssSelector(".collection-check-all"))

    And("I choose to release for marking")
    click on partialLinkText("Marking")
    eventually({
      val release = partialLinkText("Release selected for marking").webElement
      release.isDisplayed should be (true)
      click on release
    })
    Then("I reach the release submissions screen")
    eventually(currentUrl should include(s"/admin/assignments/$assignmentId/release-submissions"))

    When("I confirm")
    eventually {
      cssSelector(s"input[name=confirm]").webElement.click()
      cssSelector(s"input[value=Confirm]").webElement.click()
    }
    Then("The submissions are released")
    eventually(currentUrl should include(s"/admin/assignments/$assignmentId/summary"))
  }

  def openMarkingWorkflowSettings(): Unit = {
    When("I click on the Marking workflows link")
    click on linkText("Marking workflows")
    Then("I should reach the marking workflow page")
    eventually {
      currentUrl should include("/markingworkflows")
    }
  }

  def createMarkingWorkflow(workflowName: String, workflowType: MarkingWorkflowType, markers: Seq[LoginDetails]*): String = as(P.Admin1) {
    openAdminPage()
    loadCurrentAcademicYearTab()
    openMarkingWorkflowSettings()

    When("I click create")
    click on partialLinkText("Create")
    Then("I should reach the create marking workflow page")
    currentUrl should include("/add")

    And("I enter the necessary data")
    textField("workflowName").value = workflowName
    singleSel("workflowType").value = workflowType.name
    var markerA = markers.headOption.getOrElse(Nil)
    Seq("markersA" -> markers.headOption.getOrElse(Nil), "markersB" -> markers.tail.headOption.getOrElse(Nil)).foreach { case (field, m) =>
      m.zipWithIndex.foreach { case (marker, i) =>
        val markerField = findAll(cssSelector(s".$field input.flexi-picker")).toList.apply(i)
        markerField.underlying.sendKeys(marker.usercode)

        // Wait for the flexi picker to find a result, then select it
        eventually {
          findAll(cssSelector(".flexi-picker-result")).toList.count(_.isDisplayed) should be(1)
        }
        markerField.underlying.sendKeys(Keys.TAB)

        click on className(field).webElement.findElement(By.cssSelector("button.btn"))
        eventually {
          findAll(cssSelector(s".$field input.flexi-picker")).toList.count(_.isDisplayed) should be(1)
        }
      }
    }
    And("I submit the form")
    submit()
    Then("I should be redirected back to the marking workflow page")
    eventually {
      currentUrl should endWith("/markingworkflows")
    }
    And("Row with that workflow should be there")
    val tbody = className("table").webElement.findElement(By.tagName("tbody"))
    val row = tbody.findElements(By.tagName("tr")).asScala.find({
      _.findElement(By.tagName("td")).getText == workflowName
    })
    row should be(Symbol("defined"))
    val link = row.get.findElement(By.partialLinkText("Modify"))
    val url = link.getAttribute("href")
    val pattern = """.*markingworkflows/(.*)/edit""".r
    val workflowId = pattern.findAllIn(url).matchData.toSeq.headOption.map(_.group(1))
    workflowId.get
  }


  def addMarkingWorkflow(workflowName: String, workflowType: MarkingWorkflowType)(callback: String => Unit): Unit = as(P.Admin1) {
    callback(createMarkingWorkflow(workflowName, workflowType, Seq(P.Marker1, P.Marker2), Seq(P.Marker3)))
  }

  def editAssignment(workflowId: String): Unit = {
    When("I click on the edit button")
    click on partialLinkText("Edit assignment")
    Then("I see the edit details screen")
    eventually(pageSource contains "Edit assignment details" should be (true))

    When("I choose Reusable workflow")
    singleSel("workflowCategory").value = WorkflowCategory.Reusable.code
    Then("I see the reusable workflow list")
    eventually(pageSource contains "Marking workflow type" should be (true))

    When("I select reusable workflow")
    val select = new Select(find(cssSelector("select[name=reusableWorkflow]")).get.underlying)
    select.selectByValue(workflowId)
    And("I submit the form")
    cssSelector(s"input[name=editAndEditDetails]").webElement.click()
    eventually(cssSelector(".confirm.btn-primary").webElement.isDisplayed shouldBe true)
    cssSelector(".confirm.btn-primary").webElement.click()

    Then("I should be back on the summary page")
    currentUrl should endWith("/summary")

  }

  def submitAssignment(user: LoginDetails, assignmentName: String, assignmentId: String, file: String, mustBeEnrolled: Boolean = true): Unit = as(user) {
    if (mustBeEnrolled) {
      val submitLink = eventually {
        id("main").webElement.findElement(By.xpath("//*[contains(text(),'" + assignmentName + "')]"))
          .findElement(By.xpath("../../../../div[contains(@class, 'item-info')]")).findElement(By.linkText("Submit assignment"))
      }
      click on submitLink

      currentUrl should endWith(assignmentId)
    } else {
      // Just go straight to the submission URL
      go to Path(s"/coursework/submission/$assignmentId")
    }

    find(cssSelector("input[type=file]")).get.underlying.sendKeys(getClass.getResource(file).getFile)

    val submitButton = id("main").webElement.findElement(By.cssSelector("div.submit-buttons .btn-primary"))
    click on submitButton
  }

  def getInputByLabel(label: String): Option[WebElement] =
    findAll(tagName("label")).find(_.underlying.getText.trim == label).map(_.underlying.getAttribute("for")) map {
      id(_).webElement
    }

  def loadCurrentAcademicYearTab(): Unit = {
    And("I click the current academic year tertiary nav bar")
    val tertiaryNavBar = find(cssSelector("nav.navbar.navbar-tertiary"))
    val tertiaryNavBarElement = tertiaryNavBar.get.underlying
    val currentYear = AcademicYear.now()
    click on tertiaryNavBarElement.findElement(By.partialLinkText(currentYear.getLabel))
  }

  def waitForAllAssignmentsToLoad(): Unit = {
    eventually(pageSource contains "Loading…" should be (false))
  }
}

