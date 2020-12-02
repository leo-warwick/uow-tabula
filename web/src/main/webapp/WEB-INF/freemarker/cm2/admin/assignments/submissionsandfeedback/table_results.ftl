<#import "*/coursework_components.ftl" as components />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />

<#escape x as x?html>
  <@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
  <@modal.modal id="feedback-modal"></@modal.modal>
  <#if results.students??>
    <#function hasSubmissionOrFeedback students>
      <#local result = [] />
      <#list students as student>
        <#if student.coursework.enhancedSubmission?? || student.coursework.enhancedFeedback??>
          <#local result = result + [student] />
        </#if>
      </#list>
      <#return result />
    </#function>

    <#--  use the university ID from feedback if possible  -->
    <#macro studentIdentifier user coursework><#compress>
        <#if coursework.enhancedFeedback??>${coursework.enhancedFeedback.feedback.studentIdentifier}<#elseif user.warwickId??>${user.warwickId}<#else>${user.userId!}</#if>
    </#compress></#macro>

    <div class="submission-feedback-results" data-popout="false">
      <#if (results.students?size > 0)>
        <table id="submission-feedback-info"
               class="submission-feedback-list table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers">
          <colgroup class="student">
            <col class="checkbox" />
            <#if department.showStudentName>
              <col class="student-info" />
              <col class="student-info" />
            </#if>
            <col class="student-info" />
            <#if assignment.showSeatNumbers>
              <col class="student-info" />
            </#if>
          </colgroup>

          <colgroup class="submission">
            <col class="files" />
            <col class="submitted" />
            <col class="status" />
            <#assign submissionColspan=3 />

            <#if assignment.wordCountField??>
              <#assign submissionColspan=submissionColspan+1 />
              <col class="word-count" />
            </#if>

            <#if assignment.cm2MarkingWorkflow??>
              <#assign submissionColspan=submissionColspan+results.workflowMarkers?size />
              <#list results.workflowMarkers as marker_col>
                <col class="${marker_col}" />
              </#list>
              <#if assignment.hasModeration>
                <col class="wasModerated" />
              </#if>
            </#if>
          </colgroup>

          <#if results.hasOriginalityReport>
            <colgroup class="plagiarism">
              <col class="report" />
            </colgroup>
          </#if>

          <colgroup class="feedback">
            <#assign feedbackColspan=4 />

            <col class="files" />
            <col class="uploaded" />
            <#if assignment.collectMarks>
              <#assign feedbackColspan=feedbackColspan+2 />
              <col class="feedback-mark" />
              <col class="feedback-grade" />
            </#if>
            <col class="viewFeedback" />
            <col class="status" />
          </colgroup>

          <thead>
          <#-- TAB-5807 - make the first header rows from TDs so that the tablesorter plugin can cope -->
          <tr>
            <td class="for-check-all"><input type="checkbox" class="collection-check-all" title="Select all/none" /></td>

            <#assign studentColspan=1/>
            <#if department.showStudentName>
              <#assign studentColspan=studentColspan+2/>
            </#if>
            <#if assignment.showSeatNumbers>
              <#assign studentColspan=studentColspan+1/>
            </#if>

            <td colspan="${studentColspan?c}">Student</td>

            <td class="submission" colspan="${submissionColspan?c}">
              Submission
            </td>

            <#if results.hasOriginalityReport>
              <td class="plagiarism">Plagiarism</td>
            </#if>

            <td class="feedback" colspan="${feedbackColspan?c}">
              Feedback
            </td>
          </tr>
          <tr>
            <th class="student"></th>
            <#if department.showStudentName>
              <th class="student sortable">First name</th>
              <th class="student sortable">Last name</th>
            </#if>
            <th class="student sortable">University ID</th>
            <#if assignment.showSeatNumbers>
              <th class="student sortable">Seat number</th>
            </#if>

            <th class="submission">Files</th>
            <th class="submission sortable" data-sorter="customdate">Submitted</th>
            <th class="submission sortable">Status</th>
            <#if assignment.wordCountField??>
              <th class="submission sortable" title="Declared word count">Words</th>
            </#if>
            <#if assignment.cm2MarkingWorkflow??>
              <#assign submissionColspan=submissionColspan+results.workflowMarkers?size />
              <#list results.workflowMarkers as marker_col>
                <th class="submission sortable">${marker_col}</th>
              </#list>
              <#if assignment.hasModeration>
                <th class="wasModerated sortable">Was moderated</th>
              </#if>
            </#if>

            <#if results.hasOriginalityReport>
              <th class="plagiarism sortable">Report</th>
            </#if>

            <th class="feedback sortable">Files</th>
            <th class="feedback sortable" data-sorter="customdate">Updated</th>
            <#if assignment.collectMarks>
              <th class="feedback sortable" data-sorter="percent">Mark</th>
              <th class="feedback sortable">Grade</th>
            </#if>
            <th class="feedback">Summary</th>
            <th class="feedback sortable">Status</th>
          </tr>
          </thead>

          <tbody>
          <#macro row submissionfeedbackinfo>
            <#local coursework=submissionfeedbackinfo.coursework>
            <#if coursework.enhancedSubmission??>
              <#local enhancedSubmission=coursework.enhancedSubmission>
              <#local submission=enhancedSubmission.submission>
            </#if>
            <#if coursework.enhancedFeedback??>
              <#local enhancedFeedback=coursework.enhancedFeedback>
            </#if>
            <#if coursework.enhancedExtension??>
              <#local enhancedExtension=coursework.enhancedExtension>
            </#if>
            <#local lateness><@components.lateness submission /></#local>

            <tr class="itemContainer<#if !enhancedSubmission??> awaiting-submission</#if>" <#if enhancedSubmission?? && submission.suspectPlagiarised> data-plagiarised="true" </#if> >
              <td><@bs3form.selector_check_row "students" submissionfeedbackinfo.user.userId /></td>
              <#if department.showStudentName>
                <td class="student">${submissionfeedbackinfo.user.firstName!}</td>
                <td class="student">${submissionfeedbackinfo.user.lastName!}</td>
              </#if>
              <td class="id">
                <#local identifier><@studentIdentifier submissionfeedbackinfo.user coursework /></#local>
                ${identifier}
                <@pl.profile_link identifier />
              </td>

              <#if assignment.showSeatNumbers>
                <td class="id">${assignment.getSeatNumber(submissionfeedbackinfo.user)!""}</td>
              </#if>

              <td class="files">
                <#if submission??>
                  <@components.submission_attachments_link submission "1 file" "${submission.allAttachments?size} files" />
                </#if>
              </td>
              <#if submission?? && submission.submittedDate??>
                <td class="submitted" data-datesort="${submission.submittedDate.millis?c!''}">
                <span tabindex="0" class="date tabula-tooltip" data-title="${lateness!''}">
                  <@fmt.date date=submission.submittedDate seconds=true capitalise=true shortMonth=true split=true />
                </span>
                </td>
              <#else>
                <td class="submitted"></td>
              </#if>
              <td class="submission-status">
                <#-- Markable - ignore placeholder submissions -->
                <#if assignment.isReleasedForMarking(submissionfeedbackinfo.user.userId)>
                  <span class="label label-info">Markable</span>
                </#if>
                <#if submission??>
                <#-- Downloaded -->
                  <#if enhancedSubmission.downloaded>
                    <span class="label label-info">Downloaded</span>
                  </#if>
                </#if>
                <@components.submission_status submission coursework.enhancedExtension coursework.enhancedFeedback submissionfeedbackinfo />
              </td>
              <#if assignment.wordCountField??>
                <td class="word-count">
                  <#if submission?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
                    ${submission.valuesByFieldName[assignment.defaultWordCountName]?number}
                  </#if>
                </td>
              </#if>
              <#if assignment.cm2MarkingWorkflow??>
                <#if enhancedFeedback??>
                  <#local feedback=enhancedFeedback.feedback />
                  <#list results.workflowMarkers as markerRole>
                    <#local markerUser=feedback.feedbackMarkerByAllocationName(markerRole)! />
                    <td>
                      <#if markerUser?has_content>
                        ${markerUser.fullName}
                      </#if>
                    </td>
                  </#list>
                  <#if assignment.hasModeration><td class="wasModerated">${feedback.wasModerated?then("Yes", "No")}</td></#if>
                <#else>
                  <#list results.workflowMarkers as markerRole>
                    <td></td>
                  </#list>
                </#if>
              </#if>
              <#if results.hasOriginalityReport>
                <td class="originality-report">
                  <#if submission??>
                    <#list submission.allAttachments as attachment>
                      <!-- Checking originality report for ${attachment.name} ... -->
                      <#if attachment.turnitinResultReceived || attachment.turnitinCheckInProgress>
                        <@components.originalityReport attachment />
                      </#if>
                    </#list>
                  </#if>
                </td>
              </#if>

              <td class="download">
                <#if enhancedFeedback??>
                  <#local attachments=enhancedFeedback.feedback.attachments />
                  <#if attachments?size gt 0>
                    <@components.studentFeedbackDownload enhancedFeedback.feedback />
                  </#if>
                </#if>
              </td>
              <#if enhancedFeedback?? && !enhancedFeedback.feedback.placeholder>
                <td class="uploaded" data-datesort="${enhancedFeedback.feedback.updatedDate.millis?c!''}">
                  <@fmt.date date=enhancedFeedback.feedback.updatedDate seconds=true capitalise=true shortMonth=true split=true />
                </td>
              <#else>
                <td class="uploaded"></td>
              </#if>

              <#if assignment.collectMarks>
                <td class="feedback-mark">
                  <#if enhancedFeedback??>
                    <#local feedback = enhancedFeedback.feedback>
                    <#if feedback.actualMark??>${feedback.actualMark}%</#if>
                    <#if feedback.hasPrivateOrNonPrivateAdjustments>
                      (Adjusted to ${feedback.latestMark}%)
                    </#if>
                  </#if>
                </td>
                <td class="grade">
                  <#if enhancedFeedback??>
                    <#local feedback = enhancedFeedback.feedback>
                    ${(feedback.actualGrade)!''}
                    <#if feedback.hasPrivateOrNonPrivateAdjustments && feedback.latestGrade??>
                      (Adjusted to ${feedback.latestGrade})
                    </#if>
                  </#if>
                </td>
              </#if>

              <td>
                <#if enhancedFeedback??>
                  <a href="<@routes.cm2.feedbackSummary assignment submissionfeedbackinfo.user.userId/>"
                     class="ajax-modal"
                     data-target="#feedback-modal">
                    View
                  </a>
                </#if>
              </td>

              <td class="feedbackReleased">
                <#if enhancedFeedback??>
                  <#if enhancedFeedback.feedback.released>
                    <#if enhancedFeedback.downloaded><span class="label label-success">Downloaded</span>
                    <#else><span class="label label-info">Published</span>
                    </#if>
                  </#if>

                  <#assign queueSitsUploadEnabled=(features.queueFeedbackForSits && department.uploadCourseworkMarksToSits) />
                  <#if queueSitsUploadEnabled>
                      <#if enhancedFeedback.feedbackForSits??>
                          <#assign feedbackForSits=enhancedFeedback.feedbackForSits />
                          <#assign feedbackForSitsStatus=feedbackForSits.status />
                          <#assign sitsWarning = feedbackForSits.dateOfUpload?? && feedbackForSitsStatus.code != "uploadNotAttempted" && (
                          (feedbackForSits.actualMarkLastUploaded!0) != (enhancedFeedback.feedback.latestMark!0) || (feedbackForSits.actualGradeLastUploaded!"") != (enhancedFeedback.feedback.latestGrade!"")
                          ) />
                          <#if feedbackForSitsStatus.code == "failed">
                            <a href="<@routes.cm2.checkSitsUpload enhancedFeedback.feedback />" target="_blank">
                          <span tabindex="0" style="cursor: pointer;" class="label label-danger use-tooltip"
                                title="There was a problem uploading to SITS. Click to try and diagnose the problem.">
                          ${feedbackForSitsStatus.description}
                          </span><#--
                        --></a>
                          <#elseif sitsWarning>
                            <span tabindex="0" class="label label-danger use-tooltip"
                                  title="The mark or grade uploaded differs from the current mark or grade. You will need to upload the marks to SITS again.">
                      ${feedbackForSitsStatus.description}
                        </span>
                          <#elseif feedbackForSitsStatus.code == "successful">
                            <span class="label label-success">${feedbackForSitsStatus.description}</span>
                          <#else>
                            <span class="label label-info">${feedbackForSitsStatus.description}</span>
                          </#if>
                      <#else>
                        <span class="label label-info">Not queued for SITS upload</span>
                      </#if>
                  </#if>
                </#if>
              </td>
            </tr>
          </#macro>

          <#list results.students as submissionfeedbackinfo>
            <@row submissionfeedbackinfo />
          </#list>
          </tbody>
        </table>
        <#assign users=[] />
        <#list results.students as student>
          <#assign users = users + [student.user] />
        </#list>
        <p><@fmt.bulk_email_students users /></p>
      <#else>
        <p>There are no records for selected filter criteria.</p>
      </#if>
    </div>

  </#if>

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
      $('.use-popover').tabulaPopover({trigger: 'click focus', container: 'body'});

      // add a custom parser for date columns - works from a data-datesort value that holds the date in millis
      $.tablesorter.addParser({
        id: 'customdate',
        is: function (s, table, cell, cellIndex) {
          return false; /*return false so this parser is not auto detected*/
        },
        format: function (s, table, cell, cellIndex) {
          var $cell = $(cell);
          return $cell.attr('data-datesort') || s;
        },
        parsed: false,
        type: 'numeric'
      });

      var $submissionFeedbackResultsTable = $(".submission-feedback-results table");
      $submissionFeedbackResultsTable.sortableTable({
        textExtraction: function (node) {
          var $el = $(node);
          if ($el.hasClass('originality-report')) {
            var $tooltip = $el.find('.similarity-tooltip').first();
            if ($tooltip.length) {
              return $tooltip.text().substring(0, $tooltip.text().indexOf('%'));
            } else {
              return '0';
            }
          } else if ($el.hasClass('word-count')) {
            return $el.text().trim().replace(',', '');
          } else {
            return $el.text().trim();
          }
        }
      });

      Coursework.initBigList();

      $('.submission-feedback-results').wideTables();
      // We probably just grew a scrollbar, so let's trigger a window resize
      $(window).trigger('resize.ScrollToFixed');

    })(jQuery);
  </script>
</#escape>
