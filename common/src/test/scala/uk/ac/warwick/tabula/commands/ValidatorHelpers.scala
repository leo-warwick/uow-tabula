package uk.ac.warwick.tabula.commands

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.{Mockito, TestBase}


trait ValidatorHelpers {

	this: TestBase with Mockito =>

	def hasError(validator: SelfValidating, error: String) = testValidator(validator, error, hasError=true)

	def hasNoError(validator: SelfValidating, noError: String) = testValidator(validator, noError, hasError=false)

	def testValidator(validator: SelfValidating, field: String, hasError: Boolean): Unit = {
		val errors = new BindException(validator, "command")
		validator.validate(errors)
		errors.hasFieldErrors(field) should be {hasError}
	}

}
