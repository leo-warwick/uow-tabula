<#--

Adding or editing a custom role definition

-->
<#if view_type="add">
	<#assign submit_text="Create" />
<#elseif view_type="edit">
	<#assign submit_text="Save" />
</#if>

<#escape x as x?html>
<#compress>

<h1><#if view_type = "add">Create<#else>Edit</#if> custom role definition</h1>
<#assign commandName="command" />

<@f.form method="post" action="${form_url}" commandName=commandName>
	<@f.errors cssClass="error form-errors" />

	<#--
	Common form fields.
	-->
	<@bs3form.labelled_form_group path="name" labelText="Name">
		<@f.input path="name" cssClass="form-control" />
		<div class="help-block">
			A descriptive name that will be used to refer to this role.
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="baseDefinition" labelText="Base role definition">
		<@f.select path="baseDefinition" cssClass="form-control">
			<#assign shownOriginalValue = false />
			<#list allRoleDefinitions as roleDefinition>
				<#assign roleDefinitionName><#compress>
					<#if roleDefinition.id??>
						${roleDefinition.id}
					<#elseif roleDefinition.selector??>
						${roleDefinition.name}(${roleDefinition.selector.id})
					<#else>
						${roleDefinition.name}
					</#if>
				</#compress></#assign>
				<option value="${roleDefinitionName}"<#if status.value! == roleDefinitionName> selected</#if>>${roleDefinition.description}</option>
				<#if !status.value?has_content || status.value == roleDefinitionName><#assign shownOriginalValue = true /></#if>
			</#list>
			<#if !shownOriginalValue>
				<option value="${status.value}" selected>${status.value}</option>
			</#if>
		</@f.select>
		<div class="help-block">
			The "starting point" for this custom role definition.
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.form_group>
		<input type="submit" value="${submit_text}" class="btn btn-primary">
		<a class="btn btn-default" href="<@routes.admin.customroles department />">Cancel</a>
	</@bs3form.form_group>

</@f.form>

</#compress>
</#escape>