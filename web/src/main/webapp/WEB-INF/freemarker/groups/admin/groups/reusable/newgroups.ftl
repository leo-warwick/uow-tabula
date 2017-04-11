<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create a set of reusable small groups</h1>

	<@f.form id="newGroups" method="POST" commandName="command">
		<@components.reusable_set_wizard true 'groups' smallGroupSet />

		<#include "_editGroups.ftl" />

		<@bs3form.form_group>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="${ManageDepartmentSmallGroupsMappingParameters.createAndAddStudents}"
				value="Save and add students"
				title="Select which students are included in these groups"
				data-container="body"
				/>
			<input
				type="submit"
				class="btn btn-primary use-tooltip"
				name="create"
				value="Save and exit"
				title="Save your groups and allocate students later"
				data-container="body"
				/>
			<a class="btn btn-default" href="<@routes.groups.crossmodulegroups smallGroupSet.department smallGroupSet.academicYear />">Cancel</a>
		</@bs3form.form_group>
	</@f.form>
</#escape>