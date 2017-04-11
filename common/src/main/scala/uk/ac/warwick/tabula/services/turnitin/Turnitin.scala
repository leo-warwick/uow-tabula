package uk.ac.warwick.tabula.services.turnitin

import java.io.InputStream

import dispatch.classic.Request.toRequestVerbs
import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.impl.client.DefaultRedirectStrategy
import org.apache.http.params.HttpConnectionParams
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpResponse}
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.{DisposableBean, InitializingBean}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.helpers.Logging

case class FileData(data: InputStream, name: String)

object Turnitin {
	/**
		* ID that we should store classes under. They are per-module so we base it on the module code.
		* This ID is stored within Turnitin and requests for the same ID should return the same class.
		*/
	def classIdFor(assignment: Assignment, prefix: String) = ClassId(prefix + "-" + assignment.module.code)

	/**
		* ID that we should store assignments under. Our assignment ID is as good an identifier as any.
		* This ID is stored within Turnitin and requests for the same ID should return the same assignment.
		*/
	def assignmentIdFor(assignment: Assignment) = AssignmentId("Assignment-" + assignment.id)

	def classNameFor(assignment: Assignment): ClassName = {
		val module = assignment.module
		ClassName(module.code.toUpperCase + " - " + module.name)
	}

	def assignmentNameFor(assignment: Assignment): AssignmentName = {
		AssignmentName(assignment.name + " (" + assignment.academicYear.toString + ")")
	}
}

/**
 * Service for accessing the Turnitin plagiarism API.
 *
 * You call login() first with a user's details, which if successful will give you a
 * Session that has methods that will be called as that user. When done you can call
 * logout() to end the session.
 */
@Service
class Turnitin extends Logging with DisposableBean with InitializingBean {


	/** The top level account ID (usually for University of Warwick account) */
	@Value("${turnitin.aid}") var aid: String = null
	/** Sub-account ID underneath University of Warwick */
	@Value("${turnitin.said}") var said: String = null
	/** Shared key as set up on the University of Warwick account's Open API settings */
	@Value("${turnitin.key}") var sharedSecretKey: String = null

	@Value("${turnitin.url}") var apiEndpoint: String = _

	// Warwick's API version
	@Value("${turnitin.integration}") var integrationId: String = _

	@Value("${turnitin.class.prefix}") var classPrefix: String =_

	/**
	 * If this is set to true, responses are returned with HTML debug info,
	 * and also it doesn't make any changes - the server just lets you know whether
	 * your request looks okay.
	 */
	var diagnostic = false

	val userAgent = "Coursework submission app, University of Warwick, coursework@warwick.ac.uk"

	// URL to call for all requests.
	lazy val endpoint: Request = url(apiEndpoint) <:< Map("User-Agent" -> userAgent)

	// TODO does this really need to be a custom instance?
	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
			HttpConnectionParams.setConnectionTimeout(getParams, 20000)
			HttpConnectionParams.setSoTimeout(getParams, 20000)
			setRedirectStrategy(new DefaultRedirectStrategy {
				override def isRedirected(req: HttpRequest, res: HttpResponse, ctx: HttpContext) = false
			})
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy {
		http.shutdown()
	}

	override def afterPropertiesSet {

	}

	def login(email:String, firstName:String, lastName:String): Option[Session] = {
		val session = new Session(this, null)
		val userEmail = if (email == null || email.isEmpty) firstName + lastName + "@turnitin.warwick.ac.uk" else email

		session.userEmail = userEmail
		session.userFirstName = firstName
		session.userLastName = lastName
		session.login() match {
			case Created(sessionId) if sessionId != "" =>
				val session = new Session(this, sessionId)
				session.userEmail = userEmail
				session.userFirstName = firstName
				session.userLastName = lastName
				session.acquireUserId()
				Some(session)
			case _ => None
		}
	}

}