package uk.ac.warwick.tabula.system

import javax.servlet.ServletRequest

/** Mixin binding for commands that implement the BindListener trait.
  * This will call onBind after any regular binding, to allow the command
  * to do custom post-binding code.
  */
trait BindListenerBinding extends CustomDataBinder {

  override def bind(request: ServletRequest): Unit = {
    super.bind(request)

    // Custom onBind methods
    if (target.isInstanceOf[BindListener]) {
      target.asInstanceOf[BindListener].onBind(super.getBindingResult)
    }
  }

}
