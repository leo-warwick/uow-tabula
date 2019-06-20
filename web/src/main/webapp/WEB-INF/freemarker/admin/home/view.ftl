<#escape x as x?html>

  <#macro link_to_department department>
    <a href="<@routes.admin.departmenthome department />">
      Go to the ${department.name} admin page
    </a>
  </#macro>

  <#if user.loggedIn && user.firstName??>
    <h1>Hello, ${user.firstName}</h1>
  <#else>
    <h1>Hello</h1>
  </#if>

  <#if nonempty(ownedDepartments) || nonempty(ownedModuleDepartments) || nonempty(ownedRouteDepartments) || nonempty(departmentsWithManualUsers)>
    <div><h2 class="section">Administration</h2></div>

    <#if nonempty(ownedModuleDepartments)>
      <h6>My managed <@fmt.p number=ownedModuleDepartments?size singular="module" shownumber=false /></h6>

      <ul class="links">
        <#list ownedModuleDepartments as department>
          <li>
            <@link_to_department department />
          </li>
        </#list>
      </ul>
    </#if>

    <#if nonempty(ownedRouteDepartments)>
      <h6>My managed <@fmt.p number=ownedRouteDepartments?size singular="route" shownumber=false /></h6>

      <ul class="links">
        <#list ownedRouteDepartments as department>
          <li>
            <@link_to_department department />
          </li>
        </#list>
      </ul>
    </#if>

    <#if nonempty(ownedDepartments)>
      <h6>My department-wide <@fmt.p number=ownedDepartments?size singular="responsibility" plural="responsibilities" shownumber=false /></h6>

      <ul class="links">
        <#list ownedDepartments as department>
          <li>
            <@link_to_department department />
          </li>
        </#list>
      </ul>
    </#if>

    <#if nonempty(departmentsWithManualUsers)>
      <h6>Departments with manually added users</h6>
      <ul class="links">
        <#list departmentsWithManualUsers?keys as department>
          <li>
            <a href="<@routes.admin.manualmembershipeo department />">
              ${department.name} - (<@fmt.p mapGet(departmentsWithManualUsers, department).assignments "assignment"/>
              and <@fmt.p mapGet(departmentsWithManualUsers, department).smallGroupSets "small group set"/>)
            </a>
          </li>
        </#list>
      </ul>
    </#if>
  <#else>

    <p class="lead muted">
      This is a service for managing departmental administration settings and permissions.
    </p>
    <p>
      You do not currently have permission to manage departmental settings and permissions. If you think this is incorrect or you need assistance, please visit
      our <a href="/help">help page</a>.
    </p>
  </#if>

</#escape>
