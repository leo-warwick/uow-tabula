package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.{AutowiringRelationshipServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

abstract class AbstractRelationshipAgentsController
  extends ApiController {
  self: RelationshipServiceComponent =>

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(@ModelAttribute("getCommand") cmd: ViewRelationshipAgentsCommand): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "agents" -> cmd.apply().map(sr => Map(
          "firstName" -> sr(1),
          "lastName" -> sr(2),
          "universityId" -> sr(0)
        ))
      )
    ))
  }

  class ViewRelationshipAgentsCommand(val permission: Permission, val permissionsTarget: PermissionsTarget, val relationshipType: StudentRelationshipType) extends Command[Seq[Array[Object]]] with ReadOnly with Unaudited {
    PermissionCheck(permission, permissionsTarget)

    override def applyInternal(): Seq[Array[Object]] = permissionsTarget match {
      case dept: Department => relationshipService.listCurrentStudentRelationshipsByDepartment(relationshipType, dept).groupBy(_.agent).filter(sr => sr._2.head.agentMember.isDefined).map(sr =>
        Seq(sr._1, sr._2.head.agentMember.map(am => am.firstName).orNull, sr._2.head.agentLastName).toArray.asInstanceOf[Array[Object]]
      ).toSeq
      case PermissionsTarget.Global => relationshipService.listCurrentRelationshipsGlobally(relationshipType)
      case _ => throw new IllegalArgumentException("Unsupported permissionsTarget")
    }
  }
}

@Controller
@RequestMapping(Array("/v1/relationships/agents/{department}/{studentRelationshipType}"))
class RelationshipAgentsForDepartmentController extends AbstractRelationshipAgentsController
  with AutowiringRelationshipServiceComponent {
  @ModelAttribute("getCommand")
  def getCommand(@PathVariable studentRelationshipType: StudentRelationshipType, @PathVariable department: Department): ViewRelationshipAgentsCommand =
    new ViewRelationshipAgentsCommand(Permissions.Profiles.StudentRelationship.Read(studentRelationshipType), mandatory(department), mandatory(studentRelationshipType))
}

@Controller
@RequestMapping(Array("/v1/relationships/agents/{studentRelationshipType}"))
class RelationshipAgentsGlobalController extends AbstractRelationshipAgentsController
  with AutowiringRelationshipServiceComponent {
  @ModelAttribute("getCommand")
  def getCommand(@PathVariable studentRelationshipType: StudentRelationshipType): ViewRelationshipAgentsCommand =
    new ViewRelationshipAgentsCommand(Permissions.Profiles.StudentRelationship.Read(studentRelationshipType), PermissionsTarget.Global, mandatory(studentRelationshipType))
}
