<#compress><#escape x as x?html>

  <#import "*/permissions_macros.ftl" as pm />
  <#assign perms_url><@routes.admin.deptperms department/></#assign>

  <div class="permissions-page">
    <div class="pull-right btn-toolbar">
      <a class="btn btn-default" href="<@routes.admin.rolesDepartment department />">About roles</a>
      <a class="btn btn-default" href="<@routes.admin.permissions department />">Advanced</a>
    </div>

    <#function route_function dept>
      <#local result><@routes.admin.deptperms dept /></#local>
      <#return result />
    </#function>
    <@fmt.id7_deptheader "Departmental permissions" route_function "for" />

    <#if department.hasChildren>
      <div class="alert alert-info">
        <i class="fa fa-info-circle"></i> Permissions in a parent department cascade to sub-departments.
        Any permission explicitly granted or revoked in a sub-department takes precedence over the parent department.
      </div>
    </#if>

    <@pm.validation_alerts "addCommand" />
    <@pm.validation_alerts "removeCommand" />

    <#if (action!"") == "add">
      <@pm.alerts department.name users role />
    <#elseif (action!"") == "remove">
      <@pm.alerts department.name users role />
    </#if>

    <div class="row" id="tutors-supervisors-row">
      <div class="col-md-6">
        <h3 class="permissionTitle">Senior
          tutors <@fmt.help_popover id="seniortutors" title="Senior tutors" content="A senior tutor can see everything that a personal tutor can, for every student in the department." html=true /></h3>

        <@pm.roleTable perms_url "tutor-table" department "StudentRelationshipAgentRoleDefinition(tutor)" "Senior tutor" />
      </div>

      <div class="col-md-6">
        <h3 class="permissionTitle">Senior
          supervisors <@fmt.help_popover id="seniorsupervisors" title="Senior supervisors" content="A senior supervisor can see everything that a supervisor can, for every student in the department." html=true /></h3>

        <@pm.roleTable perms_url "supervisor-table" department "StudentRelationshipAgentRoleDefinition(supervisor)" "Senior supervisor" />
      </div>
    </div>
    <div class="row">
      <div class="col-md-6">
        <h3 class="permissionTitle">Departmental
          administrators <@fmt.help_popover id="deptadmins" title="Departmental Administrators" content="A departmental administrator can manage Modules, Marking workflows and Extension settings; and can assign tutors and supervisors." html=true /></h3>

        <@pm.roleTable perms_url "admin-table" department "DepartmentalAdministratorRoleDefinition" "Departmental administrator" />
      </div>

      <#if features.mitCircs>
        <div class="col-md-6">
          <h3 class="permissionTitle">Mitigating circumstances officers
            <@fmt.help_popover id="mcos" title="Mitigating circumstances officers" content="A mitigating circumstances officer can create and review mitigating circumstances submissions." html=true /></h3>

          <@pm.roleTable perms_url "mco-table" department "MitigatingCircumstancesOfficerRoleDefinition" "Mitigating circumstances officer" />
        </div>
      </#if>
    </div>

    <#assign scope=department />

    <h2>Module roles (for all modules in the department)</h2>

    <#include "../modules/_roles.ftl" />

    <h2>Route roles (for all routes in the department)</h2>

    <#include "../routes/_roles.ftl" />
  </div>

</#escape></#compress>
