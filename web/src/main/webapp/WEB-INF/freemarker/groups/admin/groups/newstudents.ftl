<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<h1>Create small groups</h1>
	<h4><span class="muted">for</span> <@fmt.module_name module /></h4>

	<div class="fix-area">
		<@f.form id="newStudents" method="POST" commandName="command" class="dirty-check">
			<@components.set_wizard true 'students' smallGroupSet />

			<#include "_editStudents.ftl" />

			<div class="fix-footer">
				<#if smallGroupSet.linked>
					<a class="btn btn-primary" href="<@routes.groups.createsetevents smallGroupSet />">Add events</a>
				<#else>
					<input
						type="submit"
						class="btn btn-primary"
						name="${ManageSmallGroupsMappingParameters.createAndAddEvents}"
						value="Save and add events"
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