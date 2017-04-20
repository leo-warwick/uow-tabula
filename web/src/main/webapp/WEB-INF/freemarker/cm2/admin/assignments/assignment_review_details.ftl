<#import "*/assignment_components.ftl" as components />
<#escape x as x?html>

<div class="deptheader">
    <h1>Review assignment</h1>

    <h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<@components.assignment_wizard 'review' assignment.module true assignment/>
    <div class="form-group">
		<#assign detailsUrl><@routes.cm2.editassignmentdetails assignment /></#assign>

		<@review_details_header 'Assignment details' detailsUrl />
		<@review_details 'Assignment title' assignment.name />
		<@review_date_fld_details 'Open date' assignment.openDate />
		<@review_date_fld_details 'Open ended reminder date' assignment.openEndedReminderDate />
        <!--TODO- Integrate workflowCategory fields after assignment workflow impl - may be add a method to workflowCategory  that could be called here-->
    </div>


    <div class="form-group">
		<#assign feedbackDetailsUrl><@routes.cm2.assignmentfeedback assignment 'edit' /></#assign>
		<@review_details_header 'Feedback details' feedbackDetailsUrl />
		<#if (assignment.feedbackTemplate.name)??>
		<#assign feedbackTemplate = assignment.feedbackTemplate.name />
	<#else>
		<#assign feedbackTemplate = '' />
	</#if>
		<@review_details 'Feedback template' feedbackTemplate />
		<@review_details 'Automatically release to markers when assignment closes or after plagiarism check' sharedPropertiesForm.automaticallyReleaseToMarkers?string('Yes','No') />
		<@review_details 'Collect marks' sharedPropertiesForm.collectMarks?string('Yes','No') />
		<@review_details 'Credit bearing' sharedPropertiesForm.summative?string('Summative','Formative') />
		<@review_details 'Dissertation' sharedPropertiesForm.dissertation?string('Yes','No') />
    </div>

    <div class="form-group">
		<#assign studentDetailsUrl><@routes.cm2.assignmentstudents assignment 'edit' /></#assign>
		<@review_details_header 'Student details' studentDetailsUrl />
        <div><label class="review-label">Total number of students:</label> ${membershipInfo.totalCount} enrolled
			<#if membershipInfo.excludeCount gt 0 || membershipInfo.includeCount gt 0>
                <span class="very-subtle">(${membershipInfo.sitsCount} from SITS<#if membershipInfo.usedExcludeCount gt 0> after ${membershipInfo.usedExcludeCount} removed manually</#if><#if membershipInfo.usedIncludeCount gt 0>, plus ${membershipInfo.usedIncludeCount} added manually</#if>)</span>
			<#else>
                <span class="very-subtle">from SITS</span>
			</#if>
        </div>
    </div>

    <div class="form-group">
		<#assign markerDetailsUrl><@routes.cm2.assignmentmarkers assignment 'edit' /></#assign>
		<@review_details_header 'Marker details' markerDetailsUrl />
        <!--TODO- Integrate this after assignment marker impl-->
    </div>

    <div class="form-group">
		<#assign submissionDetailsUrl><@routes.cm2.assignmentsubmissions assignment 'edit' /></#assign>
		<@review_details_header 'Submission details' submissionDetailsUrl />
		<@review_details 'Collect submissions' sharedPropertiesForm.collectSubmissions?string('Yes','No') />
		<@review_details 'Automatically check submissions for plagiarism' sharedPropertiesForm.automaticallySubmitToTurnitin?string('Yes','No') />
		<@review_details 'Show plagiarism notice' sharedPropertiesForm.displayPlagiarismNotice?string('Yes','No') />
		<@review_details 'Submission scope' sharedPropertiesForm.restrictSubmissions?string('Only allow students enrolled on this assignment to submit coursework','Allow anyone with a link to the assignment page to submit coursework') />
		<@review_details 'Resubmissions' sharedPropertiesForm.allowResubmission?string('Yes','No') />
		<@review_details 'Extension requests' sharedPropertiesForm.allowExtensions?string('Yes','No') />

    </div>

    <div class="form-group">
		<#assign optionDetailsUrl><@routes.cm2.assignmentoptions assignment 'edit' /></#assign>
		<@review_details_header 'Options details' optionDetailsUrl />
		<@review_details 'Minimum attachments per submission' sharedPropertiesForm.minimumFileAttachmentLimit?string />
		<@review_details 'Maximum attachments per submission' sharedPropertiesForm.fileAttachmentLimit?string />
		<#assign fileTypes =  (sharedPropertiesForm.fileAttachmentTypes)?join(", ")/>
		<@review_details 'Attachment file types' fileTypes />
		<@review_details label_info='Maximum file size' assignment_property=(sharedPropertiesForm.individualFileSizeLimit!'')?string defaultValue='None'/>
		<@review_details label_info='Minimum word count' assignment_property=(sharedPropertiesForm.wordCountMin!'')?string defaultValue='None'/>
		<@review_details label_info='Maximum word count' assignment_property=(sharedPropertiesForm.wordCountMax!'')?string defaultValue='None'/>
		<@review_details label_info='Word count conventions' assignment_property=sharedPropertiesForm.wordCountConventions defaultValue='None'/>
        <div class="row">
            <div class="col-md-3"><label class="review-label">Text to show on submission form:</label></div>
            <div class="col-md-9">
				<#if sharedPropertiesForm.comment??><pre class="review-dtls"><span>${sharedPropertiesForm.comment}</pre><#else>None</#if>
            </div>
        </div>
    </div>
    <div class="fix-footer">
        <a class="btn btn-default" href="<@routes.cm2.home />">Confirm</a>
    </div>
</div>

	<#macro review_details_header header_info tab_link>
    <div class="row">
        <div class="col-md-3"><h2>${header_info}</h2></div>
        <div class="col-md-2"><a href="${tab_link}">Edit details</a></div>
    </div>
	</#macro>

	<#macro review_details label_info assignment_property='' defaultValue=''>
    <div><label class="review-label">${label_info}:</label> <#if assignment_property != ''>${assignment_property}<#else>${defaultValue}</#if></div>
	</#macro>
	<#macro review_date_fld_details label_info assignment_property=''>
    <div><label class="review-label">${label_info}:</label> <#if assignment_property != ''><@fmt.date assignment_property /></#if></div>
	</#macro>

</#escape>

<style type="text/css">
    pre.review-dtls {
        border:none;
        background-color: transparent;
        font-size: inherit;
        font-family: inherit;
        padding:inherit;
        color:inherit;
    }
</style>