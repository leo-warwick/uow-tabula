<#import "*/coursework_components.ftl" as components />

<div class="well">
  <h3>Submission</h3>

  <div class="labels">
    <#if submission.late>
      <span tabindex="0" class="label label-danger use-tooltip" title="<@components.lateness submission />" data-container="body">Late</span>
    <#elseif  submission.authorisedLate>
      <span tabindex="0" class="label label-info use-tooltip" title="<@components.lateness submission />" data-container="body">Within Extension</span>
    </#if>

    <#if submission.suspectPlagiarised>
      <span tabindex="0" class="label label-danger use-tooltip" title="Suspected of being plagiarised" data-container="body">Plagiarism suspected</span>
    <#elseif submission.investigationCompleted>
      <span tabindex="0" class="label label-info use-tooltip" title="No evidence of plagiarism was found" data-container="body">Plagiarism investigation completed</span>
    </#if>

    <#if features.disabilityOnSubmission && command.disability??>
      <a class="use-popover white-text-decoration" id="popover-disability" data-html="true"
         tabindex="0" role="button"
         data-original-title="Disability disclosed"
         data-trigger="click focus"
         data-container="body"
         data-content="<p>This student has chosen to make the marker of this submission aware of their disability and for it to be taken it into consideration. This student has self-reported the following disability code:</p><div class='well'><h6>${command.disability.code}</h6><small>${(command.disability.sitsDefinition)!}</small></div>"
      >
        <span class="label label-info">Disability disclosed</span>
      </a>
    </#if>

    <#if command.reasonableAdjustmentsDeclared!false>
        <@components.reasonable_adjustment_declaration/>
    </#if>
  </div>

  <div>
    <@spring.message code=command.submissionState /> <@components.submission_details command.submission />

    <#list submission.allAttachments as attachment>
      <!-- Checking originality report for ${attachment.name} ... -->
      <#if attachment.turnitinResultReceived || attachment.turnitinCheckInProgress>
        <@components.originalityReport attachment />
      </#if>
    </#list>
    <#-- Assignment may be openended -->
    <#if submission.deadline??>
      <br />
      Due date: <@fmt.date date=submission.deadline capitalise=true shortMonth=true />
    </#if>
  </div>
</div>
