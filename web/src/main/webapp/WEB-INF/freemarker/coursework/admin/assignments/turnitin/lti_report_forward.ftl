<#escape x as x?html>

Requesting report from Turnitin

<form action="${turnitin_report_url}" method="post" id="report-form">
	<#list params?keys as key>
		<#assign value = params[key] />
		<input type="hidden" name="${key}" value="${value}" />
	</#list>
</form>
<script>
	jQuery(function($){
		$('#report-form').submit();
	});
</script>
</#escape>