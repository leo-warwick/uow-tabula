<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<div class="pull-right">
		<#if syllabusPlusEventCount gt 0>
			<#if is_edit_set>
				<#assign import_external_url><@routes.groups.editseteventsediteventimport smallGroupEvent /></#assign>
			<#else>
				<#assign import_external_url><@routes.groups.createseteventsediteventimport smallGroupEvent /></#assign>
			</#if>

			<a class="btn btn-default" href="${import_external_url}">
				Update from Syllabus+
			</a>
		<#else>
			<div class=" use-tooltip" title="There are no scheduled small groups defined for <@fmt.module_name module false /> in Syllabus+, the central timetabling system">
				<a class="btn btn-default disabled">
					Import events from Syllabus+
				</a>
			</div>
		</#if>
	</div>

	<div class="deptheader">
		<div class="pull-right">
			<@fmt.bulk_email_group smallGroup.students "Email the students attending this event" />
		</div>
		<h1 class="with-settings">Edit event</h1>
		<h4 class="with-related"><span class="muted">for</span> ${smallGroup.name}</h4>
	</div>

	<@f.form method="post" action="" commandName="editSmallGroupEventCommand">
		<@f.errors cssClass="error form-errors" />

		<#assign newRecord=false />
		<#include "_event_fields.ftl" />

		<div class="submit-buttons">
			<input
				type="submit"
				class="btn btn-primary"
				name="create"
				value="Save"
			/>
			<a class="btn btn-default" href="${cancelUrl}">Cancel</a>
		</div>
	</@f.form>
</#escape>