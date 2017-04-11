<#--

This section contains the form fields that can apply to a group of
assignments, as well as to an individual one.

If you add a field it should also be added to _common_fields_hidden.ftl
so that they can be passed around between requests.

-->

<#if features.feedbackTemplates && department.feedbackTemplates?has_content>
	<@form.labelled_row "feedbackTemplate" "Feedback template">
		<@f.select path="feedbackTemplate">
			<@f.option value="" label="No template"/>
			<#list department.feedbackTemplates as template>
				<@f.option value="${template.id}" label="${template.name}"/>
			</#list>
		</@f.select>
		<div class="help-block">
			Select the feedback template that will be used for this assignment. Copies of the template will be
			distributed along with student submissions.
		</div>
	</@form.labelled_row>
</#if>

<#if features.markingWorkflows && command.allMarkingWorkflows?has_content>

	<#assign disabled = !(canUpdateMarkingWorkflow!true)>

	<@form.labelled_row "markingWorkflow" "Marking workflow">
		<#if !disabled>
			<@f.select path="markingWorkflow">
				<@f.option value="" label="None"/>
				<#list command.allMarkingWorkflows as markingWorkflow>
					<@f.option value="${markingWorkflow.id}" label="${markingWorkflow.name} (${markingWorkflow.markingMethod.description})"/>
				</#list>
			</@f.select>
			<div class="help-block">
				Marking workflows define how and by whom the assignment will be marked. They are set up for the department by a Departmental Administrator <#if can.do("MarkingWorkflow.Manage", department)><a href="<@routes.coursework.markingworkflowlist department />">here</a></#if>.
			</div>
		<#else>
			<span class="uneditable-value">
				<#if command.markingWorkflow??>
					${command.markingWorkflow.name}
				<#else>
					(none)
				</#if>
			</span>
			<div class="help-block">
				<span class="warning">You cannot change the marking workflow for this assignment as it already has submissions.</span>
			</div>
			<#if command.markingWorkflow??>
				<label class="checkbox">
					<input type="checkbox" id="removeWorkflowPreview" />
					Remove marking workflow
				</label>
				<div class="alert alert-warning" id="removeWorkflowMessage" style="margin-bottom: 0; <#if !command.removeWorkflow>display: none;</#if>">
					This cannot be undone. If you remove the marking workflow:
					<ul>
						<li>you will lose access to any existing marker feedback</li>
						<li>you will not be able to add another workflow</li>
						<li>you will not be able to re-apply the current workflow</li>
					</ul>
					<label class="checkbox">
						<@f.checkbox path="removeWorkflow" id="removeWorkflow"/>
						Confirm that you wish to remove the marking workflow
					</label>
				</div>
				<script>
					jQuery(function($){
						$('#removeWorkflowPreview').on('change', function(){
							if ($(this).is(':checked')) {
								$('#removeWorkflowMessage').show();
							} else {
								$('#removeWorkflowMessage').hide();
							}
						})
					})
				</script>
			</#if>
		</#if>



	</@form.labelled_row>

	<#assign automaticallyReleaseToMarkersHelp>
		When using a marking workflow, automatically release all submissions to markers when the assignment closes.
		Any late submissions or submissions within an extension will be released when they are received.<br />
		<strong>Note:</strong> Students who do not submit will not be released automatically and will need to be released manually.
	</#assign>
	<@form.row>
		<@form.field>
			<label class="checkbox">
				<@f.checkbox path="automaticallyReleaseToMarkers" id="automaticallyReleaseToMarkers" />
				Automatically release to markers
				<@fmt.help_popover id="automaticallyReleaseToMarkersHelp" content="${automaticallyReleaseToMarkersHelp}" html=true />
			</label>
		</@form.field>
	</@form.row>
</#if>

<#if features.collectMarks>
	<@form.labelled_row "collectMarks" "Marks">
		<label class="checkbox">
			<@f.checkbox path="collectMarks" id="collectMarks" />
			Collect marks
		</label>
	</@form.labelled_row>
</#if>

<#if features.summativeFilter>
	<@form.row>
			<@form.label>Credit bearing</@form.label>
			<@form.field>
				<label class="radio">
					<@f.radiobutton path="summative" value="true" />
					Summative (counts towards final mark)
				</label>
				<label class="radio">
					<@f.radiobutton path="summative" value="false" />
					Formative (does not count towards final mark)
				</label>
				<div class="help-block">
					This field only affects feedback reports.
				</div>
			</@form.field>
		</@form.row>
</#if>

<#if features.dissertationFilter>
	<@form.row>

		<@form.field>
		<label class="checkbox">
			<@f.checkbox path="dissertation" id="dissertation" />
			Is this assignment a dissertation?
		</label>
		<div class="help-block">
			Dissertations don't have a 20 day turnaround time for feedback.
		</div>
		</@form.field>
	</@form.row>
</#if>
