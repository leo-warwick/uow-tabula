<#import "*/modal_macros.ftlh" as modal />
<#import "*/cm2_macros.ftl" as cm2 />
<#--
first page of the form to setup a bunch of assignments from SITS.
-->
<#escape x as x?html>
  <#assign commandName="command"/>

  <#function route_function dept>
    <#local result><@routes.cm2.create_sitsassignments dept academicYear /></#local>
    <#return result />
  </#function>
  <@cm2.departmentHeader "Create assignments from SITS" department route_function academicYear />

  <#assign step=action!'select'/>

  <#assign actionUrl><@routes.cm2.create_sitsassignments department academicYear /></#assign>
  <@f.form method="post" id="batch-add-form" action=actionUrl modelAttribute=commandName>
    <#if step =='select'>
      <div class="alert alert-info slow-page-warning">
        <p>This page may take a few seconds to fully load, please wait&hellip;</p>
      </div>

      <p>Below are all the assessment components defined for this department in SITS.</p>

      <p>Use the checkboxes to select the assessment components you want to set up as assignments in Tabula. Some items, such as exams and Audit Only
        components, are deselected by default. However, you can reselect them if you want (for example, to publish feedback for an exam).</p>

      <p>One assignment is created for each assessment component you select. The assessment component name will be used as the assignment name. To change the
        assignment name, select the pencil icon to edit the name.</p>
    <#elseif step =='options'>
      <p>
        <button class="btn btn-default" data-action="refresh-select">Return to assignment selection</button>
      </p>

      <div id="batch-add-errors">
        <#include "batch_new_sits_validation.ftl" />
      </div>

      <p>
        You can change each assignment's options later, but it's a good idea to set some common options in bulk now.
      </p>

      <ol>
        <li>Set assignment options such as a feedback template, and set the open and close dates.</li>
        <li>Select the <strong>Create assignments</strong> button to finish setting up the assignments.</li>
      </ol>
    </#if>
    <input type="hidden" name="action" value="error" /><!-- this is changed before submit -->

    <#if department.children?size gt 0>
      <@bs3form.labelled_form_group path="includeSubDepartments" labelText="">
        <@bs3form.checkbox path="includeSubDepartments">
          <@f.checkbox path="includeSubDepartments" id="includeSubDepartments" /> Include modules in sub-departments
        </@bs3form.checkbox>
      </@bs3form.labelled_form_group>
    </#if>
    <#macro hidden_properties>
      <@f.hidden path="assessmentComponent" />
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
    <div class="fix-area">
      <#if step='options'>
        <div class="fix-header">
          <div id="options-buttons">
            <#-- options sets -->
            <a class="btn btn-default btn-default" id="set-options-button" data-target="#set-options-modal"
               href="<@routes.cm2.assignmentSharedOptions department/>">
              Set options
            </a>
            <a class="btn btn-default btn-default" id="set-dates-button" data-target="#set-dates-modal">
              Set dates
            </a>
            <#list command.optionsMap?keys as optionsId>
              <span class="options-button">
								<button class="btn btn-default" data-group="${optionsId}">
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
							</span>
            </#list>
          </div>
          <div style="margin-top: 10px">
            <span id="selected-count">0 selected</span>
            <span id="selected-deselect"><a href="#">Clear selection</a></span>
          </div>
        </div>
      </#if>
      <div class="assessment-component">
        <table class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers" id="batch-add-table">
          <thead>
          <tr>
            <th class="for-check-all"><input type="checkbox" checked="checked" class="collection-check-all use-tooltip" title="Select all/none"></th>
            <th>Module</th>
            <th><abbr tabindex="0" title="Component type" class="use-tooltip">Type</abbr></th>
            <th><abbr tabindex="0" title="Sequence" class="use-tooltip">Seq</abbr></th>
            <th><abbr tabindex="0" title="Occurrence/Cohort" class="use-tooltip">Occ</abbr></th>
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
                    ${item.assessmentComponent.moduleCode?upper_case}
                  </td>
                  <td class="selectable">
                    ${(item.assessmentComponent.assessmentType.value)!'A'}
                  </td>
                  <td class="selectable">
                    ${item.assessmentComponent.sequence}
                  </td>
                  <td class="selectable">
                    ${item.occurrence!'NONE'}
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

      <div class="fix-footer">
        <#if step='select'>
          <button class="btn btn-primary" data-action="options">Continue</button>
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
          <button id="batch-add-submit-button" class="btn btn-primary" data-action="submit">Create assignments</button>
        </#if>
      </div>
    </div>
  </@f.form>

  <#if step='options'>
  <#-- popup box for 'Set options' button -->
    <@modal.modal id="set-options-modal">
      <@modal.wrapper cssClass="modal-lg">
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
    </@modal.modal>
    <@modal.modal id="set-dates-modal">
      <@modal.wrapper>
        <@modal.header>
          <h3 class="modal-title">Set dates</h3>
        </@modal.header>
        <@modal.body>
          <@f.form  class="dateTimePair dirty-check-ignore" modelAttribute=commandName>
            <@bs3form.labelled_form_group path="defaultOpenDate" labelText="Open date">
              <div class="input-group">
                <input type="text" id="modal-open-date" name="openDate" class="form-control date-picker" value="${status.value}">
                <span class="input-group-addon"><i class="fa fa-calendar"></i></span>
              </div>
              <div class="help-block">
                Assignments will open at 9am on this date.
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
              <div class="help-block">
                Assignments will close at this time.
                <#if showIntro("assignment-openclose-date-restrictions", "anywhere")>
                <#assign introText>
                  <p>All assignments are required to close between ${assignmentCloseTimes} on a University working day.</p>

                  <p>Assignments open at 9am on the open date, which must also be a University working day.
                    This makes it more likely that the first submission attempts will be made while staff members are available to deal with any issues.</p>
                </#assign>
                <a href="#"
                   id="assignment-openclose-date-restrictions"
                   class="use-introductory"
                   data-hash="${introHash("assignment-openclose-date-restrictions", "anywhere")}"
                   data-title="Open and close date restrictions"
                   data-placement="bottom"
                   data-html="true"
                   aria-label="Help"
                   data-content="${introText}"><i class="fa fa-question-circle"></i></a>
              </#if>
              </div>
            </@bs3form.labelled_form_group>
          </@f.form>
        </@modal.body>
        <@modal.footer>
          <div class="submit-buttons">
            <button class="btn btn-primary">Save dates</button>
            <button class="btn btn-default" data-dismiss="modal">Close</button>
          </div>
        </@modal.footer>
      </@modal.wrapper>
    </@modal.modal>

    <script type="text/javascript" nonce="${nonce()}">
      // Give a heads up if you're about to navigate away from your progress
      jQuery(window).on('beforeunload.backattack', function () {
        return "If you leave this page, you will lose your progress.";
      });

      // Disable the heads up when we submit the form through the proper means
      jQuery('form').on('submit', function () {
        jQuery(window).off('beforeunload.backattack');
      });
    </script>
  </#if>
  <!-- </script> specifically added for functional test -->
  <@script "/static/js/assignment-batch-select.js" />

</#escape>
