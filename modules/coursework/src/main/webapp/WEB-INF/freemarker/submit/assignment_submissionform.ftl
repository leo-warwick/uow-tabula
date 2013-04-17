<#if !submission?? && assignment.collectSubmissions>
	<#include "_assignment_deadline.ftl" />
</#if>

<#if (canSubmit && !submission??) || canReSubmit>

	<#if submission??>
	<hr>
	<h2>Re-submit</h2>
	<#if assignment.openEnded>
		<p>You can still re-submit your work in case you've made a mistake.</p>
	<#else>
		<p>You can re-submit your work in case you've made a mistake,
			<#if isExtended>
				up until the end of your extension, <@fmt.date date=extension.expiryDate /> (in ${durationFormatter(extension.expiryDate)}).
			<#else>
				up until the deadline, <@fmt.date date=assignment.closeDate /> (in ${durationFormatter(assignment.closeDate)}).
		    </#if>
		</p>
		</#if>
	</#if>

	<#if assignment.closed && !isExtended>
		<div class="alert alert-error">
			<h3>Submission date has passed</h3>
			<p>
				You can still submit to this assignment but your mark may be affected. 
			</p>
		</div>
	</#if>

	<@f.form cssClass="submission-form double-submit-protection form-horizontal" enctype="multipart/form-data" method="post" action="${url('/module/${module.code}/${assignment.id}#submittop')}" modelAttribute="submitAssignmentCommand">
	<@f.errors cssClass="error form-errors">
	</@f.errors>
	
	<@form.row>
	 <label class="control-label">Your University ID</label>
	 <@form.field>
	   <div class="uneditable-input">${user.apparentUser.warwickId}</div>
	 </@form.field>
    </@form.row>
	
	<div class="submission-fields">
	
	<#list assignment.fields as field>
	<div class="submission-field">
	<#include "/WEB-INF/freemarker/submit/formfields/${field.template}.ftl" >
	</div>
	</#list>
	
	<#if features.privacyStatement>
	<@form.row>
	<label class="control-label">Privacy statement</label>
	<@form.field>
		<p class="privacy-field">
			The data on this form relates to your submission of 
			coursework. The date and time of your submission, your 
			identity and the work you have submitted will all be 
			stored, but will not be used for any purpose other than 
			administering and recording your coursework submission.
		</p>
	</@form.field>
	</@form.row>
	</#if>
	
	<#if assignment.displayPlagiarismNotice>
	<@form.row>
	<label class="control-label">Plagiarism declaration</label>
	<@form.field>
		<p class="plagiarism-field">
			Work submitted to the University of Warwick for official
			assessment must be all your own work and any parts that
			are copied or used from other people must be appropriately
			acknowledged. Failure to properly acknowledge any copied 
			work is plagiarism and may result in a mark of zero. 
		</p>
		<p>
			<@f.errors path="plagiarismDeclaration" cssClass="error" />
			<label><@f.checkbox path="plagiarismDeclaration" /> I confirm that this assignment is all my own work</label>
		</p>
	</@form.field>
	</@form.row>
	</#if>
	
	<@form.row>
	<label class="control-label">Assignment information</label>
	<@form.field>
		<div class="assignment-info-field">
			<#if !assignment.openEnded>
				<#assign time_remaining = durationFormatter(assignment.closeDate) />
				<p>
					The deadline for this assignment is <@fmt.date date=assignment.closeDate />, 
					<span class="time-remaining">${time_remaining}</span>.
					
					<#if isExtended>
						<#assign extension_time_remaining = durationFormatter(extension.expiryDate) />
						
						You have an extension until <@fmt.date date=extension.expiryDate />,
						<span class="time-remaining">${extension_time_remaining}</span>.
					</#if>
				</p>
				
				<#if assignment.allowResubmission && (!assignment.closed || isExtended)>
					<p>
						You can submit to this assignment multiple times up to the deadline. Only
						the latest submission of your work will be accepted, and you will not be able
						to change this once the deadline has passed.
					</p> 
				</#if>
				
				<#if assignment.allowLateSubmissions>
					<p>
					  You can submit<#if assignment.allowResubmission> once only</#if> to this assignment after the deadline, but your mark 
					  may be affected.
					</p>
				</#if>
			<#else>
				<p>
					This assignment does not have a deadline.
				
					<#if assignment.allowResubmission>
						You can submit to this assignment multiple times, but only
						the latest submission of your work will be kept. 
					</#if>
				</p>
			</#if>
		</div>
	</@form.field>
	</@form.row>
	
	</div>
	
	<div class="submit-buttons">
	<input class="btn btn-large btn-primary" type="submit" value="Submit">
	</div>
	</@f.form>
	
<#elseif !submission??>

	<#if !assignment.collectSubmissions>
		<p>
			This assignment isn't collecting submissions through this system, but you may get
			an email to retrieve your feedback from here.
		</p>
		
		<h3>Expecting your feedback?</h3>
		
		<p>
			Sorry, but there doesn't seem to be anything here for you. 
			If you've been told to come here to retrieve your feedback 
			then you'll need to get in touch directly with your 
			course/module convenor to see why it hasn't been published yet. 
			When it's published you'll receive an automated email.
		</p>
		
	<#elseif assignment.closed>
		<div class="alert alert-error">
			<h3>Submission date has passed</h3>
			
			This assignment doesn't allow late submissions.
		</div>
	<#elseif !assignment.opened>
		<p>This assignment isn't open yet - it will open <@fmt.date date=assignment.openDate at=true />.</p>
	<#else>
		<p>
			
		</p>
	</#if>
	
</#if>
