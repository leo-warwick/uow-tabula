package uk.ac.warwick.tabula.web.controllers.admin

import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class AdminController extends BaseController with AdminBreadcrumbs {

  hideDeletedItems

  final override def onPreRequest(): Unit = {

  }

}
