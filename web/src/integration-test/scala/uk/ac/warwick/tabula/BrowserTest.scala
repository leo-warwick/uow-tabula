package uk.ac.warwick.tabula

import org.scalatest._
import org.scalatest.exceptions.TestFailedException
import org.scalatest.junit._
import org.scalatest.selenium.WebBrowser
import org.junit.runner.RunWith
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.WebDriver
import java.util.Properties
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.phantomjs.PhantomJSDriver
import java.io.File
import java.io.FileInputStream
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar
import com.gargoylesoftware.htmlunit.BrowserVersion
import uk.ac.warwick.userlookup.UserLookup
import scala.util.Try
import scala.util.Success
import org.joda.time.DateTime

/** Abstract base class for Selenium tests.
  *
  * The WebBrowser trait does all the Selenium magic, hooking into
  * the WebDriver we provide.
  */
@RunWith(classOf[JUnitRunner])
abstract class BrowserTest
	extends FlatSpec
	with ShouldMatchers
	with BeforeAndAfter
	with EventuallyAjax
	with SpanSugar
	with WebBrowser
	with WebsignonMethods
	with UserKnowledge {

	// Shorthand to expose properties to test classes
	val P = FunctionalTestProperties

	/** Generates a full URL to browse to,
	  * e.g. Path("/coursework") -> "https://tabula-test.warwick.ac.uk/coursework"
	  */
	def Path(path: String): String = P.SiteRoot + path

	implicit lazy val webDriver: WebDriver = P.Browser match {
		case "htmlunit" => //new HtmlUnitDriver(true) // JS enabled
			val driver = new HtmlUnitDriver(htmlUnitBrowserVersion) // JS enabled
			driver.setJavascriptEnabled(true)
			driver
		case "chrome" => new ChromeDriver
		case "firefox" => new FirefoxDriver
		case "ie" => new InternetExplorerDriver
		case "phantomjs" => new PhantomJSDriver
	}

	// Can be overridden by a test if necessary.
	val htmlUnitBrowserVersion = BrowserVersion.BEST_SUPPORTED

	def ifHtmlUnitDriver(operation:HtmlUnitDriver=>Unit): Unit = {
		webDriver match {
			case h:HtmlUnitDriver=>operation(h)
			case _=> // do nothing
		}
	}

	def disableJQueryAnimationsOnHtmlUnit() {
		ifHtmlUnitDriver { driver =>
			executeScript("jQuery.support.transition = false")
		}
	}

	// Sometimes you need to wait for a page to load after clicking on a link
	def verifyPageLoaded(fun: => Unit): Unit = eventuallyAjax(fun)

	// Don't set textField.value for a datetimepicker, as IT WILL HURT YOU
	class DateTimePickerField(val underlying: org.openqa.selenium.WebElement, selector: String) {
		if(!(underlying.getTagName.toLowerCase == "input" && underlying.getAttribute("type").toLowerCase == "text"))
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

case class LoginDetails(usercode: String, password: String, description: String, warwickId:String)

trait EventuallyAjax extends Eventually with SpanSugar {
	/**
	 * eventually{} is a generic ScalaTest method to repeatedly
	 * try a block of code until it works or we give up. eventuallyAjax {}
	 * just calls that with some sensible default timeouts.
	 */
	def eventuallyAjax(fun: =>Unit) {
		eventually(timeout(30.seconds), interval(200.millis)) (fun)
	}
}

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

	private val userLookup = new UserLookup
	// hardcode the service URLs; if they ever change, it's as
	// easy to change them here as it is in a properties file.
	userLookup.setSsosUrl("https://websignon.warwick.ac.uk")
	userLookup.setGroupServiceLocation("https://websignon.warwick.ac.uk")

	val SiteRoot: String = prop("toplevel.url")
	val Browser: String = prop("browser")

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
		val usercodeKey = "user." + identifier + ".usercode"

		if (properties.containsKey(usercodeKey)) {
			val warwickId = Try(userLookup.getUserByUserId(fileProp(usercodeKey))) match {
				case Success(user)=>user.getWarwickId
				case _=>"UNKNOWN"
			}

			LoginDetails(
				fileProp(usercodeKey),
				fileProp("user." + identifier + ".password"),
				description,
				warwickId
			)
		} else {
			Assertions.fail("Properties missing for "+description+" (user."+identifier+".usercode)")
		}
	}
}

trait UserKnowledge {
	var currentUser: LoginDetails = _
}
