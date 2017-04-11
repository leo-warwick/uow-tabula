<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<div class="deptheader">
		<h1>Create small groups</h1>
		<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
	</div>

	<div class="fix-area">
		<@f.form id="newEvents" method="POST" commandName="command">
			<@components.set_wizard true 'events' smallGroupSet />

			<#assign is_edit = false />
			<#include "_editEvents.ftl" />

			<div class="fix-footer">
				<input
					type="submit"
					class="btn btn-primary"
					name="${ManageSmallGroupsMappingParameters.createAndAllocate}"
					value="Save and allocate students"
				/>
				<input
					type="submit"
					class="btn btn-primary"
					name="create"
					value="Save and exit"
				/>
				<a class="btn btn-default" href="<@routes.groups.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>">Cancel</a>
			</div>
		</@f.form>
	</div>
</#escape>