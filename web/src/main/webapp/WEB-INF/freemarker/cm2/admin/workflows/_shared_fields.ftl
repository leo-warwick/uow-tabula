<#if newRecord || canEditWorkflowType>
	<@bs3form.labelled_form_group path="workflowType" labelText="Marking workflow type">
		<@f.select path="workflowType" class="form-control workflow-modification" >
			<option value="" disabled selected></option>
			<#list availableWorkflows as workflowType>
				<option <#if status.value?? && (status.value!"") == workflowType.name> selected="selected"</#if>
						value="${workflowType.name}"
						data-numroles="${workflowType.roleNames?size}"
						data-roles="${workflowType.roleNames?join(",")}"
				>
					${workflowType.description}
				</option>
			</#list>
		</@f.select>
		<div class="help-block">
			<a href="#" data-toggle="modal" data-target="#workflowTypeHelp">Which type should I use?</a>
		</div>
	</@bs3form.labelled_form_group>

	<!-- Modal -->
	<div class="modal fade" id="workflowTypeHelp" tabindex="-1" role="dialog" aria-labelledby="workflowTypeHelp">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
					<h4 class="modal-title" id="workflowTypeHelpLabel">Marking workflow types</h4>
				</div>
				<div class="modal-body">
					<dl>
						<dt>Single marking</dt>
						<dd>One marker is allocated to a submission. They mark the submission and pass it to the administrator.</dd>
						<dt>Moderated marking</dt>
						<dd>The first marker marks the submission and passes it to the moderator. The moderator decides the final mark and passes the submission to the administrator.</dd>
						<dt>Double seen marking</dt>
						<dd>The first marker adds their feedback. They send the submission to a second marker, who can see the first marker's feedback, mark or grade. The second marker adds their own feedback, mark or grade. They return the submission to the first marker, who decides the final mark and summarises comments into a single item of feedback for the student. Comments can be combined or rewritten entirely.</dd>
						<dt>Double blind marking</dt>
						<dd>Multiple independent markers concurrently mark a submission and do not see others’ marks. All submissions go to final markers who finalise the feedback and can average marks. The final marker passes the submission to the administrator. Note that only one final marker is allocated to each submission.</dd>
					</dl>
				</div>
			</div>
		</div>
	</div>
	<#if features.moderationSelector>
		<@bs3form.labelled_form_group path="sampler" labelText="Moderated assignment selection" cssClass="hidden sampler workflow-modification">
			<div class="radio">
				<@bs3form.radio_inline>
					<@f.radiobutton path="sampler" value="admin" /> Administrator
				</@bs3form.radio_inline>
				<@bs3form.radio_inline>
					<@f.radiobutton path="sampler" value="marker" /> Marker
				</@bs3form.radio_inline>
				<@bs3form.radio_inline>
					<@f.radiobutton path="sampler" value="moderator" /> Moderator
				</@bs3form.radio_inline>
			</div>
			<div class="help-block">
				Choose who is responsible for selecting assignments for moderation.
			</div>
		</@bs3form.labelled_form_group>
	</#if>
<#else>
	<@bs3form.labelled_form_group labelText="Workflow type">
		<select id="workflowType" name="workflowType" class="form-control workflow-modification" disabled="disabled">
			<option
					selected="selected"
					value="${workflow.workflowType.name}"
					data-numroles="${workflow.workflowType.roleNames?size}"
					data-roles="${workflow.workflowType.roleNames?join(",")}"
			>
				${workflow.workflowType.description}
			</option>
		</select>
		<input type="hidden" name="workflowType" value="${workflow.workflowType.name}" />

		<div class="help-block">
			<#if workflow.isReusable()>
				It is not possible to modify the marking method once a marking workflow has been created.
			<#else>
				It is not possible to modify the marking method once marking has started.
			</#if>
		</div>
	</@bs3form.labelled_form_group>

	<#if features.moderationSelector>
		<@bs3form.labelled_form_group path="sampler" labelText="Moderated assignment selection" cssClass="hidden sampler workflow-modification">
			<div class="radio">
				<@bs3form.radio_inline>
					<@f.radiobutton path="sampler" value="admin" disabled=true /> Administrator
				</@bs3form.radio_inline>
				<@bs3form.radio_inline>
					<@f.radiobutton path="sampler" value="marker" disabled=true  /> Marker
				</@bs3form.radio_inline>
				<@bs3form.radio_inline>
					<@f.radiobutton path="sampler" value="moderator" disabled=true  /> Moderator
				</@bs3form.radio_inline>
			</div>
			<div class="help-block">
				<#if workflow.isReusable()>
					It is not possible to modify the moderation selector once a marking workflow has been created.
				<#else>
					It is not possible to modify the moderation selector once marking has started.
				</#if>
			</div>
		</@bs3form.labelled_form_group>
	</#if>

</#if>

<#assign markerHelp>
	Add an individual <span class="role">marker</span>'s name or University ID.<#if !canDeleteMarkers> At least one assignment that uses this workflow has marking in progress so you can't remove markers. You can replace markers instead.</#if>
</#assign>

<@bs3form.labelled_form_group path="markersA" labelText="Add markers" cssClass="markersA">
	<@bs3form.flexipicker path="markersA" placeholder="User name" list=true multiple=true auto_multiple=false delete_existing=canDeleteMarkers />
	<div class="help-block">${markerHelp}</div>
</@bs3form.labelled_form_group>


<@bs3form.labelled_form_group path="markersB" labelText="Add markers" cssClass="markersB hide">
	<@bs3form.flexipicker path="markersB" placeholder="User name" list=true multiple=true auto_multiple=false delete_existing=canDeleteMarkers />
	<div class="help-block">${markerHelp}</div>
</@bs3form.labelled_form_group>

<#if !newRecord && workflow??>
	<@bs3form.labelled_form_group>
		<a href="<@routes.cm2.reusableWorkflowReplaceMarker department academicYear workflow />">Replace marker</a>
	</@bs3form.labelled_form_group>
</#if>

<script type="text/javascript">
	(function ($) { "use strict";
		$('select[name=workflowType]').on('change', function() {
			var $this = $(this);
			var $workflowOption = $this.find('option:selected');

			if($workflowOption.val() === 'Moderated'){
				$('.sampler').removeClass('hidden');
			} else {
				$('.sampler').addClass('hidden');
			}

			var roleNames = $workflowOption.data('roles') ? $workflowOption.data('roles').split(",") : [];
			if(roleNames.length !== 0){
				var roleA = roleNames[0].toLowerCase();
				$('.form-group.markersA label').text("Add " + roleA);
				$('.form-group.markersA .role').text(roleA);

				var useMarkerB = $workflowOption.data("numroles") > 1;
				if(useMarkerB) {
					var roleB = roleNames[1].toLowerCase();
					$('.form-group.markersB .role').text(roleB);
					$('.form-group.markersB label').text("Add " + roleB);
				}
				$('.markersB').removeClass('hide').toggle(useMarkerB);
			}

		}).trigger('change');

	})(jQuery);
</script>