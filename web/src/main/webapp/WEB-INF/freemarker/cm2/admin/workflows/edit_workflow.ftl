<#escape x as x?html>

<#assign formAction><@routes.cm2.reusableWorkflowEdit department academicYear workflow/></#assign>
<#assign commandName = "editMarkingWorkflowCommand" />
<#assign isNew = false />

<#include "_modify_workflow.ftl" />

</#escape>