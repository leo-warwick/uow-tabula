package uk.ac.warwick.tabula.web.controllers.admin.modules

import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.admin.AdminBreadcrumbs
import uk.ac.warwick.tabula.{Fixtures, ItemNotFoundException, Mockito, TestBase}
import uk.ac.warwick.tabula.commands.admin.modules.SortModulesCommandState
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.commands.{Appliable, GroupsObjects}
import org.springframework.validation.BindException

class SortModulesControllerTest extends TestBase with Mockito {

  val controller = new SortModulesController

  @Test def createsCommand(): Unit = {
    val department = Fixtures.department("in")

    val command = controller.command(department)

    command should be(anInstanceOf[Appliable[Unit]])
  }

  @Test(expected = classOf[ItemNotFoundException]) def requiresDepartment(): Unit = {
    controller.command(null)
  }

  class CountingCommand(val department: Department) extends Appliable[Unit] with GroupsObjects[Module, Department] with SortModulesCommandState {
    var populateCount = 0
    var sortCount = 0
    var applyCount = 0

    def populate(): Unit = {
      populateCount += 1
    }

    def sort(): Unit = {
      sortCount += 1
    }

    def apply(): Unit = {
      applyCount += 1
    }
  }

  trait Fixture {
    val department: Department = Fixtures.department("in")
    val subDepartment: Department = Fixtures.department("in-ug")
    department.children.add(subDepartment)
    subDepartment.parent = department
  }

  @Test def formOnParent(): Unit = {
    new Fixture {
      val command = new CountingCommand(department)
      val mav: Mav = controller.showForm(command)
      mav.viewName should be("admin/modules/arrange/form")
      mav.toModel should be(Map("breadcrumbs" -> Seq(AdminBreadcrumbs.Department(department))))

      command.populateCount should be(1)
      command.sortCount should be(1)
      command.applyCount should be(0)
    }
  }

  @Test def formOnChild(): Unit = {
    new Fixture {
      val command = new CountingCommand(subDepartment)
      val mav: Mav = controller.showForm(command)
      mav.viewName should be(s"redirect:${Routes.admin.department.sortModules(department)}")
      mav.toModel should be(Symbol("empty"))

      command.populateCount should be(1)
      command.sortCount should be(1)
      command.applyCount should be(0)
    }
  }

  @Test def formOnLoneDepartment(): Unit = {
    new Fixture {
      val command = new CountingCommand(Fixtures.department("xx"))
      val mav: Mav = controller.showForm(command)
      mav.viewName should be("admin/modules/arrange/form")
      mav.toModel should be(Map("breadcrumbs" -> Seq(AdminBreadcrumbs.Department(command.department))))

      command.populateCount should be(1)
      command.sortCount should be(1)
      command.applyCount should be(0)
    }
  }

  @Test def submitParent(): Unit = {
    new Fixture {
      val command = new CountingCommand(department)
      val errors = new BindException(command, "command")

      val mav: Mav = controller.submit(command, errors)
      mav.viewName should be("admin/modules/arrange/form")
      mav.toModel("saved").toString should be("true")

      command.populateCount should be(0)
      command.sortCount should be(1)
      command.applyCount should be(1)
    }
  }

  @Test def submitValidationErrors(): Unit = {
    new Fixture {
      val command = new CountingCommand(department)
      val errors = new BindException(command, "command")
      errors.reject("fail")

      val mav: Mav = controller.submit(command, errors)
      mav.viewName should be("admin/modules/arrange/form")
      mav.toModel should be(Map("breadcrumbs" -> Seq(AdminBreadcrumbs.Department(department))))

      command.populateCount should be(0)
      command.sortCount should be(1)
      command.applyCount should be(0)
    }
  }

}
