package uk.ac.warwick.tabula

import java.io.{File, FileInputStream, IOException}
import java.util.{Base64, Properties}

import com.google.common.base.Charsets
import com.google.common.io.{ByteSource, Files}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.openqa.selenium.chrome.{ChromeDriver, ChromeDriverService, ChromeOptions}
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.remote.ScreenshotException
import org.openqa.selenium.support.events.{AbstractWebDriverEventListener, EventFiringWebDriver}
import org.openqa.selenium.{OutputType, TakesScreenshot, WebDriver, WebElement}
import org.scalatest._
import org.scalatest.concurrent.Eventually
import org.scalatest.exceptions.TestFailedException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span, SpanSugar}
import org.scalatestplus.junit._
import org.scalatestplus.selenium.WebBrowser
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.userlookup.UserLookup
import uk.ac.warwick.util.core.ExceptionUtils

import scala.util.{Success, Try}

/** Abstract base class for Selenium tests.
  *
  * The WebBrowser trait does all the Selenium magic, hooking into
  * the WebDriver we provide.
  */
@RunWith(classOf[JUnitRunner])
abstract class BrowserTest
  extends AnyFlatSpec
    with Matchers
    with BeforeAndAfter
    with SpanSugar
    with WebBrowser
    with WebsignonMethods
    with UserKnowledge
    with TestScreenshots
    with BeforeAndAfterAll {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(200, Millis))

  // Shorthand to expose properties to test classes
  val P: FunctionalTestProperties.type = FunctionalTestProperties

  /** Generates a full URL to browse to,
    * e.g. Path("/coursework") -> "https://tabula-test.warwick.ac.uk/coursework"
    */
  def Path(path: String): String = P.SiteRoot + path

  val screenshotDirectory = new File(P.ScreenshotDirectory)

  // Run at the end of this Suite (regular after() doesn't work because ScalaTest
  // reuses an instance for all tests, unlike JUnit which makes a new instance per test)
  override def afterAll(): Unit = {
    super.afterAll()
    webDriver.quit()
  }

  implicit lazy val webDriver: EventFiringWebDriver = {
    val driver = P.Browser match {
      case "chrome" =>
        val service = new ChromeDriverService.Builder()
          .usingDriverExecutable(new File(System.getProperty("webdriver.chrome.driver", P.ChromeDriverLocation)))
          .usingAnyFreePort()
          .withVerbose(true)
          .withSilent(false)
          .withLogFile(new File("chromedriver.log"))
          .build()

        val options = new ChromeOptions
        options.setBinary(new File(System.getProperty("chrome.binary.path", P.ChromeLocation)))
        options.setHeadless(true)

        new ChromeDriver(service, options)

      case "firefox" => new FirefoxDriver
    }

    // Set the most common screen resolution for Tabula
    // Set a large height so we don't have to deal with fixed footers occluding forms
    driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 3000))
    new EventFiringWebDriver(driver)
      .register(new DismissIntroductoryPopovers)
  }

  // Sometimes you need to wait for a page to load after clicking on a link
  def verifyPageLoaded(fun: => Unit): Unit = eventually(fun)

  // Don't set textField.value for a datetimepicker, as IT WILL HURT YOU
  class DateTimePickerField(val underlying: org.openqa.selenium.WebElement, selector: String) {
    if (!(underlying.getTagName.toLowerCase == "input" && underlying.getAttribute("type").toLowerCase == "text"))
      throw new TestFailedException(
        Some("Element " + underlying + ", specified as a dateTimePicker, is not a text field."),
        None,
        0
      )

    def value: String = underlying.getAttribute("value")

    def value_=(newValue: DateTime): AnyRef = {
      val nearestHourAsString = newValue.toString("dd-MMM-yyyy HH:00:00")
      do {
        underlying.clear()
        underlying.sendKeys(nearestHourAsString)
      } while (value != nearestHourAsString) // loop to fix weird Selenium failure mode where string is repeated
      val script = s"var dtp = jQuery('$selector').data('datetimepicker'); if (dtp!=undefined) dtp.update();"
      executeScript(script)
    }

    def clear(): Unit = underlying.clear()
  }

  def dateTimePicker(queryString: String): DateTimePickerField = {
    val (el, selector) = try {
      val el = IdQuery(queryString).webElement
      (el, s"#${el.getAttribute("id")}")
    } catch {
      case _: Throwable =>
        val el = NameQuery(queryString).webElement
        (el, s"[name=${el.getAttribute("name")}]")
    }
    new DateTimePickerField(el, selector)
  }
}

case class LoginDetails(usercode: String, password: String, description: String, warwickId: String)

/** Properties that can be overridden by a functionaltest.properties file in the classpath.
  *
  * Can also be overridden by a System property, which can be useful e.g. for running a similar
  * set of tests multiple times with a different browser. (Note that some properties cannot be
  * system properties, such as
  *
  * Defaults are in functionaltest-default.properties.
  */
object FunctionalTestProperties {
  private val properties = loadOptionalProps()

  // You can define .universityId properties in tabula-functionaltest.properties and this won't be used at all
  private lazy val userLookup = {
    System.setProperty("ssoclient.cache.strategy", "InMemoryOnly")
    val ul = new UserLookup
    // hardcode the service URLs; if they ever change, it's as
    // easy to change them here as it is in a properties file.
    ul.setSsosUrl("https://websignon.warwick.ac.uk")
    ul.setGroupServiceLocation("https://websignon.warwick.ac.uk")
    ul
  }

  val SiteRoot: String = prop("toplevel.url")
  val Browser: String = prop("browser")
  val ChromeLocation: String = prop("chrome.binary.path")
  val ChromeDriverLocation: String = prop("webdriver.chrome.driver")
  val ScreenshotDirectory: String = prop("screenshot.dir")

  /* Test user accounts who can sign in during tests. Populated from properties.
   * The tests currently REQUIRE that the user's first name is
   * equal to the usercode, since we look for "Signed in as X" to
   * determine whether we're signed in. Open to a better solution.
   */
  lazy val Admin1: LoginDetails = userDetails("admin1", "Departmental admin")
  lazy val Admin2: LoginDetails = userDetails("admin2", "Departmental admin")
  lazy val Admin3: LoginDetails = userDetails("admin3", "Departmental admin")
  lazy val Admin4: LoginDetails = userDetails("admin4", "Departmental admin")
  lazy val ExtensionManager1: LoginDetails = userDetails("extman1", "Extension manager")
  lazy val ExtensionManager2: LoginDetails = userDetails("extman2", "Extension manager")
  lazy val Marker1: LoginDetails = userDetails("marker1", "Marker")
  lazy val Marker2: LoginDetails = userDetails("marker2", "Marker")
  lazy val Marker3: LoginDetails = userDetails("marker3", "Marker")
  lazy val ModuleManager1: LoginDetails = userDetails("modman1", "Module Manager")
  lazy val ModuleManager2: LoginDetails = userDetails("modman2", "Module Manager")
  lazy val Student1: LoginDetails = userDetails("student1", "Student")
  lazy val Student2: LoginDetails = userDetails("student2", "Student")
  lazy val Student3: LoginDetails = userDetails("student3", "Student")
  lazy val Student4: LoginDetails = userDetails("student4", "Student")
  lazy val Student5: LoginDetails = userDetails("student5", "Student")
  lazy val Sysadmin: LoginDetails = userDetails("sysadmin", "System Administrator")

  /**
    * Get a property by name, or null if not found anywhere. Checks in this order
    * - System properties
    * - provided tabula-functionaltest.properties
    * - default properties
    */
  private def prop(name: String) =
    scala.util.Properties.propOrElse(name, fileProp(name))

  /** Like prop() but excludes system properties */
  private def fileProp(name: String) = properties.getProperty(name)

  private def loadOptionalProps() = {
    val file = new File(System.getProperty("user.home"), "tabula-functionaltest.properties")
    if (!file.exists()) {
      Assertions.fail("Properties file missing:  " + file)
    }
    val properties = new Properties
    properties.load(getClass.getResourceAsStream("/functionaltest-default.properties"))
    properties.load(new FileInputStream(file))
    properties
  }

  private def userDetails(identifier: String, description: String) = {
    val usercodeKey = s"user.$identifier.usercode"

    if (properties.containsKey(usercodeKey)) {
      val universityIdKey = s"user.$identifier.universityId"
      val warwickId =
        if (properties.containsKey(universityIdKey)) fileProp(universityIdKey)
        else Try(userLookup.getUserByUserId(fileProp(usercodeKey))) match {
          case Success(user) => user.getWarwickId
          case _ => "UNKNOWN"
        }

      LoginDetails(
        fileProp(usercodeKey),
        fileProp(s"user.$identifier.password"),
        description,
        warwickId
      )
    } else {
      Assertions.fail("Properties missing for " + description + " (user." + identifier + ".usercode)")
    }
  }
}

trait UserKnowledge {
  var currentUser: LoginDetails = _
}

trait TestScreenshots extends Logging {
  self: TestSuite =>

  implicit val webDriver: WebDriver // let the trait know this will be implemented
  val screenshotDirectory: File

  override def withFixture(test: NoArgTest): Outcome = {
    test() match {
      case failed: Failed =>
        val e = ExceptionUtils.retrieveException(failed.exception, classOf[ScreenshotException])

        val screenshot = Try(
          if (e != null)
            Some(Base64.getDecoder.decode(e.getBase64EncodedScreenshot.getBytes(Charsets.UTF_8)))
          else
            webDriver match {
              case d: TakesScreenshot => Some(d.getScreenshotAs(OutputType.BYTES))
              case _ => None
            }
        ).toOption.flatten

        screenshot.foreach { screenshot =>
          val outputFile = new File(screenshotDirectory, s"${test.name}.png")
          try {
            ByteSource.wrap(screenshot).copyTo(Files.asByteSink(outputFile))
            logger.info("Screenshot written to " + outputFile.getAbsolutePath)
          } catch {
            case ex: IOException =>
              logger.error("Couldn't write screenshot", ex)
          }
        }

        failed
      case other => other
    }
  }
}

class DismissIntroductoryPopovers extends AbstractWebDriverEventListener with WebBrowser with Eventually with Matchers {
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(200, Millis))

  private def dismissIntroductoryPopovers(implicit driver: WebDriver): Unit = {
    // If the introductory popover is visible, dismiss it
    if (cssSelector(".popover.introductory").findElement.exists(_.isDisplayed)) {
      click on cssSelector(".popover.introductory button.close")

      eventually {
        if (find(cssSelector(".popover.introductory")).nonEmpty)
          click on cssSelector(".popover.introductory button.close")

        find(cssSelector(".popover.introductory")) should be(None)
      }
    }
  }

  override def beforeClickOn(element: WebElement, driver: WebDriver): Unit =
    dismissIntroductoryPopovers(driver)

  override def afterNavigateTo(url: String, driver: WebDriver): Unit =
    dismissIntroductoryPopovers(driver)
}
