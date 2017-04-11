<#escape x as x?html>
	<#assign form_url><@routes.admin.addcustomroleoverride customRoleDefinition /></#assign>

	<h1>Add custom role override</h1>

	<@f.form method="post" action="${form_url}" commandName="command">
		<@f.errors cssClass="error form-errors" />

		<@bs3form.labelled_form_group path="permission" labelText="Permission">
			<@f.select path="permission" cssClass="form-control">
				<#list allPermissions?keys as group>
					<optgroup label="${group}">
						<#list allPermissions[group] as permission>
							<option value="${permission._1()}"<#if status.value?? && permission._1()?? && (status.value!"") == (permission._1()!"")> selected="selected"</#if>>${permission._2()}</option>
						</#list>
					</optgroup>
				</#list>
			</@f.select>
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="overrideType" labelText="Allowed?">
			<@f.select path="overrideType" cssClass="form-control">
				<option value="true">Allow</option>
				<option value="false">Disallow</option>
			</@f.select>
		</@bs3form.labelled_form_group>

		<@bs3form.form_group>
			<input type="submit" value="Add" class="btn btn-primary">
			<a class="btn btn-default" href="<@routes.admin.customroleoverrides customRoleDefinition />">Cancel</a>
		</@bs3form.form_group>
	</@f.form>
</#escape>