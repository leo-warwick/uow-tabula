<#--
This must include all the fields that are included
in _feedback_fields.ftl, _submissions_fields.ftl and _options_fields.ftl
or else some values will get lost between requests.
TODO a way to not have to do these, or at least way to make it obvious when this hasn't been updated
-->

<#-- Outputs false as the value if the value is missing. -->
<#macro hiddenorfalse path>
	<@spring.bind path=path>
		<#local value=status.value!'false' />
		<#if value=''>
			<#local value="false" />
		</#if>
	<input type="hidden" name="${status.expression}" value="${value}" />
	</@spring.bind>
</#macro>

<@hiddenorfalse path="collectSubmissions" />
<@hiddenorfalse path="collectMarks" />
<#if !(ignoreQueueFeedbackForSits!false)>
	<@hiddenorfalse path="queueFeedbackForSits" />
</#if>
<@hiddenorfalse path="displayPlagiarismNotice" />
<@hiddenorfalse path="restrictSubmissions" />
<@hiddenorfalse path="allowLateSubmissions" />
<@hiddenorfalse path="allowResubmission" />
<@hiddenorfalse path="allowExtensions" />
<@hiddenorfalse path="extensionAttachmentMandatory" />
<@hiddenorfalse path="allowExtensionsAfterCloseDate" />
<@hiddenorfalse path="summative" />
<@hiddenorfalse path="includeInFeedbackReportWithoutSubmissions" />
<@hiddenorfalse path="automaticallyReleaseToMarkers" />
<@hiddenorfalse path="automaticallySubmitToTurnitin" />
<@f.hidden path="wordCountMin" />
<@f.hidden path="wordCountMax" />
<@f.hidden path="wordCountConventions" />
<@f.hidden path="minimumFileAttachmentLimit" />
<@f.hidden path="fileAttachmentLimit" />
<@f.hidden path="fileAttachmentTypes" />
<@f.hidden path="individualFileSizeLimit" />
<@f.hidden path="comment" />
<@f.hidden path="feedbackTemplate" />
<@f.hidden path="markingWorkflow" />