package uk.ac.warwick.tabula.web.controllers

trait TestControllerOverrides extends ControllerViewsOverrides { self: BaseController =>

}

trait ControllerViewsOverrides { self: ControllerViews =>
	override def loginUrl = "http://sso.example.com/login"
}