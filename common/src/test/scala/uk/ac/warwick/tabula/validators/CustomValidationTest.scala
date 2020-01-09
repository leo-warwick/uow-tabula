package uk.ac.warwick.tabula.validators

import javax.validation.Validation
import uk.ac.warwick.tabula.TestBase
import org.junit.Test


class CustomValidationTest extends TestBase {

  @Test def validationConstraints: Unit = {
    val factory = Validation.buildDefaultValidatorFactory
    val validator = factory.getValidator

    validator.validate(new TestValidScalaObject("")) should not be (Symbol("empty"))
    validator.validate(new TestValidScalaObject("a")) should be(Symbol("empty"))
  }

}