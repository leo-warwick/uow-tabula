package uk.ac.warwick.tabula

import java.io.{File, InputStream, StringReader}
import java.util
import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.ObjectMapper
import freemarker.cache.{ClassTemplateLoader, MultiTemplateLoader}
import freemarker.core.{Environment, HTMLOutputFormat}
import freemarker.template._
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.log4j.NDC
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.BlobStoreContext
import org.joda.time.{DateTime, DateTimeUtils, ReadableInstant}
import org.junit.rules.Timeout
import org.junit.{After, Before, Rule}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.junit.{AssertionsForJUnit, JUnitSuite}
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.{MockHttpServletRequest, MockHttpServletResponse}
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.Transactions
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.objectstore.{BlobStoreObjectStorageService, ObjectStorageService}
import uk.ac.warwick.tabula.web.views.ScalaFreemarkerConfiguration
import uk.ac.warwick.userlookup.{AnonymousUser, User}
import uk.ac.warwick.util.core.spring.FileUtils
import uk.ac.warwick.util.web.Uri

import scala.collection.parallel.CollectionConverters._
import scala.jdk.CollectionConverters._

/** Base class for tests which boringly uses the JUnit support of
 * Scalatest, so you do @Test annotated methods as you normally would.
 * You can use ScalaTest's "should" matchers though, which is nice.
 *
 * Also a bunch of methods for generating fake support resources.
 */
abstract class TestBase extends JUnitSuite with Matchers with ScalaFutures with AssertionsForJUnit with TestHelpers with TestFixtures with Logging {
  // bring in type so we can be lazy and not have to import @Test
  type Test = org.junit.Test

  // No test should take longer than 2 minutes
  val minuteTimeout = new Timeout(2, TimeUnit.MINUTES)

  @Rule def timeoutRule: Timeout = minuteTimeout

  @After
  def tearDownTestLoggers(): Unit = {
    TestLoggerFactory.tearDown()
  }

  // Give Scala Futures time to run
  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(50, Millis))

  Transactions.enabled = false
  EventHandling.enabled = false

  // IntelliJ tests via JUnit only half-fill this property, so set it here.
  if (System.getProperty("TestProcessId") == "F${surefire.forkNumber}") {
    System.setProperty("TestProcessId", "F1")
  }

  NDC.pop()
  NDC.push(System.getProperty("TestProcessId"))
  logger.trace("TestBase instantiated for " + this.getClass.getName)
}

/** Various test objects
 */
trait TestFixtures {
  def newFreemarkerConfiguration(): ScalaFreemarkerConfiguration = newFreemarkerConfiguration(JHashMap())


  def newFreemarkerConfiguration(sharedVariables: JMap[String, Any]) = new ScalaFreemarkerConfiguration {
    setTemplateLoader(new MultiTemplateLoader(Array(
      new ClassTemplateLoader(getClass, "/freemarker/"), // to match test templates
      new ClassTemplateLoader(getClass, "/") // to match live templates
    )))
    setAutoIncludes(List("WEB-INF/freemarker/prelude.ftl").asJava)
    setSharedVariables(sharedVariables)
  }

  def testRequest(uri: String = null): MockHttpServletRequest = {
    val req = new MockHttpServletRequest
    req.setRequestURI(uri)
    req
  }

  def emptyFeatures: FeaturesImpl = Features.empty

  /** Creates an Assignment with a module and department,
   * and a few pre-filled fields.
   */
  def newDeepAssignment(moduleCode: String = "IN101"): Assignment = {
    val department = new Department
    department.isImportDepartment = true
    val module = new Module(moduleCode, department)
    val assignment = new Assignment(module)
    assignment.setDefaultBooleanProperties()
    assignment
  }

  def testResponse = new MockHttpServletResponse

  /** Returns midnight on the first day of this year and month. */
  def dateTime(year: Int, month: Int): DateTime = dateTime(year, month, 1)

  def dateTime(year: Int, month: Int, day: Int): DateTime = new DateTime(year, month, day, 0, 0, 0, 0)

  def newSSOConfiguration: SSOConfiguration = {
    val config = new PropertiesConfiguration()
    config.addProperty("mode", "new")
    config.addProperty("origin.login.location", "https://xebsignon.warwick.ac.uk/origin/hs")
    config.addProperty("shire.location", "https://xabula.warwick.ac.uk/tabula/shire")
    config.addProperty("shire.providerid", "tabula:service")
    config.addProperty("ssoclient.cache.strategy", "InMemoryOnly")

    new SSOConfiguration(config)
  }
}

trait TestHelpers extends TestFixtures {
  lazy val json: ObjectMapper = new JsonObjectMapperFactory().createInstance

  def readJsonMap(s: String): Map[String, Any] = json.readValue(new StringReader(s), classOf[JMap[String, Any]]).asScala.toMap

  var currentUser: CurrentUser = null

  var temporaryFiles: Set[File] = Set.empty

  var blobStoreContext: BlobStoreContext = _

  // Location of /tmp - best to create a subdir below it.
  lazy val IoTmpDir = new File(System.getProperty("java.io.tmpdir"))
  val random = new scala.util.Random

  @Before def emptyTempDirSet: Unit = temporaryFiles = Set.empty

  @Before def setupAspects: Unit = {

  }

  def createTransientObjectStore(): ObjectStorageService = {
    if (blobStoreContext == null)
      blobStoreContext = ContextBuilder.newBuilder("transient").buildView(classOf[BlobStoreContext])

    val blobStore = new BlobStoreObjectStorageService(blobStoreContext, "JavaTestTmp-" + random.nextLong())
    blobStore.afterPropertiesSet()
    blobStore
  }

  /** Returns a new temporary directory that will get cleaned up
   * automatically at the end of the test.
   */
  def createTemporaryDirectory(): File = {
    // try 10 times to find an unused filename.
    // Stream is lazy so it won't try making 10 files every time.
    val dir = findTempFile
    if (!dir.mkdir()) throw new IllegalStateException("Couldn't create " + dir)
    temporaryFiles += dir
    dir
  }

  def createTemporaryFile(): File = {
    val file = findTempFile
    if (!file.createNewFile()) throw new IllegalStateException("Couldn't create " + file)
    temporaryFiles += file
    file
  }

  private def findTempFile: File = {
    def randomTempFile() = new File(IoTmpDir, "JavaTestTmp-" + random.nextLong())

    // Create a Stream that will generate random files forever, then take the first 10.
    // The Iterator will only calculate its elements on demand so it won't always generate 10 Files.
    Iterator.continually(randomTempFile()).take(10)
      .find(!_.exists)
      .getOrElse(throw new IllegalStateException("Couldn't find unique filename!"))
  }

  /** Removes any directories created by #createTemporaryDirectory
   */
  @After def deleteTemporaryDirs: Unit = try {
    temporaryFiles.par foreach FileUtils.recursiveDelete
    if (blobStoreContext != null) blobStoreContext.close()
  } catch {
    case _: Throwable => /* squash! will be cleaned from temp eventually anyway */
  }

  def withFakeTime(when: ReadableInstant)(fn: => Unit): Unit =
    try {
      DateTimeUtils.setCurrentMillisFixed(when.getMillis)
      uk.ac.warwick.util.core.DateTimeUtils.useMockDateTime(java.time.Instant.ofEpochMilli(when.getMillis), new Runnable {
        override def run(): Unit = fn
      })
    } finally {
      DateTimeUtils.setCurrentMillisSystem()
    }

  /** Sets up a pretend requestinfo context with the given pretend user
   * around the callback.
   *
   * Can pass null as the usercode to make an anonymous user.
   *
   * withUser("cusebr") { /* ... your code */  }
   */
  def withUser(code: String, universityId: String = null, profile: Option[Member] = None)(fn: => Unit): Unit = {
    val user = if (code == null) {
      new AnonymousUser()
    } else {
      val u = new User(code)
      u.setIsLoggedIn(true)
      u.setFoundUser(true)
      u.setWarwickId(universityId)
      u
    }

    withCurrentUser(new CurrentUser(user, user, profile))(fn)
  }

  def clearRequestLevelCache(): Unit =
    RequestInfo.fromThread
      .getOrElse(throw new IllegalStateException("No RequestInfo active"))
      .requestLevelCache.shutdown() // withUser maintains a cache

  def withCurrentUser(user: CurrentUser)(fn: => Unit): Unit = {
    val requestInfo = RequestInfo.fromThread match {
      case Some(info) => throw new IllegalStateException("A RequestInfo is already open")
      case None => {
        new RequestInfo(user, Uri.parse("http://www.example.com/page"), Map())
      }
    }

    try {
      currentUser = user
      RequestInfo.open(requestInfo)
      fn
    } finally {
      currentUser = user
      RequestInfo.close()
    }
  }

  def withSSOConfig(ssoConfig: SSOConfiguration = newSSOConfiguration)(fn: => Unit): Unit = {
    try {
      SSOConfiguration.setConfig(ssoConfig)
      fn
    } finally {
      SSOConfiguration.setConfig(null)
    }
  }

  /** Fetches a resource as a string. Assumes UTF-8 unless specified.
   */
  def resourceAsString(path: String, encoding: String = "UTF-8"): String = new String(resourceAsBytes(path), encoding)

  def resourceAsBytes(path: String): Array[Byte] = FileCopyUtils.copyToByteArray(new ClassPathResource(path).getInputStream)

  def resourceAsStream(path: String): InputStream = new ClassPathResource(path).getInputStream

  /**
   * custom matcher to let you write
   *
   * myObject should be ( anInstanceOf[MyType] )
   *
   * See https://groups.google.com/forum/#!topic/scalatest-users/UrdRM6XHB4Y
   */
  def anInstanceOf[T](implicit manifest: Manifest[T]): BePropertyMatcher[AnyRef] = {
    val clazz = manifest.runtimeClass.asInstanceOf[Class[T]]
    new BePropertyMatcher[AnyRef] {
      def apply(left: AnyRef) =
        BePropertyMatchResult(clazz.isAssignableFrom(left.getClass), "an instance of " + clazz.getName)
    }
  }
}

trait FreemarkerTestHelpers {

  class StubFreemarkerMethodModel extends TemplateMethodModelEx with Mockito {
    val mock: TemplateMethodModelEx = mock[TemplateMethodModelEx]

    def exec(arguments: util.List[_]): AnyRef = Option(mock.exec(arguments)).getOrElse(HTMLOutputFormat.INSTANCE.fromMarkup(""))
  }

  class StubFreemarkerDirectiveModel extends TemplateDirectiveModel with TemplateMethodModelEx with Mockito {
    val mockMethod: TemplateMethodModelEx = mock[TemplateMethodModelEx]
    val mockDirective: TemplateDirectiveModel = mock[TemplateDirectiveModel]

    def execute(env: Environment, params: util.Map[_, _], loopVars: Array[TemplateModel], body: TemplateDirectiveBody): Unit = {
      mockDirective.execute(env, params, loopVars, body)
    }

    def exec(arguments: util.List[_]): AnyRef = Option(mockMethod.exec(arguments)).getOrElse(HTMLOutputFormat.INSTANCE.fromMarkup(""))
  }

}
