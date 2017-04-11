<#escape x as x?html>

<h1>LTI Conformance tester (basic-lti-launch-request)</h1>

<@f.form method="post" action="${url('${endpoint}')}" commandName="ltiConformanceTesterPopulateFormCommand">

	<#list response?keys as key>
		<#if response[key]?has_content>
			<@bs3form.labelled_form_group labelText=key>
				<input type="text" value="${response[key]}" class="form-control" name="${key}" id="${key}" />
			</@bs3form.labelled_form_group>
		</#if>
	</#list>

	<@f.errors cssClass="error form-errors" />

	<@bs3form.labelled_form_group>
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn btn-default" href="<@url page="/sysadmin/turnitinlti" />">Cancel</a>
	</@bs3form.labelled_form_group>

</@f.form>

</#escape>