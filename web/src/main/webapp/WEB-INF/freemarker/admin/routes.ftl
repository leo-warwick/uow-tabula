<#--
Just a handy place to create macros for generating URLs to various places, to save time
if we end up changing any of them.

TODO grab values from the Routes object in code, as that's pretty equivalent and
	we're repeating ourselves here. OR expose Routes directly.

--><#compress>
<#macro _u page><@url context='/admin' page=page /></#macro>

<#macro home><@_u page="/" /></#macro>
<#macro departmenthome department><@_u page="/department/${department.code}/" /></#macro>
<#macro depthome module><@_u page="/department/${module.adminDepartment.code}/#module-${module.code}" /></#macro>

<#macro deptperms department><@_u page="/department/${department.code}/permissions" /></#macro>
<#macro moduleperms module><@_u page="/module/${module.code}/permissions" /></#macro>
<#macro routeperms route><@_u page="/route/${route.code}/permissions" /></#macro>

<#macro displaysettings department><@_u page="/department/${department.code}/settings/display" /></#macro>
<#macro notificationsettings department><@_u page="/department/${department.code}/settings/notification" /></#macro>

<#macro editdepartment department><@_u page="/department/${department.code}/edit" /></#macro>
<#macro createsubdepartment department><@_u page="/department/${department.code}/subdepartment/new" /></#macro>
<#macro createmodule department><@_u page="/department/${department.code}/module/new" /></#macro>
<#macro sortmodules department><@_u page="/department/${department.code}/sort-modules" /></#macro>
<#macro sortroutes department><@_u page="/department/${department.code}/sort-routes" /></#macro>

<#macro permissions scope><@_u page="/permissions/${scope.urlCategory}/${scope.urlSlug}" /></#macro>
<#macro permissionstree scope><@_u page="/permissions/${scope.urlCategory}/${scope.urlSlug}/tree" /></#macro>

<#macro customroles department><@_u page="/department/${department.code}/customroles/list" /></#macro>
<#macro addcustomrole department><@_u page="/department/${department.code}/customroles/add" /></#macro>
<#macro editcustomrole role><@_u page="/department/${role.department.code}/customroles/${role.id}/edit" /></#macro>
<#macro deletecustomrole role><@_u page="/department/${role.department.code}/customroles/${role.id}/delete" /></#macro>
<#macro customroleoverrides role><@_u page="/department/${role.department.code}/customroles/${role.id}/overrides/list" /></#macro>
<#macro addcustomroleoverride role><@_u page="/department/${role.department.code}/customroles/${role.id}/overrides/add" /></#macro>
<#macro deletecustomroleoverride override><@_u page="/department/${override.customRoleDefinition.department.code}/customroles/${override.customRoleDefinition.id}/overrides/${override.id}/delete" /></#macro>

<#macro roles><@_u page="/roles" /></#macro>
<#macro rolesDepartment department><@_u page="/department/${department.code}/roles" /></#macro>

</#compress>
