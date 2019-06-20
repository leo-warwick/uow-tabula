<#if features.collectRatings && feedback.collectRatings && isSelf>
  <div id="feedback-rating-container" class="is-stackable">
    <!-- fallback for noscript -->
    <div style="padding:0.5em">
      <a target="_blank" href="<@routes.coursework.ratefeedback feedback />">Rate your feedback</a> (opens in a new window/tab)
    </div>
  </div>
</#if>

<#if feedback.latestMark?? || feedback.latestGrade??>
  <div class="mark-and-grade">
    <#if feedback.studentViewableAdjustments?has_content && feedback.latestMark??>
      <h3>Adjusted mark: ${feedback.latestMark}%</h3>
    <#elseif feedback.latestMark??>
      <h3>Mark: ${feedback.latestMark}%</h3>
    </#if>
    <#if feedback.studentViewableAdjustments?has_content && feedback.latestGrade??>
      <h3>Adjusted grade: ${feedback.latestGrade}</h3>
    <#elseif feedback.latestGrade??>
      <h3>Grade: ${feedback.latestGrade}</h3>
    </#if>
  </div>

  <#list feedback.studentViewableAdjustments as viewableFeedback>
    <#if viewableFeedback??>
      <div class="alert">
        <p>
          <strong>${viewableFeedback.reason}</strong> - An adjustment has been made to your final mark.
          <#if feedback.assignment.summative>
            The mark shown above will contribute to your final module mark.
          </#if>
        </p>
        <#if viewableFeedback.comments??><p>${viewableFeedback.comments}</p></#if>
        <p>Your marks before adjustment were:</p>

        <#if viewableFeedback_has_next>
          <#if feedback.studentViewableAdjustments[viewableFeedback_index +1].mark??>
            <div>Mark: ${feedback.studentViewableAdjustments[viewableFeedback_index +1].mark}%</div></#if>
          <#if feedback.studentViewableAdjustments[viewableFeedback_index +1].grade??>
            <div>Grade: ${feedback.studentViewableAdjustments[viewableFeedback_index +1].grade}</div></#if>
        <#else>
          <#if feedback.actualMark??>
            <div>Mark: ${feedback.actualMark}%</div>
          <#else>
            There was no mark before adjustment.
          </#if>
          <#if feedback.actualGrade??>
            <div>Grade: ${feedback.actualGrade}</div>
          <#else>
            There was no grade before adjustment.
          </#if>
        </#if>
      </div>
    </#if>
  </#list>
</#if>

<#if assignment.genericFeedback??>
  <div class="feedback-notes">
    <h3>General feedback on the assignment:</h3>
    <div class="feedback-notes-content">
      <p>${assignment.genericFeedbackFormattedHtml!""}</p>
    </div>
  </div>
</#if>
<#if feedback.comments??>
  <div class="feedback-notes">
    <h3>Feedback on
      <#if isSelf>
        your
      <#else>
        the student's
      </#if>
      submission</h3>
    <div class="feedback-notes-content">
      ${feedback.commentsFormattedHtml!""}
    </div>
  </div>
</#if>

<p>
  <#assign feedbackcount=feedback.attachments?size>
  <#-- Only offer a Zip if there's more than one file. -->
  <#if feedbackcount gt 1>
  <p>
    <#if isSelf>
      Your
    <#else>
      The student's
    </#if>
    feedback consists of ${feedback.attachments?size} files.</p>
  <p>
    <#assign zipDownloadUrl><#compress>
      <#if isSelf>
        <@routes.coursework.feedbackZip feedback />
      <#else>
        <@routes.coursework.feedbackZip_in_profile feedback />
      </#if>
    </#compress></#assign>

    <a class="btn btn-success" href="${zipDownloadUrl}"><i class="icon-gift"></i>
      Download all as a Zip file
    </a>
  </p>
  <p>Or download the attachments individually below.</p>
<#elseif feedbackcount gt 0>
  <p>
    <#if isSelf>
      Your
    <#else>
      The student's
    </#if>
    feedback file is available to download below.</p>
</#if>

<#if feedback.attachments?has_content>
  <ul class="file-list">
    <#list feedback.attachments as attachment>
      <li>
        <#assign attachmentDownloadUrl><#compress>
          <#if isSelf>
            <@routes.coursework.feedbackAttachment feedback attachment />
          <#else>
            <@routes.coursework.feedbackAttachment_in_profile feedback attachment />
          </#if>
        </#compress></#assign>

        <a class="btn<#if feedbackcount=1> btn-success</#if>" href="${attachmentDownloadUrl}"><i class="icon-file"></i>
          ${attachment.name}
        </a>
      </li>
    </#list>
  </ul>
</#if>

</p>

<#if feedback.hasOnlineFeedback || feedback.hasMarkOrGrade || feedback.comments?? || assignment.genericFeedback??>
  <a href="<@routes.coursework.feedbackPdf assignment=assignment feedback=feedback/>"> Download<#if feedback.attachments?has_content> additional</#if> feedback
    as a PDF file</a>
</#if>
