package uk.ac.warwick.tabula.web.controllers.groups

import uk.ac.warwick.tabula.web.controllers.BaseController

abstract class GroupsController extends BaseController with GroupsBreadcrumbs {

  hideDeletedItems

  final override def onPreRequest(): Unit = {

  }

}
