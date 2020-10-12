package uk.ac.warwick.tabula.validators

import javax.validation.constraints.NotEmpty

class TestValidScalaObject {
  def this(n: String) = {
    this()
    name = n
  }

  @NotEmpty
  var name: String = _
}

