<#escape x as x?html>
<div class="pull-right">
	<a class="btn btn-default" href="<@routes.exams.markingWorkflowReplace department markingWorkflow />">Replace marker</a>
</div>

<#assign view_type="edit" />
<#assign form_url><@routes.exams.markingWorkflowEdit department markingWorkflow /></#assign>
<#include "_form.ftl" />
</#escape>