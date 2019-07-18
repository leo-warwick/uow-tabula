<#escape x as x?html>
<#import "*/coursework_components.ftl" as components />

<#if command.submission?has_content>
  <#assign submission = command.submission />
  <ul class="list-unstyled">

    <li><strong><@spring.message code=command.submissionState />:</strong><@components.submission_details submission /></li>

    <#if assignment.wordCountField?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
      <li><strong>Word count:</strong> ${submission.valuesByFieldName[assignment.defaultWordCountName]?number}</li>
    </#if>

    <li>
      <strong>Plagiarism check:</strong>
      <#if submission.hasOriginalityReport>
        <@compress single_line=true>
          <@fmt.p submission.attachmentsWithOriginalityReport?size "file" /><#if submission.suspectPlagiarised>, marked as plagiarised<#elseif submission.investigationCompleted>, plagiarism investigation completed</#if>
        </@compress>
        <div class="originality-reports">
          <#list submission.attachmentsWithOriginalityReport as attachment>
            <@components.originalityReport attachment />
          </#list>
        </div>
      <#else>
        This submission has not been checked for plagiarism
      </#if>
    </li>

    <#if features.disabilityOnSubmission && command.disability??>
      <li>
        <strong>Disability disclosed:</strong>
        <a href="#" class="use-popover cue-popover" id="popover-disability" data-html="true"
           data-container="body"
           data-content="<p>This student has chosen to make the marker of this submission aware of their disability and for it to be taken it into consideration. This student has self-reported the following disability code:</p><div class='well'><h6>${command.disability.code}</h6><small>${(command.disability.sitsDefinition)!}</small></div>"
        >
          See details
        </a>
      </li>
    </#if>
  </ul>
<#else>
  <#if command.assignment.openEnded>
    <span>This student has not submitted yet.</span>
  <#else>
    <#assign wasIs><#if command.assignment.isClosed() && !command.assignment.isWithinExtension(command.student)>was<#else>is</#if></#assign>
    <#assign dueDate = command.assignment.submissionDeadline(command.student) />
    <span>This student has not submitted yet. Their submission ${wasIs} due at <@fmt.date date=dueDate capitalise=true shortMonth=true /></span>
  </#if>
</#if>
<#if command.extension?has_content>
  <ul class="list-unstyled">
    <li>
      <strong>Extension:</strong> <@spring.message code=command.extensionState /><#if command.extensionDate??> for <@fmt.date date=command.extensionDate capitalise=true shortMonth=true /></#if>
    </li>
  </ul>
</#if>
</#escape>
