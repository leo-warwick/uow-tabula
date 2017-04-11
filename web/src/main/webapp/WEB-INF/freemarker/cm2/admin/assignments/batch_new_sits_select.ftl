<#import "*/modal_macros.ftl" as modal />
<#--
first page of the form to setup a bunch of assignments from SITS.
-->
<#escape x as x?html>
	<#assign commandName="command"/>
	<#function route_function dept>
		<#local result><@routes.cm2.create_sitsassignments dept /></#local>
		<#return result />
	</#function>

	<@fmt.id7_deptheader "Setup assignments" route_function "for" />

	<#assign step=action!'select'/>

	<#assign actionUrl><@routes.cm2.create_sitsassignments department /></#assign>
	<@f.form method="post" id="batch-add-form" action=actionUrl commandName=commandName>
		<#if step =='select'>
			<div class="alert alert-info slow-page-warning">
				<p>This page may take a few seconds to fully load, please wait &hellip;</p>
			</div>
			<h2>Step 1 - choose which assignments to setup</h2>
			<div class="col-md-10">
				<p>Below are all of the assessment components defined for this department in SITS, the central system.</p>

				<p>The first thing to do is choose which ones you want to set up to use for assessment.
					Use the checkboxes on the left hand side to choose which ones you want to setup in the coursework submission system,
					and then click Next. Some items (such as exams and "Audit Only" components) have already been unchecked,
					but you can check them if you want (for example, if you want to publish feedback for an exam).
				</p>
		<#elseif step =='options'>
			<h2>Step 2 - choose options for assignments</h2>
			<div class="col-md-10">
				<div id="batch-add-errors">
					<#include "batch_new_sits_validation.ftl" />
				</div>
				<p>
					Now you need to choose how you want these assignments to behave, such as submission dates
					and resubmission behaviour.
				</p>
				<ul>
					<li>Select/unselect assignments using the checkboxes on the left.</li>
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

			<@bs3form.labelled_form_group path="academicYear" labelText="Academic year">
				<#if step="select">
					<@f.select path="academicYear" id="academicYearSelect" cssClass="form-control">
						<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
					</@f.select>
				<#else>
					<@f.hidden path="academicYear"/>
					<@f.hidden path="includeSubDepartments"/>
					<span class="form-control-static">
						<@spring.bind path="academicYear">${status.actualValue.label}</@spring.bind>
					</span>
				</#if>
			</@bs3form.labelled_form_group>
			<#if department.children?size gt 0>
				<@bs3form.labelled_form_group path="includeSubDepartments" labelText="">
					<@bs3form.checkbox path="includeSubDepartments">
						<@f.checkbox path="includeSubDepartments" id="includeSubDepartments" /> Include modules in sub-departments
					</@bs3form.checkbox>
				</@bs3form.labelled_form_group>
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
			<#list command.sitsAssignmentItems as item>
				<#if step != 'select' && !item.include>
					<@spring.nestedPath path="sitsAssignmentItems[${item_index}]">
						<@hidden_properties />
					</@spring.nestedPath>
				</#if>
			</#list>
			<div class = 'assessment-component'>
				<table class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers" id="batch-add-table">
					<thead>
						<tr>
							<th class="for-check-all"><input  type="checkbox" checked="checked" class="collection-check-all use-tooltip" title="Select all/none"> </th>
							<th>Module</th>
							<th><abbr title="Component type" class="use-tooltip">Type</abbr></th>
							<th><abbr title="Sequence" class="use-tooltip">Seq</abbr></th>
							<th><abbr title="Occurrence/Cohort" class="use-tooltip">Occ</abbr></th>
							<th>Component name</th>
							<#if step="options">
								<th></th>
								<th></th>
							</#if>
						</tr>
					</thead>
					<tbody>
						<#list command.sitsAssignmentItems as item>
							<@spring.nestedPath path="sitsAssignmentItems[${item_index}]">
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
											<#-- render all field errors for sitsAssignmentItems[x] -->
											<@bs3form.errors path="" />
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
					</tbody>
				</table>
				<#-- Hidden fields for items we unchecked in the first step, just to remember that we unchecked them -->
				<#list command.sitsAssignmentItems as item>
					<@spring.nestedPath path="sitsAssignmentItems[${item_index}]">
						<#if step!="select" && !item.include>
							<@f.hidden path="include" />
						</#if>
					</@spring.nestedPath>
				</#list>
			</div>
		</div>
		<div class="col-md-2">
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
					<button class="btn btn-large btn-default btn-block use-tooltip" data-container="body" data-action="refresh-select" title="Go back to change your assignment choices, without losing your work so far.">&larr; Back</button>
					<button id="batch-add-submit-button" class="btn btn-large btn-primary btn-block" data-action="submit">Submit</button>

					<div id="selected-count">0 selected</div>
					<div id="selected-deselect"><a href="#">Clear selection</a></div>
					<#-- options sets -->
					<a class="btn btn-default btn-default btn-block" id="set-options-button" data-target="#set-options-modal" href="<@routes.cm2.assignmentSharedOptions department />">
						Set options&hellip;
					</a>
					<a class="btn btn-default btn-default btn-block" id="set-dates-button" data-target="#set-dates-modal">
						Set dates&hellip;
					</a>
					<#list command.optionsMap?keys as optionsId>
						<div class="options-button">
							<button class="btn btn-default btn-block" data-group="${optionsId}">
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
	</@f.form>

	<#if step='options'>
		<#-- popup box for 'Set options' button -->
		<div class="modal fade" id="set-options-modal">
			<@modal.wrapper>
				<@modal.header>
					<h3 class="modal-title">Set options</h3>
				</@modal.header>
				<@modal.body></@modal.body>
				<@modal.footer>
					<div class="submit-buttons">
						<button class="btn btn-primary">Save options</button>
						<button class="btn btn-default" data-dismiss="modal">Close</button>
					</div>
				</@modal.footer>
			</@modal.wrapper>
		</div>
		<div class="modal fade" id="set-dates-modal">
			<@modal.wrapper>
				<@modal.header>
					<h3 class="modal-title">Set dates</h3>
				</@modal.header>
				<@modal.body>
					<@f.form  class="dateTimePair dirty-check-ignore" commandName=commandName>
						<@bs3form.labelled_form_group path="defaultOpenDate" labelText="Open date">
							<div class="input-group">
								<input type="text" id="modal-open-date" name="openDate" class="form-control date-time-minute-picker" value="${status.value}">
								<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
							</div>
						</@bs3form.labelled_form_group>
						<@bs3form.labelled_form_group path="defaultOpenEnded" labelText="">
							<@bs3form.checkbox path="defaultOpenEnded">
								<@f.checkbox path="defaultOpenEnded" id="modal-open-ended" />Open ended
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
								<@fmt.help_popover id="defaultOpenEndedInfo" content="${popoverText}" html=true/>
							</@bs3form.checkbox>
						</@bs3form.labelled_form_group>
						<@bs3form.labelled_form_group path="defaultCloseDate" labelText="Close date">
							<div class="input-group">
								<input type="text" id="modal-close-date" name="closeDate" class="form-control date-time-minute-picker" value="${status.value}">
								<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
							</div>
						</@bs3form.labelled_form_group>
					</@f.form>
				</@modal.body>
				<@modal.footer>
					<div class="submit-buttons">
						<button class="btn btn-primary">Set dates options</button>
						<button class="btn btn-default" data-dismiss="modal">Close</button>
					</div>
				</@modal.footer>
			</@modal.wrapper>
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
	<script type="text/javascript" src="/static/js/assignment-batch-select.js" />
</#escape>
