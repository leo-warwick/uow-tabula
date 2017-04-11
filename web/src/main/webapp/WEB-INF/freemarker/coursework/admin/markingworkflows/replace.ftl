<#escape x as x?html>

<div class="fix-area">
	<h1>Replace marker in ${markingWorkflow.name}</h1>

	<#assign form_url><@routes.coursework.markingworkflowreplace markingWorkflow /></#assign>
	<@f.form method="post" action="${form_url}" commandName="command" cssClass="form-horizontal">
		<@f.errors cssClass="error form-errors" />

		<@form.labelled_row "oldMarker" "Marker to replace">
			<@f.select path="oldMarker">
				<#list command.allMarkers as marker>
					<option value="${marker.userId}">${marker.fullName} (${marker.userId})</option>
				</#list>
			</@f.select>
		</@form.labelled_row>

		<@form.labelled_row "newMarker" "New marker">
			<@form.flexipicker path="newMarker" />
		</@form.labelled_row>

		<div class="alert alert-warning">
			<p>
				<strong>Note: </strong> Any submissions to the assignments listed below that have already been released will not be re-released,
				so the new marker will not be notified that they need to mark the submissions. You should contact the new marker yourself.
			</p>
			<p>
				<label class="checkbox"><@f.checkbox path="confirm" /> I understand that I need to contact the new marker to inform them that they may need to mark submissions.</label>
				<@f.errors path="confirm" cssClass="error form-errors" />
			</p>
		</div>

		<#if command.affectedAssignments?has_content>
			<p>The following assignments will be updated:</p>
			<ul>
				<#list command.affectedAssignments as assignment>
					<li><@fmt.assignment_name assignment=assignment withFormatting=false /></li>
				</#list>
			</ul>
		</#if>

		<div class="submit-buttons fix-footer">
			<input type="submit" value="Replace" class="btn btn-primary">
			<a class="btn" href="<@routes.coursework.markingworkflowedit markingWorkflow />">Cancel</a>
		</div>
	</@f.form>
</div>
</#escape>