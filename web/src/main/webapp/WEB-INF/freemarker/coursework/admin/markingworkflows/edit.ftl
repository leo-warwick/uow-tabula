<div class="pull-right">
	<a class="btn btn-default" href="<@routes.coursework.markingworkflowreplace command.markingWorkflow />">Replace marker</a>
</div>

<#assign view_type="edit" />
<#assign form_url><@routes.coursework.markingworkflowedit command.markingWorkflow /></#assign>
<#include "_form.ftl" />