package uk.ac.warwick.tabula.api.web.filters

import javax.servlet.FilterChain
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

import org.springframework.http.HttpStatus
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.sso.client.trusted.TrustedApplication
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.HttpServletRequestUtils._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.exceptions.UserError
import uk.ac.warwick.util.web.filter.AbstractHttpFilter

class ForceAPILoginMethodFilter extends AbstractHttpFilter with Logging {

	override def doFilter(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
		// Replaces ForceLoginFilter
		if (!SSOClientFilter.getUserFromRequest(request).isFoundUser) throw new APILoginMethodMissingException()

		request.getHeader("Authorization") match {
			// Basic Auth request
			case basicAuthHeader if basicAuthHeader.safeStartsWith("Basic ") =>
				chain.doFilter(request, response)

			// OAuth request
			case oauthHeader if oauthHeader.safeStartsWith("OAuth ") =>
				chain.doFilter(request, response)

			// Trusted Apps request
			case _ if request.getHeader(TrustedApplication.HEADER_CERTIFICATE).hasText =>
				chain.doFilter(request, response)

			// Ajax (GET only, don't enable it for anything else without understanding the CSRF implications)
			case _ if request.isAjaxRequest && request.getMethod.toUpperCase == "GET" =>
				chain.doFilter(request, response)

			case _ => throw new APILoginMethodMissingException()
		}
	}

}

class APILoginMethodMissingException extends java.lang.RuntimeException("API requests must be authenticated with HTTP Basic Auth or OAuth") with UserError {
	override val httpStatus = HttpStatus.UNAUTHORIZED
}
