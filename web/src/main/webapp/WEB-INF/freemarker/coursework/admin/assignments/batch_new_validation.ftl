<#--

Included directly in batch_new_select.ftl, and also rendered separately by another
controller method so that we can validate through AJAX.

-->
<@spring.hasBindErrors name=commandName>
<#if errors.hasErrors()>
<div class="alert alert-error">
<h3>Some problems need fixing</h3>
<#if errors.hasGlobalErrors()>
	<#list errors.globalErrors as e>
		<div><@spring.message message=e /></div>
	</#list>
<#else>
	<#-- there were errors but they're all field errors. -->
	<div>See the errors below.</div>
</#if>
</div>
</#if>
</@spring.hasBindErrors>