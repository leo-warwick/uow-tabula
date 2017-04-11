<#assign command = sortModulesCommand />
<#assign commandName = "sortModulesCommand" />
<#assign objectName = "module" />
<#assign submitUrl = url('/admin/department/${command.department.code}/sort-modules') />
<#escape x as x?html>

	<#include "../../_arrange.ftl" />

</#escape>