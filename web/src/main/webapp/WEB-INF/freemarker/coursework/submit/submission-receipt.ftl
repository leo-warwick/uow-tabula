<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<#import "../admin/assignments/submissionsandfeedback/_submission_details.ftl" as sd />
<#assign assignment=submission.assignment />
<#assign module=assignment.module />
<#if assignment.findExtension(submission.usercode)??>
	<#assign extension=assignment.findExtension(submission.usercode) />
</#if>

<head>
	<style type="text/css">
		body {
			font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
		}

		h1 {
			font-size: 20px;
			margin: 0 0 10px 0;
		}

		h2 {
			font-size: 18px;
			margin: 0 0 8px 0;
		}

		#header {
			border-bottom: 1px solid #888;
		}

		.mod-code {
			letter-spacing: 1px;
			font-weight: bold;
			font-size: 130%;
		}

		.mod-name {
			font-size: 85%;
		}

		.mod-name:before {
			content: '// ';
			position: relative;
			bottom: 1px;
		}

		.ass-name {
			display: block;
			margin-top: -3px;
		}
	</style>
	<title>${module.name} (${module.code?upper_case} ${module.name}) - ${assignment.name}</title>
</head>
<body>
	<div id="header">
		<img src="/static/images/logo-full-black.png" style="width: 30%; float: right;" />

		<h1><@fmt.module_name module /></h1>
		<h2 class="ass-name">${assignment.name}</h2>

		<br style="clear: both;" />
	</div>

	<div class="assignment-details">
		<#if !assignment.openEnded>
			<p>Assignment deadline <@fmt.date date=assignment.closeDate at=true seconds=false relative=false /></p>
		</#if>
	</div>

	<h2>Submission receipt for ${submission.studentIdentifier}</h2>

	<div class="submission-receipt">
		<p>Submission received <@fmt.date date=submission.submittedDate at=true seconds=true relative=false /> (<#compress>
			<#if submission.late>
				<#if (extension.approved)!false>
					<@sd.extensionLateness extension submission />
				<#else>
					<@sd.lateness submission />
				</#if>
			<#elseif submission.authorisedLate>
				within extension until <@fmt.date date=extension.expiryDate capitalise=false shortMonth=true relative=false />
			<#else>
				on time
			</#if>
		</#compress>).</p>
		<p>Submission ID: ${submission.id}</p>

		<#if submission.allAttachments??>
			<p>
				Uploaded files:
				<ul>
					<#list submission.allAttachments as attachment>
						<li>${attachment.name}</li>
					</#list>
				</ul>
			</p>
		</#if>

		<#if assignment.wordCountField?? && submission.valuesByFieldName[assignment.defaultWordCountName]??>
			<p>Declared word count: ${submission.valuesByFieldName[assignment.defaultWordCountName]?number} words</p>
		</#if>

		<#if assignment.displayPlagiarismNotice>
			<p class="plagiarism-field">
				Work submitted to the University of Warwick for official
				assessment must be all your own work and any parts that
				are copied or used from other people must be appropriately
				acknowledged. Failure to properly acknowledge any copied
				work is plagiarism and may result in a mark of zero.
			</p>
			<p>
				<input type="checkbox" checked="checked" disabled="disabled" /> I confirm that this assignment is all my own work
			</p>
		</#if>
	</div>
</body>
</html>
</#escape>