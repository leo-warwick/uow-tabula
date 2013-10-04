<#--
HFC-166 Don't use #compress on this file because
the comments textarea needs to maintain newlines.
-->
<#escape x as x?html>

<#-- Field to support redirection post-submit -->
<input type="hidden" name="action" value="submit" id="action-submit">

<#macro datefield path label cssClass="">
	<@form.labelled_row path label cssClass>
		<@f.input path=path cssClass="date-time-picker" />
	</@form.labelled_row>
</#macro>

<div class="row-fluid">
<div class="span6">
<@form.labelled_row "name" "Assignment name">
<@f.input path="name" cssClass="text" />
</@form.labelled_row>

<@datefield path="openDate" label="Open date" />

<@form.labelled_row "openEnded" "Open-ended">
	<label class="checkbox">
		<@f.checkbox path="openEnded" id="openEnded" />

		<#assign popoverText>
			<p>
		   Check this box to mark the assignment as open-ended.
		  </p>

		  <ul>
		   <li>Any close date previously entered will have no effect.</li>
		   <li>Allowing extensions and submission after the close date will have no effect.</li>
		   <li>No close date will be shown to students.</li>
		   <li>There will be no warnings for lateness, and no automatic deductions to marks.</li>
		   <li>You will be able to publish feedback individually at any time.</li>
		  </ul>
		</#assign>

		<a href="#"
		   title="What's this?"
		   class="use-popover"
		   data-title="Open-ended assignments"
		   data-html="true"
		   data-trigger="hover"
		   data-content="${popoverText}"
		   ><i class="icon-question-sign"></i></a>
	</label>
</@form.labelled_row>

<@datefield path="closeDate" label="Close date" cssClass="has-close-date" />

<#if newRecord>

	<@form.labelled_row "academicYear" "Academic year">
		<@f.select path="academicYear" id="academicYear">
			<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
		</@f.select>
	</@form.labelled_row>
	
	<script type="text/javascript">
		jQuery(function($) {
			$('#academicYear').on('change', function(e) {
				var $form = $(this).closest('form');
				$('#action-submit').val('refresh');
				
				$form.submit();
			});
		});
	</script>

<#else>

	<@form.labelled_row "academicYear" "Academic year">
	<@spring.bind path="academicYearString">
	<span class="uneditable-value">${status.value} <span class="hint">(can't be changed)<span></span>
	</@spring.bind>
	</@form.labelled_row>

</#if>

</div> <#-- end span6 -->

<#if newRecord>
	<!-- <div id="assignment-picker" class="alert alert-success"> -->
	<div class="span6 alert alert-success">
		<p><i class="icon-lightbulb"></i> To find an assignment to pre-populate from, just start typing its name.  Assignments within your
		department will be matched.  Click on an assignment to choose it.</p>
		<@form.labelled_row "prefillAssignment" "Assignment to copy:">
			<@f.hidden id="prefillAssignment" path="prefillAssignment" />

			<#if command.prefillAssignment??>
				<#assign pHolder = "${command.prefillAssignment.name} - ${command.prefillAssignment.module.code}">
			</#if>

			<input class="assignment-picker-input" type="text"
				placeholder="${pHolder!''}">
		</@form.labelled_row>
	</div> <!-- span6 -->
	<div style="clear:both;"></div>
	<script>
		jQuery(function ($) {
		    var assignmentPickerMappings;
			$(".assignment-picker-input").typeahead({
				source: function(query, process) {
					$.get("${url('/admin/module/${module.code}/assignments/picker')}", { searchTerm : query}, function(data) {
						var labels = []; // labels is the list of Strings representing assignments displayed on the screen
						assignmentPickerMappings = {};

						$.each(data, function(i, assignment) {
						    var mapKey = assignment.name + " - " + assignment.moduleCode;
							assignmentPickerMappings[mapKey] = assignment.id;
							labels.push(mapKey);
						})
						process(labels);
					});
				},
				updater: function(mapKey) {
					var assignmentId = assignmentPickerMappings[mapKey];
					$("#prefillAssignment").val(assignmentId);
					$(".assignment-picker-input").val(mapKey);
					$("#action-input").val("prefill");
					$("#addAssignmentCommand").submit();
					return assignmentPickerMappings[mapKey] || '';
				},
				minLength:1
			});
		});
	</script>
</#if>

</div> <#-- end row-fluid div -->

<#-- These fields are also used by the batch assignment importer so they are in a separate file. -->
<#include "_common_fields.ftl" />

<#import "*/membership_picker_macros.ftl" as membership_picker />

<#-- Members picker is pretty hefty so it is in a separate file -->
<#if features.assignmentMembership>

<@form.row "members" "assignmentEnrolment">
	<details id="students-details">
		<summary id="students-summary" class="collapsible large-chevron">
			<span class="legend" >Students <small>Select which students should be in this assignment</small> </span>
			<div class="alert alert-success" style="display: none;" data-display="fragment">
				The membership list for this assignment has been updated
			</div>
			<@membership_picker.header command />
		</summary>
		<@membership_picker.fieldset command 'assignment' 'assignment'/>
	</details>
</@form.row>

</#if>
<#include "_submissions_common_fields.ftl" />

<script>
jQuery(function ($) {

	<#-- TAB-830 expand submission details when collectSubmissions checkbox checked -->
	$('input[name=collectSubmissions]').on('change click keypress',function(e) {

		e.stopPropagation();

		var collectSubmissions = $('input[name=collectSubmissions]').is(':checked');

		if (collectSubmissions) {
			$("html.no-details details.submissions:not(.open) summary").click();
			$("html.details details.submissions:not([open]) summary").click();

		}  else {
			$("html.no-details details.submissions.open summary").click();
			$("html.details details.submissions[open] summary").click();
		}
	});

	<#-- if summary supported, disable defaults for this summary when a contained label element is clicked -->
	$("summary").on("click", "label", function(e){
		e.stopPropagation();
		e.preventDefault();
		$('#collectSubmissions').attr('checked', !$('#collectSubmissions').attr('checked') );
		$('input[name=collectSubmissions]').triggerHandler("change");
	});

	$('#action-submit').closest('form').on('click', '.update-only', function() {
		$('#action-submit').val('update');
	});

});
</script>
</#escape>