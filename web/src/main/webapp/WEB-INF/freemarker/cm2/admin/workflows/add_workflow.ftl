<#escape x as x?html>

<#assign formAction><@routes.cm2.reusableWorkflowAdd department academicYear /></#assign>
<#assign commandName = "addMarkingWorkflowCommand" />
<#assign isNew = true />

<#include "_modify_workflow.ftl" />

</#escape>