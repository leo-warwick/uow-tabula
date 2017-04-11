package uk.ac.warwick.tabula.web.controllers
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import uk.ac.warwick.tabula.validators.ClassValidator
import uk.ac.warwick.tabula.commands.SelfValidating
import scala.reflect.ClassTag

/**
 * Methods for setting custom validator stuff.
 *
 * Note that on its own, this just sets a validator property but doesn't
 * configure any validation to happen automatically. It's best to use this
 * by extending BaseController instead, as that sets up the data binder to
 * use this validator for commands marked with @Valid.
 */
trait ValidatesCommand {

	var validator: Validator = _

	/**
	 * When specifying a Validator for this controller, whether to
	 * keep the existing wired validator (which should be the globally autowired
	 * one handling annotation-based validation).
	 */
	protected var keepOriginalValidator: Boolean = true

	type ValidatorMethod[A] = (A, Errors) => Unit

	private def _validatesWith[A : ClassTag](fn: ValidatorMethod[A]) {
		if (validator != null) throw new IllegalStateException("Already set validator once")
		validator = new ClassValidator[A] {
			override def valid(target: A, errors: Errors): Unit = fn(target, errors)
		}
	}

	/**
	 * If the command object implements SelfValidating, this will
	 * run its validation command when a @Valid object is requested.
	 */
	def validatesSelf[A <: SelfValidating : ClassTag] {
		_validatesWith[A] { (cmd, errors) => cmd.validate(errors) }
	}

}