package uk.ac.warwick.tabula.validators
import annotation.target.field
import org.hibernate.validator.constraints.NotEmpty

class TestValidScalaObject {
  def this(n:String) {
    this()
    name = n
  }
  
  @NotEmpty
  var name:String =_
}
    
