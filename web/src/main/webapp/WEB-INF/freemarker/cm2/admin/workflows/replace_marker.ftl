<#escape x as x?html>

<#assign formAction><@routes.cm2.reusableWorkflowReplaceMarker department academicYear workflow /></#assign>
<#assign cancelAction><@routes.cm2.reusableWorkflowEdit department academicYear workflow /></#assign>

<div class="deptheader">
	<h1>Replace marker in </h1>
	<h4 class="with-related">${workflow.name}</h4>
</div>

<@f.form
	method="POST"
	class="double-submit-protection"
	action="${formAction}"
	commandName="replaceMarkerCommand"
>
	<@f.errors cssClass="error form-errors" />

	<@bs3form.labelled_form_group path="oldMarker" labelText="Marker to replace">
		<@f.select path="oldMarker" class="form-control" >
			<option value="" disabled selected></option>
			<#list replaceMarkerCommand.allMarkers as marker>
				<option <#if replaceMarkerCommand.oldMarker?? && replaceMarkerCommand.oldMarker == marker.userId>selected </#if>value="${marker.userId}">${marker.firstName} ${marker.lastName}</option>
			</#list>
		</@f.select>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group path="newMarker" labelText="New marker">
		<@bs3form.flexipicker path="newMarker" placeholder="User name" list=false multiple=false auto_multiple=false />
	</@bs3form.labelled_form_group>

	<#if replaceMarkerCommand.affectedAssignments?has_content>
		<@bs3form.labelled_form_group path="" labelText="The following assignments use this marking workflow">
			<ul>
				<#list replaceMarkerCommand.affectedAssignments as assignment>
					<li>${assignment.module.code?upper_case} ${assignment.name}</li>
				</#list>
			</ul>
		</@bs3form.labelled_form_group>

		<@bs3form.labelled_form_group path="includeCompleted" labelText="Should the marker be replaced for completed assignments?">
			<@bs3form.checkbox path="includeCompleted">
				<@f.checkbox path="includeCompleted" /> Replace the marker for assginments where marking has finished.
			</@bs3form.checkbox>
		</@bs3form.labelled_form_group>

		<div class="alert alert-info">
			<p>
				Any submissions to the assignments listed above that have already been released will not be re-released, so the new marker will not be notified that they need to mark the submissions. You should contact the new marker yourself.
			</p>
			<@bs3form.checkbox path="confirm">
				<@f.checkbox path="confirm" /> I understand that I need to contact the new marker to inform them that they may need to mark submissions.
			</@bs3form.checkbox>
		</div>
	<#else>
		<@f.hidden path="confirm" value="true" />
	</#if>

	<@bs3form.labelled_form_group>
		<input type="submit" value="Save" class="btn btn-primary">
		<a class="btn btn-default" href="${cancelAction}">Cancel</a>
	</@bs3form.labelled_form_group>

</@f.form>

</#escape>