<#escape x as x?html>
  <#if ownedDepartments?size gt 0>
    <#if ownedDepartments?size gt 1>
      <p>You are an administrator for multiple departments. Please choose a department to manage.</p>
    </#if>

    <ul>
      <#list ownedDepartments as department>
        <li><a href="<@routes.coursework.departmenthome department />">Manage ${department.name}</a></li>
      </#list>
    </ul>
  </#if>
</#escape>