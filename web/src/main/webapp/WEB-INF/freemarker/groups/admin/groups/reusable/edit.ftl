<#import "*/group_components.ftl" as components />
<#escape x as x?html>
	<h1>Edit reusable small groups: ${smallGroupSet.name}</h1>

	<@f.form id="editGroups" method="POST" commandName="editDepartmentSmallGroupSetCommand">
		<@components.reusable_set_wizard false 'properties' smallGroupSet />

		<#assign label>
			Set name
			<@fmt.help_popover id="name" content="Give this set of groups a name to distinguish it from any other sets - eg. UG Year 1 seminars and UG Year 2 seminars" />
		</#assign>
		<@bs3form.labelled_form_group path="name" labelText="${label}">
			<@f.input path="name" cssClass="form-control" />
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="academicYear" labelText="Academic year">
			<@spring.bind path="academicYear">
				<p class="form-control-static">${status.actualValue.label} <span class="very-subtle">(can't be changed)</span></p>
			</@spring.bind>
		</@bs3form.labelled_form_group>

		<@bs3form.form_group>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.editAndAddGroups}"
				value="Save and edit groups"
				title="Edit groups for set of reusable groups"
				data-container="body"
			/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="edit"
				value="Save and exit"
				title="Save your groups and add students and groups to it later"
				data-container="body"
			/>
			<a class="btn btn-default" href="<@routes.groups.crossmodulegroups smallGroupSet.department smallGroupSet.academicYear />">Cancel</a>
		</@bs3form.form_group>
	</@f.form>
</#escape>