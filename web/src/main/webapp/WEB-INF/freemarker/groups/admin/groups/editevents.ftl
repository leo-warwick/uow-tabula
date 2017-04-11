<#escape x as x?html>
<#import "*/group_components.ftl" as components />
	<div class="deptheader">
		<h1>Edit small groups</h1>
		<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /> <#if smallGroupSet??>(${smallGroupSet.academicYear.toString})</#if></h4>
	</div>

	<#if saved!false>
		<div class="alert alert-info">
			<button type="button" class="close" data-dismiss="alert">&times;</button>
			Events saved for ${smallGroupSet.name}.
		</div>
	</#if>

	<div class="fix-area">
		<@f.form id="editEvents" method="POST" commandName="command">
			<@components.set_wizard false 'events' smallGroupSet />

			<#assign is_edit = true />
			<#include "_editEvents.ftl" />

			<div class="fix-footer">
				<input
					type="submit"
					class="btn btn-primary update-only"
					value="Save"
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