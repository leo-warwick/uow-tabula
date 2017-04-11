<#--

first page of the form to setup a bunch of assignments from SITS.

-->
<#escape x as x?html>

<#assign commandName="command"/>

<@fmt.deptheader "Setup assignments" "for" department routes.coursework "setupSitsAssignments" "" />

<#assign step=action!'select'/>

<@f.form method="post" id="batch-add-form" action="${url('/coursework/admin/department/${command.department.code}/setup-assignments')}" commandName=commandName cssClass="form-horizontal">

	<#if step='select'>

		<div class="alert alert-warning slow-page-warning">
			<p>This page may take a few seconds to fully load, please wait &hellip;</p>
		</div>

		<h2>Step 1 - choose which assignments to setup</h2>

		<div class="row-fluid">
			<div class="span10">
				<p>Below are all of the assessment components defined for this department in SITS, the central system.</p>

				<p>The first thing to do is choose which ones you want to set up to use for assessment.
					Use the checkboxes on the left hand side to choose which ones you want to setup in the coursework submission system,
					and then click Next. Some items (such as exams and "Audit Only" components) have already been unchecked,
					but you can check them if you want (for example, if you want to publish feedback for an exam).
				</p>

	<#elseif step='options'>

		<h2>Step 2 - choose options for assignments</h2>

		<div class="row-fluid">
			<div class="span10">
				<div id="batch-add-errors">
					<#include "batch_new_validation.ftl" />
				</div>

				<p>
					Now you need to choose how you want these assignments to behave, such as submission dates
					and resubmission behaviour.
				</p>
				<ul>
					<li>Click and drag to select/unselect assignments (or use the checkboxes on the left).</li>
					<li>Click <strong>Set options</strong> to set e-submission and other options for selected assignments.
						You can overwrite the options for an assignment so it might be a good idea to set the most common options with
						all the assignments selected, and then set more specific options for assignments that require it.
					</li>
					<li>Click <strong>Set dates</strong> to set the opening and closing dates for selected assignments.</li>
					<li>
						Once you've set the options for some assignments, you can click one of the <strong>Re-use</strong> buttons
						to quickly apply those same options to some other assignments.
					</li>
				</ul>

	</#if>

				<input type="hidden" name="action" value="error" /><!-- this is changed before submit -->


				<@form.labelled_row "academicYear" "Academic year">
					<#if step="select">
						<@f.select path="academicYear" id="academicYearSelect">
							<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
						</@f.select>
					<#else>
					<@f.hidden path="academicYear"/>
					<@f.hidden path="includeSubDepartments"/>
					<span class="uneditable-value">
						<@spring.bind path="academicYear">${status.actualValue.label}</@spring.bind>
					</span>
				  </#if>
				</@form.labelled_row>

				<#if department.children?size gt 0>
					<@form.labelled_row "includeSubDepartments" "">
						<@f.label checkbox=true for="includeSubDepartmentsSelect">
							<@f.checkbox path="includeSubDepartments" id="includeSubDepartmentsSelect" /> Include modules in sub-departments
						</@f.label>
					</@form.labelled_row>
				</#if>

				<#macro hidden_properties>
					<@f.hidden path="upstreamAssignment" />
					<@f.hidden path="optionsId" cssClass="options-id-input" />
					<@f.hidden path="openDate" cssClass="open-date-field" />
					<@f.hidden path="openEnded" cssClass="open-ended-field" />
					<@f.hidden path="closeDate" cssClass="close-date-field" />
					<@f.hidden path="occurrence" />
					<@f.hidden path="name" cssClass="name-field" />
				</#macro>

				<#--
					Always output these hidden properties for all assignments. We want to show them
					on step 1 because we might have gone back from step 2.
				-->
				<#list command.assignmentItems as item>
					<#if step != 'select' && !item.include>
						<@spring.nestedPath path="assignmentItems[${item_index}]">
							<@hidden_properties />
						</@spring.nestedPath>
					</#if>
				</#list>

				<table class="table table-bordered table-striped" id="batch-add-table">
					<tr>
						<th>
							<div class="check-all checkbox">
								<label>
									<span class="very-subtle"></span>
									<input type="checkbox" checked="checked" class="collection-check-all use-tooltip" title="Select/unselect all">
								</label>
							</div>
						</th>
						<th>Module</th>
						<th><abbr title="Component type">Type</abbr></th>
						<th><abbr title="Sequence">Seq</abbr></th>
						<th><abbr title="Occurrence/Cohort">Occ</abbr></th>
						<th>Component name</th>
						<#if step="options">
							<th></th>
							<th></th>
						</#if>
					</tr>
					<#list command.assignmentItems as item>
						<@spring.nestedPath path="assignmentItems[${item_index}]">
							<#if step="select" || item.include>
								<tr class="itemContainer">
									<td>
										<#-- saved options for the assignment stored here -->
										<@hidden_properties />

										<#if step="select">
											<@f.checkbox path="include" cssClass="collection-checkbox" />
										<#else>
											<@f.hidden path="include" />
											<input type="checkbox" checked="checked" class="collection-checkbox" />
										</#if>
									</td>
									<td class="selectable">
										${item.upstreamAssignment.moduleCode?upper_case}
									</td>
									<td class="selectable">
										${(item.upstreamAssignment.assessmentType.value)!'A'}
									</td>
									<td class="selectable">
										${item.upstreamAssignment.sequence}
									</td>
									<td class="selectable">
										${item.occurrence}
									</td>
									<td class="selectable">
										<span class="editable-name" id="editable-name-${item_index}">${item.name!''}</span>
										<#-- TODO expose as click-to-edit -->

										<#-- render all field errors for assignmentItems[x] -->
										<@f.errors path="" cssClass="error" />
										<#-- render all field errors for assignmentItems[x].* -->
										<@f.errors path="*" cssClass="error" />
									</td>
									<#if step="options">
										<td class="selectable assignment-editable-fields-cell">
											<span class="dates-label">
												<#if form.hasvalue('openDate') && form.hasvalue('closeDate')>
													${form.getvalue("openDate")}<#if form.hasvalue("openEnded") && form.getvalue("openEnded") == "true"> (open ended)<#else> - ${form.getvalue("closeDate")}</#if>
												</#if>
											</span>
										</td>
										<td>
											<span class="options-id-label">
												<#if form.hasvalue('optionsId')>
													<#assign optionsIdValue=form.getvalue('optionsId') />
													<span class="label label-${optionsIdValue}">${optionsIdValue}</span>
												</#if>
											</span>
										</td>
									</#if>
								</tr>

							<#else>
								<#-- we include the hidden fields of unincluded items below, outside the table -->
							</#if>
						</@spring.nestedPath>
					</#list>
				</table>

				<#-- Hidden fields for items we unchecked in the first step, just to remember that we unchecked them -->
				<#list command.assignmentItems as item>
					<@spring.nestedPath path="assignmentItems[${item_index}]">
						<#if step!="select" && !item.include>
							<@f.hidden path="include" />
						</#if>
					</@spring.nestedPath>
				</#list>

			</div>

			<div class="span2">
				<#if step='select'>
					<button class="btn btn-large btn-primary btn-block" data-action="options">Next</button>
					<#-- This is for if you go Back from step 2, to remember previous options -->
					<#list command.optionsMap?keys as optionsId>
						<div class="options-group">
							<@spring.nestedPath path="optionsMap[${optionsId}]">
								<#assign ignoreQueueFeedbackForSits = true />
								<#include "_common_fields_hidden.ftl" />
							</@spring.nestedPath>
						</div>
					</#list>
				<#elseif step='options'>
					<div id="options-buttons">
						<button class="btn btn-large btn-block use-tooltip" data-container="body" data-action="refresh-select" title="Go back to change your assignment choices, without losing your work so far.">&larr; Back</button>
						<button id="batch-add-submit-button" class="btn btn-large btn-primary btn-block" data-action="submit">Submit</button>

						<div id="selected-count">0 selected</div>
						<div id="selected-deselect"><a href="#">Clear selection</a></div>
						<#-- options sets -->
						<a class="btn btn-info btn-block" id="set-options-button" data-target="#set-options-modal" href="<@url page="/coursework/admin/department/${department.code}/shared-options"/>">
							Set options&hellip;
						</a>
						<a class="btn btn-info btn-block" id="set-dates-button" data-target="#set-dates-modal">
							Set dates&hellip;
						</a>

						<#list command.optionsMap?keys as optionsId>
							<div class="options-button">
								<button class="btn btn-block" data-group="${optionsId}">
									Re-use options
									<span class="label label-${optionsId}">${optionsId}</span>
								</button>
								<div class="options-group">
									<@spring.nestedPath path="optionsMap[${optionsId}]">
										<#-- Include all the common fields as hidden fields -->
										<#assign ignoreQueueFeedbackForSits = true />
										<#include "_common_fields_hidden.ftl" />
									</@spring.nestedPath>
								</div>
							</div>
						</#list>

					</div>
				</#if>
			</div>

		</div><#-- .row-fluid -->

</@f.form>

<#if step='options'>

	<#-- popup box for 'Set options' button -->
	<div class="modal hide fade" id="set-options-modal" tabindex="-1" role="dialog" aria-labelledby="set-options-label" aria-hidden="true">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
			<h3 id="set-options-label">Set options</h3>
		</div>
		<div class="modal-body">

		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
			<button class="btn btn-primary">Save options</button>
		</div>
	</div>

	<div class="modal hide fade" id="set-dates-modal" tabindex="-1" role="dialog" aria-labelledby="set-options-label" aria-hidden="true">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
			<h3>Set dates</h3>
		</div>
		<div class="modal-body">
		<#-- this form is never submitted, it is just here for styling -->
			<form class="form-horizontal dateTimePair dirty-check-ignore">
				<@spring.nestedPath path=commandName>
					<@form.row>
						<@form.label>Open date</@form.label>
						<@form.field>
							<@spring.bind path="defaultOpenDate">
								<input type="text" id="modal-open-date" name="openDate" class="date-time-picker startDateTime" value="${status.value}">
								<input class="endoffset" type="hidden" data-end-offset="1209600000" />
							</@spring.bind>
						</@form.field>
					</@form.row>

					<@form.labelled_row "defaultOpenEnded" "Open-ended">
						<#assign popoverText>
							<p>Check this box to mark the assignments as open-ended.</p>

							<ul>
								<li>Any close dates previously entered will have no effect.</li>
								<li>Allowing extensions and submission after the close date will have no effect.</li>
								<li>No close date will be shown to students.</li>
								<li>There will be no warnings for lateness, and no automatic deductions to marks.</li>
								<li>You will be able to publish feedback individually at any time.</li>
							</ul>
						</#assign>

						<label class="checkbox">
							<@f.checkbox path="defaultOpenEnded" id="modal-open-ended" />
							<a href="#" class="use-popover"
								data-title="Open-ended assignments"
								data-html="true"
								data-trigger="hover"
								data-content="${popoverText}"
								data-container="body"
							>What's this?</a>
						</label>
					</@form.labelled_row>

					<@form.row cssClass="has-close-date">
						<@form.label>Close date</@form.label>
						<@form.field>
							<@spring.bind path="defaultCloseDate">
								<input type="text" id="modal-close-date" name="closeDate" class="date-time-picker endDateTime" value="${status.value}">
							</@spring.bind>
						</@form.field>
					</@form.row>
				</@spring.nestedPath>
			</form>
		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Close</button>
			<button class="btn btn-primary">Set dates</button>
		</div>
	</div>

	<script type="text/javascript">
		// Give a heads up if you're about to navigate away from your progress
		jQuery(window).on('beforeunload.backattack', function() {
			return "If you leave this page without clicking either the Submit button or the Back button above it, you will lose your progress.";
		});

		// Disable the heads up when we submit the form through the proper means
		jQuery('form').on('submit', function() {
			jQuery(window).off('beforeunload.backattack');
		});
	</script>

</#if>

<script type="text/javascript">
	//<[![CDATA[
	<#include "batch_new_select_js.ftl" />
	//]]>

	jQuery('.slow-page-warning').hide('fast');
</script>

</#escape>
