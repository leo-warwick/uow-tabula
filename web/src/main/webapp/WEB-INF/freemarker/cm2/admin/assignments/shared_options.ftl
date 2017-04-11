<#escape x as x?html>
	<#assign command = sharedAssignmentPropertiesForm />
	<#assign actionUrl><@routes.cm2.assignmentSharedOptions department /></#assign>

	<@f.form method="post" action=actionUrl commandName="sharedAssignmentPropertiesForm" >
		<#if submitted?? && submitted && !hasErrors>
			<span class="ajax-response" data-status="success"></span>
		</#if>

		<#-- This is also included by assignment tabs (feedback/submission/option) since the forms to add/edit a single assignment
		use the same fields -->
		<#include "_feedback_fields.ftl"/>
		<#include "_submissions_fields.ftl" />
		<#include "_options_fields.ftl" />
	</@f.form>

</#escape>