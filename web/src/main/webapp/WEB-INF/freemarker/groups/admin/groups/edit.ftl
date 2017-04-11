<#escape x as x?html>
<#import "*/group_components.ftl" as components />

<div class="deptheader">
	<h1>Edit small groups</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /> <#if smallGroupSet??>(${smallGroupSet.academicYear.toString})</#if></h4>
</div>

<#if smallGroupSet?? && smallGroupSet.allocationMethod.dbValue == 'StudentSignUp'>
	<div class="alert">These groups are currently <strong>${smallGroupSet.openForSignups?string("open","closed")}</strong> for self sign-up</div>
</#if>

<#if saved!false>
	<div class="alert alert-info">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		Properties saved for ${smallGroupSet.name}.
	</div>
</#if>

<div class="fix-area">
	<@f.form method="post" action="${url('/groups/admin/module/${module.code}/groups/${smallGroupSet.id}/edit')}" commandName="editSmallGroupSetCommand" cssClass="dirty-check">
		<@components.set_wizard false 'properties' smallGroupSet />

		<@f.errors cssClass="error form-errors" />

		<#assign newRecord=false />
		<#include "_fields.ftl" />

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
			<a class="btn btn-default dirty-check-ignore" href="<@routes.groups.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>">Cancel</a>
		</div>

	</@f.form>
</div>

</#escape>