<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<@f.form id="newGroups" method="POST" commandName="command" class="form-horizontal">
		<@components.set_wizard true 'groups' smallGroupSet.linked />

		<#include "_editGroups.ftl" />

		<div class="submit-buttons">
			<#if smallGroupSet.linked>
				<a class="btn btn-success" href="<@routes.createsetevents smallGroupSet />">Add events</a>
			<#else>
				<input
					type="submit"
					class="btn btn-success"
					name="${ManageSmallGroupsMappingParameters.createAndAddStudents}"
					value="Add students"
					/>
				<input
					type="submit"
					class="btn btn-primary"
					name="create"
					value="Save"
					/>
			</#if>
			<a class="btn" href="<@routes.depthome module=smallGroupSet.module />">Cancel</a>
		</div>
	</@f.form>
</#escape>