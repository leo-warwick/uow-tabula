<@f.form action="${url('/sysadmin/permissions-helper')}" commandName="permissionsHelperCommand">

	<@bs3form.labelled_form_group path="user" labelText="User ID">
		<@bs3form.flexipicker path="user" placeholder="Type name or usercode" />
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="scopeType" labelText="Permission scope type">
		<@bs3form.radio>
			<input type="radio" name="${status.expression}" value="" <#if !status.value?? || status.value?is_string>checked</#if> />
			None
		</@bs3form.radio>

		<#list allPermissionTargets as targetClass>
			<@bs3form.radio>
				<@f.radiobutton path="scopeType" value="${targetClass.name}" />
				${targetClass.simpleName}
			</@bs3form.radio>
		</#list>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="scope" labelText="Permission scope">
		<@f.input path="scope" cssClass="text form-control" /><small>Type the ID or code that you wish to use as a permission scope</small>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="permission" labelText="Permission">
		<@f.select path="permission" cssClass="form-control">
			<option value="">Show all</option>
			<#list allPermissions?keys as group>
				<#if group == "">
					<optgroup label="Global">
				<#else>
					<optgroup label="${group}">
				</#if>
					<#list allPermissions[group] as permission>
						<option value="${permission._1()}"<#if status.value?? && permission._1()?? && (status.value!"") == (permission._1()!"")> selected="selected"</#if>>${permission._2()}</option>
					</#list>
				</optgroup>
			</#list>
		</@f.select>
	</@bs3form.labelled_form_group>

	<@bs3form.form_group>
			<input class="btn btn-lg btn-primary" type="submit" value="Submit">
	</@bs3form.form_group>
</@f.form>