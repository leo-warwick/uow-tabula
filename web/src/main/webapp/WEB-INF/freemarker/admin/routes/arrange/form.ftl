<#assign command = sortRoutesCommand />
<#assign commandName = "sortRoutesCommand" />
<#assign objectName = "route" />
<#assign submitUrl = url('/admin/department/${command.department.code}/sort-routes') />
<#escape x as x?html>

<#include "../../_arrange.ftl" />

</#escape>