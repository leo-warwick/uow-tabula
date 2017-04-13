package uk.ac.warwick.tabula.coursework

import scala.collection.JavaConverters._
import org.joda.time.DateTime
import org.openqa.selenium.{By, WebElement}
import uk.ac.warwick.tabula.{BrowserTest, FunctionalTestAcademicYear, LoginDetails}
import uk.ac.warwick.tabula.web.{FeaturesDriver, FixturesDriver}
import org.scalatest.exceptions.TestFailedException
import org.scalatest.GivenWhenThen

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

trait CourseworkFixtures extends BrowserTest with FeaturesDriver with FixturesDriver with GivenWhenThen {

	val TEST_MODULE_CODE = "xxx02"
	val TEST_ROUTE_CODE="xx123"
	val TEST_DEPARTMENT_CODE="xxx"
	val TEST_COURSE_CODE="Ux123"

	before {
		Given("The test department exists")
		go to Path("/fixtures/setup")
		pageSource should include("Fixture setup successful")

		createPremarkedAssignment(TEST_MODULE_CODE)

		val assessmentFuture = Future {
			And("There is an assessment component for module xxx01")
			createAssessmentComponent("XXX", "XXX01-15", "Cool essay")

			And("There is an upstream assessment group for xxx01 with students1-4 in it")
			createUpstreamAssessmentGroup("XXX01-15", Seq(P.Student1.warwickId, P.Student2.warwickId, P.Student3.warwickId, P.Student4.warwickId))
		}

		And("student1-4 are members")
		createRoute(TEST_ROUTE_CODE, TEST_DEPARTMENT_CODE, "Test Route")
		createCourse(TEST_COURSE_CODE, "Test Course")
		createStudentMember(
			P.Student1.usercode,
			routeCode = TEST_ROUTE_CODE,
			courseCode = TEST_COURSE_CODE,
			deptCode = TEST_DEPARTMENT_CODE,
			academicYear = FunctionalTestAcademicYear.current.startYear.toString
		)
		// Make them at the same time.
		val concurrentJobs = Seq(
			assessmentFuture,
			Future { createStudentMember(
				P.Student1.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) },
			Future { createStudentMember(
				P.Student2.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) },
			Future { createStudentMember(
				P.Student3.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) },
			Future { createStudentMember(
				P.Student4.usercode,
				routeCode = TEST_ROUTE_CODE,
				courseCode = TEST_COURSE_CODE,
				deptCode = TEST_DEPARTMENT_CODE,
				academicYear = FunctionalTestAcademicYear.current.startYear.toString
			) }
		)

		// Wait to complete
		for { job <- concurrentJobs } Await.ready(job, Duration(60, duration.SECONDS))
	}


	def as[T](user: LoginDetails)(fn: => T): T = {
		currentUser = user
		signIn as user to Path("/coursework")

		fn
	}

	/* Runs callback with assignment ID */
	def withAssignment(
			moduleCode: String,
			assignmentName: String,
			settings: Seq[String] => Unit = allFeatures,
			members: Seq[String] = Seq(P.Student1.usercode, P.Student2.usercode),
			managers: Seq[String] = Seq(),
			assistants: Seq[String] = Seq())(callback: String => Unit): Unit = as(P.Admin1) {
		click on linkText("Go to the Test Services admin page")
		verifyPageLoaded {
			// wait for the page to load
			find(cssSelector("div.dept-show")) should be('defined)
		}

		click on linkText("Show")

		if ((assistants ++ managers).nonEmpty) {
			// Optionally add module assistants/managers if requested
			val info = getModuleInfo(moduleCode)
			click on info.findElement(By.className("module-manage-button")).findElement(By.partialLinkText("Manage"))

			val editPerms = info.findElement(By.partialLinkText("Edit module permissions"))
			eventually {
				editPerms.isDisplayed should be {true}
			}
			click on editPerms

			def pick(table: String, usercodes: Seq[String]) {
				verifyPageLoaded{
					find(cssSelector(s"$table .pickedUser")) should be ('defined)
				}
				usercodes.foreach { u =>
					click on cssSelector(s"$table .pickedUser")
					enter(u)
					val typeahead = cssSelector(".typeahead .active a")
					eventuallyAjax {
						find(typeahead) should not be None
					}
					click on typeahead
					find(cssSelector(s"$table form.add-permissions")).get.underlying.submit()
				}
			}

			pick(".modulemanager-table", managers)
			pick(".moduleassistant-table", assistants)

			// as you were...
			go to Path("/coursework")
			click on linkText("Go to the Test Services admin page")
			verifyPageLoaded{
				// wait for the page to load
				find(cssSelector("div.dept-show")) should be ('defined)
			}
			click on linkText("Show")
		}

		val info = getModuleInfo(moduleCode)
		click on info.findElement(By.className("module-manage-button")).findElement(By.partialLinkText("Manage"))

		val addAssignment = info.findElement(By.partialLinkText("Create new assignment"))
		eventually {
			addAssignment.isDisplayed should be {true}
		}

		click on addAssignment

		val prefilledReset = linkText("Don't do this")
		if (prefilledReset.findElement.isDefined) {
			click on prefilledReset
		}

		textField("name").value = assignmentName
		settings(members)

		submit()

		// Ensure that we've been redirected back
		withClue(pageSource) {
			currentUrl should endWith ("/department/xxx/#module-" + moduleCode.toLowerCase)
		}

		// NOTE: This assumes no duplicate assignment names!
		val assignmentInfo = getAssignmentInfo(moduleCode, assignmentName)

		val copyableUrl = assignmentInfo.findElement(By.className("linkForStudents")).getAttribute("href")
		val assignmentId = copyableUrl.substring(copyableUrl.lastIndexOf('/') + 1)

		callback(assignmentId)
	}

	def addSingleMarkingWorkflow()(callback: String => Unit): Unit = as(P.Admin1) {

		click on linkText("Go to the Test Services admin page")
		verifyPageLoaded {
			// wait for the page to load
			find(cssSelector("div.dept-show")) should be('defined)
		}

		When("I should be able to click on the Manage button")
		click on cssSelector(".dept-settings a.dropdown-toggle")

		Then("I should see the workflows menu option")
		val markingWorkflowsLink = cssSelector(".dept-settings .dropdown-menu").webElement.findElement(By.partialLinkText("Marking workflows"))
		eventually {
			markingWorkflowsLink.isDisplayed should be {true}
		}
		click on markingWorkflowsLink
		When("I click on the create workflows button")
		click on partialLinkText("Create")

		And("I enter the necessary data")
		textField("name").value = "First only workflow"
		singleSel("markingMethod").value = "FirstMarkerOnly"
		textField("firstMarkers").value = P.Marker1.usercode

		Then("Another marker field should magically appear")
		eventually {
			findAll(cssSelector("input.flexi-picker")).toList.count(_.isDisplayed) should be (2)
		}
		submit()

		Then("I should be redirected back to the marking workflow page")
		currentUrl should endWith ("/markingworkflows")

		val copyableUrl = find(xpath("(//a[contains(text(),'Delete')])[2]")).get.underlying.getAttribute("href")
		val workflowId = copyableUrl.substring(copyableUrl.lastIndexOf('/') + 1)



		callback(workflowId)
	}


	def submitAssignment(user: LoginDetails, moduleCode: String, assignmentName: String, assignmentId: String, file: String, mustBeEnrolled: Boolean = true): Unit = as(user) {
		if (mustBeEnrolled) {
			linkText(assignmentName).findElement should be ('defined)

			click on linkText(assignmentName)

			currentUrl should endWith(assignmentId + "/")
		} else {
			// Just go straight to the submission URL
			go to Path("/coursework/module/" + moduleCode.toLowerCase + "/" + assignmentId + "/")
		}

		// The assignment submission page uses FormFields which don't have readily memorable names, so we need to get fields by their label
		getInputByLabel("File") should be ('defined)

		click on getInputByLabel("File").orNull
		pressKeys(getClass.getResource(file).getFile)

		new TextField(getInputByLabel("Word count").orNull).value = "1000"

		checkbox("plagiarismDeclaration").select()

		submit()
		verifyPageLoaded(
			pageSource contains "Thanks, we've received your submission." should be {true}
		)
	}


	def requestExtension(user: LoginDetails, moduleCode: String, assignmentName: String, assignmentId: String, requestedDate: DateTime, mustBeEnrolled: Boolean = true): Unit = as(user) {
		if (mustBeEnrolled) {
			linkText(assignmentName).findElement should be ('defined)

			click on linkText(assignmentName)

			currentUrl should endWith(assignmentId + "/")
		} else {
			// Just go straight to the submission URL
			go to Path("/coursework/module/" + moduleCode.toLowerCase + "/" + assignmentId + "/")
		}

		click on partialLinkText("Request an extension")

		/**
			* Check that HTMLUnit has coped with the date picker JS correctly.
			* As of v2.0 it doesn't like calls to $(el).data() without arguments, so:
			*
			* 	if ('val' in $(el).data()) {...}
			*
			*	corrupts $(el) in the DOM! Instead, we locally change to:
			*
			* 	if ($(el).data('val') != undefined) {...}
			*
			* instead. Noted here in case of unexpected reversion.
			*
			*/
		textField("requestedExpiryDate").isDisplayed should be {true}

		// complete the form
		textArea("reason").value = "I have a desperate need for an extension."

		dateTimePicker("requestedExpiryDate").value = requestedDate

		checkbox("readGuidelines").select()

		submit()

		verifyPageLoaded(
			pageSource contains "You have requested an extension" should be {true}
		)
	}


	def allFeatures(members: Seq[String]) {
		// TODO Can't test link to SITS for our fixture department
		// Don't bother messing around with assigning students, let's just assume students will magically find the submit page
		click on id("student-summary-legend")
		className("show-adder").findElement map { _.underlying.isDisplayed } should be (Some(true))

		// Make sure JS is working
		id("js-hint").findElement should be ('empty)

		if (members.nonEmpty) {
			disableJQueryAnimationsOnHtmlUnit()

			click on linkText("Add students manually")
			eventually { textArea("massAddUsers").isDisplayed should be {true} }

			textArea("massAddUsers").value = members.mkString("\n")
			click on className("add-students")

			// Eventually, a Jax!
			eventuallyAjax { textArea("massAddUsers").isDisplayed should be {false} }
			// there will be a delay between the dialog being dismissed and the source being updated by the
			// ajax response. So wait some more
			eventuallyAjax{pageSource should include(members.size + " manually enrolled")}
		}

		checkbox("collectSubmissions").select()

		eventually {
			find("submission-options") map { _.isDisplayed } should be (Some(true))
		}

		// Turn everything on
		checkbox("collectMarks").select()
		checkbox("displayPlagiarismNotice").select()
		radioButtonGroup("restrictSubmissions").value = "true"
		checkbox("allowResubmission").select()
		checkbox("allowExtensions").select()

		// Type the file types in so that the javascript understands
		find("fileExtensionList").get.underlying.findElement(By.tagName("input")).sendKeys("docx txt pdf")

		textArea("assignmentComment").value =
			"""Hello my special friends.

			Here is another paragraph"""

		textField("wordCountMin").value = "1"
		textField("wordCountMax").value = "10000"

	}

	def getModuleInfo(moduleCode: String): WebElement = {
		val moduleInfoBlocks = findAll(className("module-info"))

		if (moduleInfoBlocks.isEmpty)
			throw new TestFailedException("No module-info blocks found on this page. Check it's the right page and that the user has permission.", 0)

		val matchingModule = moduleInfoBlocks.filter(_.underlying.findElement(By.className("mod-code")).getText == moduleCode.toUpperCase)

		if (matchingModule.isEmpty)
			throw new TestFailedException(s"No module-info found for ${moduleCode.toUpperCase}", 0)

		matchingModule.next().underlying
	}

	def getAssignmentInfo(moduleCode: String, assignmentName: String): WebElement = {
		val module = getModuleInfo(moduleCode)
		if (module.getAttribute("class").indexOf("collapsible") != -1 && module.getAttribute("class").indexOf("expanded") == -1) {
			click on module.findElement(By.className("section-title"))

			eventuallyAjax {
				module.getAttribute("class") should include ("expanded")
			}
		}

		val assignmentBlocks = module.findElements(By.className("assignment-info")).asScala

		if (assignmentBlocks.isEmpty)
			throw new TestFailedException("No assignment-info blocks found on this page. Check it's the right page and that the user has permission.", 0)

		val matchingAssignment = assignmentBlocks.filter(_.findElement(By.className("name")).getText.trim == assignmentName)

		if (matchingAssignment.isEmpty)
			throw new TestFailedException(s"No module-info found for $assignmentName", 0)

		matchingAssignment.head
	}

	def getInputByLabel(label: String): Option[WebElement] =
		findAll(tagName("label")).find(_.underlying.getText.trim == label) map { _.underlying.getAttribute("for") } map { id(_).webElement }

}