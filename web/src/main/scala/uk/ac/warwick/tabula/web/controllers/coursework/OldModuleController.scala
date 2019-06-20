package uk.ac.warwick.tabula.web.controllers.coursework

import uk.ac.warwick.tabula._
import data.model._
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import org.springframework.web.bind.annotation._
import org.springframework.stereotype._

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.web.Mav

class ViewModuleCommand(module: Module) extends ViewViewableCommand(Permissions.Module.ManageAssignments, module)

@Profile(Array("cm1Enabled"))
@Controller
@RequestMapping(Array("/${cm1.prefix}/module/{module}/"))
class OldModuleController extends OldCourseworkController {

  hideDeletedItems

  @ModelAttribute def command(@PathVariable module: Module) = new ViewModuleCommand(module)

  @RequestMapping
  def viewModule(@ModelAttribute cmd: ViewModuleCommand): Mav = {
    val module = cmd.apply()

    Mav("coursework/submit/module",
      "module" -> module,
      "assignments" -> module.assignments.asScala
        .filterNot(_.deleted)
        .sortBy(_.closeDate)
        .reverse)
  }

}
