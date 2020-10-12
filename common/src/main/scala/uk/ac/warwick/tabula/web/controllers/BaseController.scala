package uk.ac.warwick.tabula.web.controllers

import java.net.URI

import javax.annotation.Resource
import org.springframework.beans.factory.annotation.{Autowired, Required}
import org.springframework.context.MessageSource
import org.springframework.stereotype.Controller
import org.springframework.validation.Validator
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{Logging, StringUtils}
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods}
import uk.ac.warwick.tabula.validators.CompositeValidator
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException, PermissionDeniedException, RequestInfo}

import scala.util.Try

trait ControllerMethods extends PermissionsCheckingMethods with Logging {
  def user: CurrentUser

  var securityService: SecurityService

  def restricted[A <: PermissionsChecking](something: => A): Option[A] =
    try {
      permittedByChecks(securityService, user, something)
      Some(something)
    } catch {
      case _@(_: ItemNotFoundException | _: PermissionDeniedException) => None
    }

  def restrictedBy[A <: PermissionsChecking](fn: => Boolean)(something: => A): Option[A] =
    if (fn) restricted(something)
    else Some(something)
}

trait ControllerViews extends Logging {
  val Mav = uk.ac.warwick.tabula.web.Mav

  def getReturnTo(defaultUrl: String): String = {
    Try {
      val uri = new URI(getReturnToUnescaped(defaultUrl))

      implicit def nonEmpty(s: String): Option[String] = Option(s).filterNot(_ == "").filterNot(_ == null)

      val qs: Option[String] = uri.getQuery
      val path: Option[String] = uri.getRawPath

      path.map(_ + qs.map("?" + _).getOrElse(""))
    }.toOption.flatten.getOrElse("")
  }

  def getReturnToUnescaped(defaultUrl: String): String =
    requestInfo.flatMap(_.requestParameters.get("returnTo")).flatMap(_.headOption).filter(_.hasText).fold({
      if (defaultUrl.isEmpty)
        logger.warn("Empty defaultUrl when using returnTo")
      defaultUrl
    })(url =>
      // Prevent returnTo rabbit hole by stripping other returnTos from the URL
      url.replaceAll("[&?]returnTo=[^&]*", "")
    )

  def Redirect(path: String, objects: (String, _)*): Mav = Redirect(path, Map(objects: _*))

  def Redirect(path: String, objects: Map[String, _]): Mav = Mav("redirect:" + validRedirectDestination(getReturnToUnescaped(path)), objects)

  def RedirectFlashing(path: String, flash: (String, String)*)(implicit redirectAttributes: RedirectAttributes): String = {
    flash.foreach { case (k, v) => redirectAttributes.addFlashAttribute(k, v) }
    s"redirect:${validRedirectDestination(getReturnToUnescaped(path))}"
  }

  def RedirectForceFlashing(path: String, flash: (String, String)*)(implicit redirectAttributes: RedirectAttributes): String = {
    flash.foreach { case (k, v) => redirectAttributes.addFlashAttribute(k, v) }
    s"redirect:${validRedirectDestination(path)}"
  }

  // Force the redirect regardless of returnTo
  def RedirectForce(path: String, objects: (String, _)*): Mav = RedirectForce(path, Map(objects: _*))

  def RedirectForce(path: String, objects: Map[String, _]): Mav = Mav("redirect:" + validRedirectDestination(path), objects)

  private def validRedirectDestination(dest: String): String = Option(dest).filter(d => d.startsWith("/") || d.startsWith(s"${currentUri.getScheme}://${currentUri.getAuthority}/") || d == loginUrl).getOrElse("/")

  def RedirectToSignin(target: String = loginUrl): Mav = Redirect(target)

  private def currentUri = requestInfo.get.requestedUri

  private def currentPath: String = currentUri.getPath

  def loginUrl: String = {
    val generator = new SSOLoginLinkGenerator
    generator.setConfig(SSOConfiguration.getConfig)
    generator.setTarget(currentUri.toString)
    generator.getLoginUrl
  }

  def requestInfo: Option[RequestInfo]

}

trait ControllerImports {

  import org.springframework.web.bind.annotation.RequestMethod

  final val GET = RequestMethod.GET
  final val PUT = RequestMethod.PUT
  final val HEAD = RequestMethod.HEAD
  final val POST = RequestMethod.POST
  final val DELETE = RequestMethod.DELETE
  final val PATCH = RequestMethod.PATCH

  type RequestMapping = org.springframework.web.bind.annotation.RequestMapping
}

trait PreRequestHandler {
  def preRequest: Unit
}

trait MessageResolver {
  /**
    * Resolve a message from messages.properties. This is the same way that
    * validation error codes are resolved.
    */
  def getMessage(key: String, args: Object*): String
}

/**
  * Useful traits for all controllers to have.
  */
@Controller
abstract class BaseController extends ControllerMethods
  with ControllerViews
  with ValidatesCommand
  with Logging
  with EventHandling
  with Daoisms
  with StringUtils
  with ControllerImports
  with PreRequestHandler
  with MessageResolver {

  @Required
  @Resource(name = "validator") var globalValidator: Validator = _

  @Autowired
  var securityService: SecurityService = _

  @Autowired private var messageSource: MessageSource = _

  /**
    * Resolve a message from messages.properties. This is the same way that
    * validation error codes are resolved.
    */
  def getMessage(key: String, args: Object*): String = messageSource.getMessage(key, args.toArray, null)

  var disallowedFields: List[String] = Nil

  def requestInfo: Option[RequestInfo] = RequestInfo.fromThread

  def user: CurrentUser = requestInfo.get.user

  def ajax: Boolean = requestInfo.exists(_.ajax)

  /**
    * Enables the Hibernate filter for this session to exclude
    * entities marked as deleted.
    */
  private var _hideDeletedItems = false

  def hideDeletedItems: Unit = {
    _hideDeletedItems = true
  }

  def showDeletedItems: Unit = {
    _hideDeletedItems = false
  }

  final def preRequest(): Unit = {
    // if hideDeletedItems has been called, exclude all "deleted=1" items from Hib queries.
    if (_hideDeletedItems) {
      session.enableFilter("notDeleted")
    }
    onPreRequest()
  }

  // Stub implementation that can be overridden for logic that goes before a request
  def onPreRequest(): Unit = {}

  /**
    * Sets up @Valid validation.
    * If "validator" has been set, it will be used. If "keepOriginalValidator" is true,
    * it will be joined up with the default global validator (the one that does annotation based
    * validation like @NotEmpty). Otherwise it's replaced.
    *
    * Sets up disallowedFields.
    */
  @InitBinder final def _binding(binder: WebDataBinder): Unit = {
    if (validator != null) {
      if (keepOriginalValidator) {
        val original = binder.getValidator
        binder.setValidator(new CompositeValidator(validator, original))
      } else {
        binder.setValidator(validator)
      }
    }
    binder.setDisallowedFields(disallowedFields: _*)
    binding(binder, binder.getTarget)
  }

  /**
    * Do any custom binding init by overriding this method.
    */
  def binding[A](binder: WebDataBinder, target: A): Unit = {}

}
