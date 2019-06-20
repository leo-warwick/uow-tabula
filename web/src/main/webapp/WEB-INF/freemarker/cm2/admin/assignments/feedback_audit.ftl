<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
<#import "feedback/_feedback_summary.ftl" as fs />
<#import "/WEB-INF/freemarker/coursework/admin/assignments/feedback/_feedback_summary.ftl" as cm1fs>

<#import "*/cm2_macros.ftl" as cm2/>

<#escape x as x?html>
  <@cm2.assignmentHeader "Feedback audit" assignment "for" />

  <h4>Student - ${student.fullName} (${student.warwickId!student.userId}
    ) <#if student.warwickId??><@pl.profile_link student.warwickId /><#else><@pl.profile_link student.userId /></#if></h4>
  <#if auditData.submission??>
    <#assign submission=auditData.submission />
    <#include "feedback/_submission_summary.ftl">
  </#if>

  <#if auditData.feedback??>

    <#if assignment.cm2Assignment>
      <#list auditData.feedback.completedFeedbackByStage?keys as stage>
        <#if mapGet(auditData.feedback.completedFeedbackByStage, stage)??>
          <@fs.feedbackSummary stage mapGet(auditData.feedback.completedFeedbackByStage, stage)/>
        </#if>
      </#list>
    <#else>
    <#-- For CM1 assignments we can't cycle through allFeedback as it may contained orphened rejected feedback -->
      <#if auditData.feedback.firstMarkerFeedback??>
        <div class="well">
          <@cm1fs.feedbackSummary auditData.feedback.firstMarkerFeedback isModerated true/>
          <@cm1fs.secondMarkerNotes auditData.feedback.firstMarkerFeedback isModerated />
        </div>
      </#if>
      <#if auditData.feedback.secondMarkerFeedback??>
        <div class="well">
          <@cm1fs.feedbackSummary auditData.feedback.secondMarkerFeedback isModerated true/>
          <@cm1fs.secondMarkerNotes auditData.feedback.secondMarkerFeedback isModerated />
        </div>
      </#if>
      <#if auditData.feedback.thirdMarkerFeedback??>
        <div class="well">
          <@cm1fs.feedbackSummary auditData.feedback.thirdMarkerFeedback isModerated true/>
          <@cm1fs.secondMarkerNotes auditData.feedback.thirdMarkerFeedback isModerated />
        </div>
      </#if>
    </#if>

    <#assign feedback = auditData.feedback />
    <#assign isSelf = false />

    <#if feedback.hasPrivateOrNonPrivateAdjustments>
      <div class="well">
        <div class="feedback-summary-heading">
          <h3>Adjustments</h3>
        </div>
        <div class="feedback-summary">
          <div class="feedback-details">
            <#list feedback.adminViewableAdjustments as adminViewableFeedback>
              <div class="adjustment alert alert-info">
                <div class="mark-grade">
                  <div>
                    <#if adminViewableFeedback.mark?has_content>
                      <div class="mg-label">Adjusted Mark:</div>
                      <div>
                        <span class="mark">${adminViewableFeedback.mark!""}%</span>
                        <span>%</span>
                      </div>
                    </#if>
                    <#if adminViewableFeedback.grade?has_content>
                      <div class="mg-label">Adjusted Grade:</div>
                      <div>
                        <span class="grade">${adminViewableFeedback.grade!""}</span>
                      </div>
                    </#if>
                  </div>
                </div>
                <div class="mark-grade">
                  <div>
                    <div class="mg-label">Adjustment made:</div>
                    <div>
                      <span><@fmt.date adminViewableFeedback.uploadedDate /></span>
                    </div>
                  </div>
                </div>
                <#if adminViewableFeedback.reason?has_content>
                  <div class="mark-grade">
                    <div>
                      <div class="mg-label">Reason for adjustment:</div>
                      <div>
                        <span>${adminViewableFeedback.reason!""}</span>
                      </div>
                    </div>
                  </div>
                </#if>
                <#if adminViewableFeedback.comments?has_content>
                  <div class="feedback-comments">
                    <h5>Adjustment comments</h5>
                    <p>${adminViewableFeedback.comments!""}</p>
                  </div>
                </#if>
              </div>
            </#list>
          </div>
        </div>
      </div>
    </#if>

    <#if feedback.released>
      <div class="well">
        <div class="feedback-summary-heading">
          <h3>Feedback delivered to student</h3>
          <#if feedback.releasedDate??><h5>Published on <@fmt.date feedback.releasedDate /></h5></#if>
        </div>
        <#include "../../submit/_assignment_feedbackdownload.ftl" />
      </div>
    </#if>

  </#if>

  <div id="profile-modal" class="modal fade profile-subset"></div>
</#escape>