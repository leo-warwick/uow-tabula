<#import "*/mitcircs_components.ftlh" as components />
<#import "/WEB-INF/freemarker/modal_macros.ftlh" as modal />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#assign canManage = can.do("MitigatingCircumstancesSubmission.Manage", submission) />
<#assign isSelf = submission.student.universityId == user.universityId!'' />

<div class="mitcircs-review fix-area">
<div class="fix-header pad-when-fixed">
  <div class="btn-toolbar dept-toolbar mitcircs-pagination">
    <#if canManage>
        <#if pagination?? && panel??>
          <a href="<@routes.mitcircs.viewPanel submission.panel />" class="mitcircs-return-link"> Return to panel</a>
        <#else>
          <a href="<@routes.mitcircs.adminhome submission.department />" class="mitcircs-return-link"></i> Return to list of submissions</a>
        </#if>
    </#if>
    <#if pagination??>
    <div class="btn-group">
      <span class="mitcircs-pagination__count">${pagination.index} of ${pagination.total}</span>
      <#if pagination.previous??>
        <a class="btn btn-link use-tooltip" href="<@routes.mitcircs.reviewSubmissionPanel pagination.previous />" title="Previous submission 'MIT-${pagination.previous.key}' (type 'k')" data-container="body" data-placement="bottom" data-pagination="previous"><i class="fal fa-chevron-up"></i></a>
      <#else>
        <a tabindex="0" role="button" class="btn btn-link disabled use-tooltip" title="Not available - this is the first submission" data-container="body" data-placement="bottom"><i class="fal fa-chevron-up"></i></a>
      </#if>
      <#if pagination.next??>
        <a class="btn btn-link use-tooltip" href="<@routes.mitcircs.reviewSubmissionPanel pagination.next />" title="Next submission 'MIT-${pagination.next.key}' (type 'j')" data-container="body" data-placement="bottom" data-pagination="next"><i class="fal fa-chevron-down"></i></a>
      <#else>
        <a tabindex="0" role="button" class="btn btn-link disabled use-tooltip" title="Not available - this is the first submission" data-container="body" data-placement="bottom"><i class="fal fa-chevron-down"></i></a>
      </#if>
    </div>
    </#if>
  </div>

  <div class="deptheader">
    <h1>Mitigating circumstances submission MIT-${submission.key}</h1>
  </div>
</div>

<@modal.modal id="profile-modal" cssClass="profile-subset"></@modal.modal>
<section class="mitcircs-details">
  <div class="row">
    <div class="col-sm-6 col-md-7">
      <#if !isSelf>
        <@components.detail label="State" condensed=true>
          ${submission.state.description}
          <#if submission.state.entryName == "Outcomes Recorded">
            <#if submission.outcomesLastRecordedBy??>
              by ${submission.outcomesLastRecordedBy.fullName!submission.outcomesLastRecordedBy.userId}
            </#if>
            <#if submission.outcomesLastRecordedOn??>
              at <@fmt.date date=submission.outcomesLastRecordedOn />
            </#if>
          </#if>
          <#if submission.state.entryName == "Approved By Chair" >
            <#if submission.outcomesApprovedBy??>
                ${submission.outcomesApprovedBy.fullName!submission.outcomesApprovedBy.userId}
            </#if>
            <#if submission.outcomesApprovedOn??>
              at <@fmt.date date=submission.outcomesApprovedOn />
            </#if>
          </#if>
        </@components.detail>
      </#if>

      <@components.studentSummary submission />
      <@components.submissionSummary submission />

      <#if submission.contacted??>
        <#if submission.contacted>
          <@components.detail "Discussed submission with">
            <@components.enumListWithOther enumValues=submission.contacts otherValue=submission.contactOther!"" condensed=false />
          </@components.detail>
        <#else>
          <@components.detail "Reason for not discussing submission">
            ${submission.noContactReason}
          </@components.detail>
        </#if>
      <#else>
        <@components.detail "Discussed submission with">
          <span class="very-subtle">TBC</span>
        </@components.detail>
      </#if>
    </div>
    <#if !isSelf>
      <div class="col-sm-6 col-md-5 col-lg-4">
        <div class="row form-horizontal">
          <div class="col-sm-4 control-label">Actions</div>
          <div class="col-sm-8">
            <#if canManage>

              <#if submission.canConfirmSensitiveEvidence>
                <p><a href="<@routes.mitcircs.sensitiveEvidence submission /><#if pagination?? && panel??>?fromPanel=true</#if>" class="btn btn-default btn-block">Confirm sensitive evidence</a></p>
              </#if>

              <#if submission.state.entryName == "Submitted">
                <p><a href="<@routes.mitcircs.readyForPanel submission /><#if pagination?? && panel??>?fromPanel=true</#if>" class="btn btn-default btn-block" data-toggle="modal" data-target="#readyModal">Ready for panel</a></p>
              <#elseif submission.state.entryName == "Ready For Panel">
                <p><a href="<@routes.mitcircs.readyForPanel submission /><#if pagination?? && panel??>?fromPanel=true</#if>" class="btn btn-default btn-block" data-toggle="modal" data-target="#readyModal">Not ready for panel</a></p>
              </#if>
              <@modal.modal id="readyModal" role="dialog"><@modal.wrapper></@modal.wrapper></@modal.modal>

              <#if submission.canRecordAcuteOutcomes>
                <p><a href="<@routes.mitcircs.recordAcuteOutcomes submission />" class="btn btn-default btn-block">Record acute outcomes</a></p>
              </#if>
              <#if submission.canRecordOutcomes>
                <p><a href="<@routes.mitcircs.recordOutcomes submission /><#if pagination?? && panel??>?fromPanel=true</#if>" class="btn btn-default btn-block">Record panel outcomes</a></p>
              </#if>
              <#elseif submission.panel??>
                <p><a href="<@routes.mitcircs.viewPanel submission.panel />" class="btn btn-default btn-block"><i class="fal fa-long-arrow-left"></i> Return to panel</a></p>
              </#if>
              <#if isPanelChair>
                <@modal.modal id="approveModal" role="dialog"><@modal.wrapper></@modal.wrapper></@modal.modal>
                <#if submission.canApproveOutcomes>
                  <p><a href="<@routes.mitcircs.approveOutcomes submission />" class="btn btn-default btn-block" data-toggle="modal" data-target="#approveModal">Approve outcomes</a></p>
                <#elseif submission.state.entryName == "Approved By Chair">
                  <p><a href="<@routes.mitcircs.approveOutcomes submission />" class="btn btn-default btn-block" data-toggle="modal" data-target="#approveModal">Unapprove outcomes</a></p>
                </#if>
              </#if>
          </div>
        </div>
      </div>
    </#if>
  </div>

  <@components.section "Details">
    <#noautoesc>${submission.formattedReason}</#noautoesc>
  </@components.section>
  <@components.section "Affected assessments">
    <#if submission.affectedAssessments?has_content>
      <table class="table table-default">
        <thead>
        <tr>
          <th class="col-sm-2">Type</th>
          <th class="col-sm-3">Module</th>
          <th class="col-sm-5">Name</th>
          <th class="col-sm-2">Deadline / exam date</th>
        </tr>
        </thead>
        <tbody>
        <#list submission.affectedAssessments as assessment>
          <tr>
            <td><@components.assessmentType assessment /></td>
            <td><@components.assessmentModule assessment /></td>
            <td>${assessment.name}</td>
            <td>
              <#if assessment.deadline??>
                <@fmt.date date=assessment.deadline includeTime=false shortMonth=true excludeCurrentYear=true />
                <#if submission.startDate?? && assessment.deadline.isBefore(submission.startDate)>
                  <span tabindex="0" class="use-tooltip" title="Before start date"><i class="far fa-exclamation-triangle text-warning"></i></span>
                <#elseif submission.endDate?? && assessment.deadline.isAfter(submission.endDate)>
                  <span tabindex="0" class="use-tooltip" title="After end date"><i class="far fa-exclamation-triangle text-warning"></i></span>
                </#if>
              <#else>
                <span class="very-subtle">Unknown</span>
              </#if>
            </td>
          </tr>
        </#list>
        </tbody>
      </table>
    <#else>
      This issue doesn't affect any assessments
    </#if>
  </@components.section>
  <#if submission.attachments?has_content>
    <@components.section "Evidence">
      <@components.attachments submission />
    </@components.section>
  </#if>
  <#if submission.evidencePending>
    <@components.section "Pending evidence">
      <p>Due date: <@fmt.date date=submission.pendingEvidenceDue includeTime = false /></p>
      <#noautoesc>${submission.formattedPendingEvidence}</#noautoesc>
    </@components.section>
  </#if>
  <#if submission.sensitiveEvidenceComments?has_content>
    <@components.section "Sensitive evidence">
      <p>Seen by: ${submission.sensitiveEvidenceSeenBy.fullName} on <@fmt.date date=submission.sensitiveEvidenceSeenOn includeTime = false /></p>
      <#noautoesc>${submission.formattedSensitiveEvidenceComments}</#noautoesc>
    </@components.section>
  <#elseif submission.hasSensitiveEvidence>
    <@components.section "Sensitive evidence">
      <p>The student has declared that there is sensitive evidence related to this submission that they would rather discuss in person.</p>
    </@components.section>
  </#if>

  <@components.section "Supplemental information">
    <@components.detail "Reasonable adjustments">
      <#if reasonableAdjustments?has_content || reasonableAdjustmentsNotes?has_content>
        <#if reasonableAdjustments?has_content>
          <ul class="fa-ul">
            <#list reasonableAdjustments?sort_by('id') as reasonableAdjustment>
              <li><span class="fa-li"><i class="fal fa-check"></i></span>${reasonableAdjustment.description}</li>
            </#list>
          </ul>
        </#if>

        <#if reasonableAdjustmentsNotes?has_content>
          <#noautoesc>${formattedReasonableAdjustmentsNotes!''}</#noautoesc>
        </#if>
      <#else>
        <span class="very-subtle">None recorded</span>
      </#if>
    </@components.detail>

    <@components.detail "Other submissions">
      <@components.submissionTable submissions=otherMitigatingCircumstancesSubmissions actions=false panel=false />
    </@components.detail>

    <@components.detail "Extensions">
      <#if relevantExtensions?has_content>
        <table class="students table table-condensed">
          <thead>
            <tr>
              <th>Module</th>
              <th>Assignment</th>
              <th class="status-col">Status</th>
              <th class="duration-col duration-col-department-wide">Length of extension</th>
              <th class="deadline-col">Submission Deadline</th>
            </tr>
          </thead>
          <tbody>
            <#list relevantExtensions as extension>
              <tr>
                <td>${extension.assignment.module.code?upper_case}</td>
                <td>${extension.assignment.name}</td>
                <td>
                  <#if extension.awaitingReview>
                    <span class="label label-warning">Awaiting review</span>
                  <#elseif extension.approved>
                    <span class="label label-success">Approved</span>
                  <#elseif extension.rejected>
                    <span class="label label-important">Rejected</span>
                  </#if>
                </td>
                <td class="duration-col">
                  <#if (extension.duration > 0)>
                    <@fmt.p extension.duration "day" />
                  <#elseif (extension.requestedExtraDuration > 0) >
                    <@fmt.p extension.requestedExtraDuration "day" /> requested
                  <#else>
                    N/A
                  </#if>
                </td>
                <td class="deadline-col <#if extension.approved>approved<#else>very-subtle</#if>">
                  <#if extension.assignment.submissionDeadline(extension.usercode)?has_content>
                      <@fmt.date date=extension.assignment.submissionDeadline(extension.usercode) shortMonth=true excludeCurrentYear=true />
                  </#if>
                </td>
              </tr>
            </#list>
          </tbody>
        </table>
      <#else>
        <span class="very-subtle">None between the affected dates</span>
      </#if>
    </@components.detail>
  </@components.section>

  <#if !isSelf>
    <#if submission.panel??>
      <@components.section "Panel">
        <@components.panelDetails panel=submission.panel show_name=true />
      </@components.section>
    </#if>

    <#if (submission.state.entryName == "Outcomes Recorded" || submission.state.entryName == "Approved By Chair") && (canManage || can.do("MitigatingCircumstancesSubmission.ViewOutcomes", submission))>
      <@components.section "Outcomes">
        <#if submission.outcomeGrading?? && (canManage || can.do("MitigatingCircumstancesSubmission.ViewGrading", submission))>
          <@components.detail label="Mitigation grade" condensed=true>
            ${submission.gradingCode} <@fmt.help_popover id="grading-${submission.key}" content="${submission.outcomeGrading.helpText}" />
            <#if submission.outcomeGrading.entryName == "Rejected" && submission.rejectionReasons?has_content>
              &ndash;
              <@components.enumListWithOther submission.rejectionReasons submission.rejectionReasonsOther!"" />
            </#if>
          </@components.detail>
        </#if>

        <#if canManage>
          <@components.detail "Grading reasoning">
            <#noautoesc>${submission.formattedOutcomeReasons}</#noautoesc>
          </@components.detail>
        </#if>

        <#if submission.outcomeGrading.entryName != "Rejected">
          <#if submission.acute>
            <@components.detail label="Outcome" condensed=true>
              <#if submission.acuteOutcome??>
                ${submission.acuteOutcome.description}
              <#else>
                <span class="very-subtle">None</span>
              </#if>
            </@components.detail>

            <#if submission.affectedAssessments?has_content>
              <@components.detail "Affected assessments">
                <ul class="list-unstyled">
                  <#list submission.assessmentsWithAcuteOutcome as assessment>
                    <li><@components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}</li>
                  </#list>
                </ul>
              </@components.detail>
            </#if>
          <#else>
            <@components.detail "Recommendations to board">
              <ul class="list-unstyled">
                <#list submission.boardRecommendations as recommendation><#if !(recommendation.assessmentSpecific!false)>
                  <li>
                    <#if recommendation.entryName == "Other">${submission.boardRecommendationOther}<#else>${recommendation.description}</#if>
                    (all assessments)
                  </li>
                </#if></#list>

                <#list submission.affectedAssessments as assessment>
                  <#if assessment.boardRecommendations?has_content>
                    <li>
                      <@components.assessmentModule assessment=assessment formatted=false /> &mdash; ${assessment.name}
                      <ul>
                        <#list assessment.boardRecommendations as recommendation>
                          <li>
                            <#if recommendation.entryName == "Other">${submission.boardRecommendationOther}<#else>${recommendation.description}</#if>
                          </li>
                        </#list>
                      </ul>
                    </li>
                  </#if>
                </#list>
              </ul>
            </@components.detail>

            <#if canManage>
              <@components.detail "Comments for board">
                <#noautoesc>${submission.formattedBoardRecommendationComments}</#noautoesc>
              </@components.detail>
            </#if>
          </#if>
        </#if>
        <#if isPanelChair>
            <@modal.modal id="approveModal" role="dialog"><@modal.wrapper></@modal.wrapper></@modal.modal>
            <#if submission.canApproveOutcomes>
              <p><a href="<@routes.mitcircs.approveOutcomes submission />" class="btn btn-default" data-toggle="modal" data-target="#approveModal">Approve outcomes</a></p>
            <#elseif submission.state.entryName == "Approved By Chair">
              <p><a href="<@routes.mitcircs.approveOutcomes submission />" class="btn btn-default" data-toggle="modal" data-target="#approveModal">Unapprove outcomes</a></p>
            </#if>
        </#if>
      </@components.section>
    </#if>
  </#if>

  <#if canManage>
    <#assign notesUrl><@routes.mitcircs.notes submission /></#assign>
    <@components.asyncSection "notes" "Notes" notesUrl />

    <#if !submission.draft && !submission.withdrawn>
      <#assign messageUrl><@routes.mitcircs.messages submission /></#assign>
      <@components.asyncSection "messages" "Messages" messageUrl />
    </#if>
  </#if>
</section>
</div>
