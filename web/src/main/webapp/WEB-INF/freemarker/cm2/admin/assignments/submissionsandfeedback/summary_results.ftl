<#import "*/coursework_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#escape x as x?html>
  <#--  use the university ID from feedback if possible  -->
  <#macro studentIdentifier user enhancedFeedback={}><#compress>
    <#if enhancedFeedback?? && enhancedFeedback.feedback??>
        ${enhancedFeedback.feedback.studentIdentifier}
    <#elseif user.warwickId??>
        ${user.warwickId}
    <#else>
        ${user.userId!}
    </#if>
  </#compress></#macro>

  <#function stage_incomplete markingStage enhancedFeedback="">
      <#if enhancedFeedback?has_content>
          <#return !enhancedFeedback.releasedToMarkers || !enhancedFeedback.isMarkedByStage(markingStage) />
      </#if>
      <#return false />
  </#function>

  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
  <@modal.modal id="feedback-modal"></@modal.modal>

  <div class="submission-feedback-results" data-popout="false">
    <#if (results.students?size > 0)>
      <table id="submission-feedback-progress"
             class="cm2-progress-table submission-feedback-list table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
        <thead class="fix-header">
        <tr>
          <th class="for-check-all"><input type="checkbox" class="collection-check-all" title="Select all/none" /></th>
          <#if alwaysSeeName || assignment.anonymity.equals(AssignmentAnonymity.NameAndID)>
            <th class="student-col sortable">First name</th>
            <th class="student-col sortable">Last name</th>
          </#if>

          <th class="student-col sortable">University ID</th>
          <#if assignment.showSeatNumbers>
            <th class="student-col sortable">Seat number</th>
          </#if>

          <th class="progress-col">Progress</th>
          <th class="action-col">Next action</th>
        </tr>
        </thead>
        <tbody>
        <#assign canProxy = can.do("Assignment.MarkOnBehalf", assignment) />
        <#macro details student>
          <#local coursework = student.coursework />
          <#if coursework.enhancedSubmission??>
            <#local enhancedSubmission = coursework.enhancedSubmission />
            <#local submission = enhancedSubmission.submission />
          </#if>
          <#if coursework.enhancedFeedback??>
            <#local enhancedFeedback = coursework.enhancedFeedback />
            <#local feedback = enhancedFeedback.feedback>
          </#if>
          <#if coursework.enhancedExtension??>
            <#local enhancedExtension=coursework.enhancedExtension />
          </#if>
          <#local identifier><@studentIdentifier student.user coursework.enhancedFeedback /></#local>
          <#local lateness><@components.lateness submission /></#local>

          <div class="details">
            <ul class="nav nav-tabs" role="tablist">
              <li role="presentation" class="active">
                <a href="#${identifier}-submission" aria-controls="${identifier}-submission" role="tab" data-toggle="tab">
                  Progress
                </a>
              </li>

              <#-- One tab for each stage in the workflow; previous stages are active, incomplete stages are not -->
              <#if assignment.cm2MarkingWorkflow??>
                <#list assignment.cm2MarkingWorkflow.markerStages as markingStage>
                  <#if markingStage_index gt 0>
                    <#local previousStageIncomplete = stage_incomplete(assignment.cm2MarkingWorkflow.markerStages[markingStage_index - 1], feedback) />
                  <#else>
                    <#local previousStageIncomplete = false />
                  </#if>
                  <#-- TAB-7183 if previous stage is incomplete, this stage should be considered incomplete -->
                  <#local incomplete = previousStageIncomplete || stage_incomplete(markingStage, feedback) />
                  <li role="presentation"<#if incomplete> class="disabled"</#if>>
                    <a<#if !incomplete> href="#${identifier}-${markingStage.name}" aria-controls="${identifier}-${markingStage.name}" role="tab" data-toggle="tab"</#if>>
                      ${markingStage.description}
                    </a>
                  </li>
                </#list>
              </#if>
            </ul>
            <div class="tab-content tab-content-equal-height">
              <div role="tabpanel" class="tab-pane active" id="${identifier}-submission">
                <@components.student_workflow_details assignment student canProxy />
              </div>

              <#-- One tab for each stage in the workflow; previous stages are active, incomplete stages are not -->
              <#if assignment.cm2MarkingWorkflow?? && feedback??>
                <#list assignment.cm2MarkingWorkflow.markerStages as markingStage>
                  <#if markingStage_index gt 0>
                    <#local previousStageIncomplete = stage_incomplete(assignment.cm2MarkingWorkflow.markerStages[markingStage_index - 1], feedback) />
                  <#else>
                    <#local previousStageIncomplete = false />
                  </#if>
                  <#-- TAB-7183 if previous stage is incomplete, this stage should be considered incomplete -->
                  <#local incomplete = previousStageIncomplete || stage_incomplete(markingStage, feedback) />
                  <div role="tabpanel" class="tab-pane" id="${identifier}-${markingStage.name}">
                    <#if mapGet(feedback.feedbackByStage, markingStage)??>
                      <#local markerFeedback = mapGet(feedback.feedbackByStage, markingStage) />
                      <@components.marker_feedback_summary
                      feedback=markerFeedback
                      stage=markingStage
                      currentStageIncomplete=incomplete />
                    </#if>
                  </div>
                </#list>
              </#if>
            </div>
          </div>
        </#macro>

        <#macro row student>
          <#local coursework = student.coursework />
          <#if coursework.enhancedSubmission??>
            <#local enhancedSubmission = coursework.enhancedSubmission />
            <#local submission = enhancedSubmission.submission />
          </#if>
          <#if coursework.enhancedFeedback??>
            <#local enhancedFeedback = coursework.enhancedFeedback />
            <#local feedback = enhancedFeedback.feedback>
          </#if>
          <#if coursework.enhancedExtension??>
            <#local enhancedExtension=coursework.enhancedExtension />
          </#if>
          <#local identifier><@studentIdentifier student.user coursework.enhancedFeedback /></#local>
          <#local lateness><@components.lateness submission /></#local>

          <tr class="itemContainer<#if !enhancedSubmission??> awaiting-submission</#if>" <#if enhancedSubmission?? && submission.suspectPlagiarised> data-plagiarised="true"</#if>
              data-contentid="${identifier}">
            <td class="check-col"><@bs3form.selector_check_row "students" student.user.userId /></td>

            <#if alwaysSeeName || assignment.anonymity.equals(AssignmentAnonymity.NameAndID)>
              <td class="student toggle-cell toggle-icon" data-profile="${identifier}">
                ${student.user.firstName!}
              </td>
              <td class="student toggle-cell">${student.user.lastName!}</td>
            </#if>

            <td class="id toggle-cell<#if !department.showStudentName> toggle-icon</#if>">
              ${identifier} <@pl.profile_link identifier />
            </td>

            <#if assignment.showSeatNumbers>
              <td class="student toggle-cell">
                ${assignment.getSeatNumber(student.user)!""}
              </td>
            </#if>

            <td class="progress-col content-cell toggle-cell">
              <dl style="margin: 0; border-bottom: 0;">
                <dt><@components.individual_stage_progress_bar student.stages?values assignment student.user /></dt>
                <dd style="display: none;" class="table-content-container" data-contentid="${identifier}">
                  <div id="content-${identifier}" class="content-container" data-contentid="${identifier}">
                    <@details student />
                  </div>
                </dd>
              </dl>
            </td>
            <td class="action-col">
              <#if student.nextStage?has_content>
                <@components.workflowMessage student.nextStage.actionCode assignment student.user />
              </#if>
            </td>
          </tr>
        </#macro>

        <#list results.students as student>
          <@row student />
        </#list>
        </tbody>
      </table>
    <#else>
      <p>There are no records for selected filter criteria.</p>
    </#if>
  </div>

  <script type="text/javascript" nonce="${nonce()}">
    (function ($) {
      var $overlapPercentageCheckbox = $('.dropdown-menu.filter-list input[type=checkbox][value="PlagiarismStatuses.WithOverlapPercentage"]');
      var checked = $overlapPercentageCheckbox.is(':checked');
      var $percentageOverlapDiv = $('div.plagiarism-filter');
      if (checked) {
        $percentageOverlapDiv.show();
      } else {
        $percentageOverlapDiv.hide();
      }

      $('.fixed-container').fixHeaderFooter();
      $('a.ajax-modal').ajaxModalLink();

      $('.use-popover').tabulaPopover();

      var $submissionFeedbackResultsTable = $(".submission-feedback-results table");
      $submissionFeedbackResultsTable.expandingTable();

      Coursework.initBigList();

      $('.submission-feedback-results').wideTables();
      $('.content-container').on('tabula.expandingTable.parentRowExpanded', function () {
        Coursework.equalHeightTabContent($(this));
      });
      $(window).on('id7:reflow', function () {
        Coursework.equalHeightTabContent($('.content-container'));
      });

      // We probably just grew a scrollbar, so let's trigger a window resize
      $(window).trigger('resize.ScrollToFixed');


    })(jQuery);
  </script>
</#escape>
