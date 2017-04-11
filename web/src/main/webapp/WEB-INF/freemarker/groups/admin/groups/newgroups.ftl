<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<div class="deptheader">
		<h1>Create small groups</h1>
		<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
	</div>

	<div class="fix-area">
		<@f.form id="newGroups" method="POST" commandName="command" class="dirty-check">
			<@components.set_wizard true 'groups' smallGroupSet />

			<#include "_editGroups.ftl" />

			<div class="fix-footer">
				<#if smallGroupSet.linked>
					<a class="btn btn-primary" href="<@routes.groups.createsetevents smallGroupSet />">Add events</a>
				<#else>
					<input
						type="submit"
						class="btn btn-primary"
						name="${ManageSmallGroupsMappingParameters.createAndAddStudents}"
						value="Save and add students"
					/>
					<input
						type="submit"
						class="btn btn-primary"
						name="create"
						value="Save and exit"
					/>
				</#if>
				<a class="btn btn-default dirty-check-ignore" href="<@routes.groups.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>">Cancel</a>
			</div>
		</@f.form>
	</div>
</#escape>