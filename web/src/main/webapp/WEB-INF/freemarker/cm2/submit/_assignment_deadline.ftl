<#--
	Used in /WEB-INF/freemarker/home/_student.ftl and assignment_submissionform.ftl
-->
<#import "../admin/assignments/submissionsandfeedback/_submission_details.ftl" as sd />

<#if !assignment.openEnded>
	<#macro extensionButtonContents label assignment>
		<a href="<@routes.cm2.extensionRequest assignment=assignment />?returnTo=<@routes.cm2.assignment assignment=assignment />" class="btn btn-default">
			${label}
		</a>
	</#macro>

	<#macro extensionButton extensionRequested isExtended>
		<p>
			<#if extensionRequested>
				<@extensionButtonContents "Review extension request" assignment />
			<#elseif !isExtended && assignment.newExtensionsCanBeRequested>
				<@extensionButtonContents "Request an extension" assignment />
			</#if>
		</p>
	</#macro>

	<#assign time_remaining = durationFormatter(assignment.closeDate) />
	<#assign showIconsAndButtons = (!textOnly)!true />
	<#if hasActiveExtension>
		<#assign extension_time_remaining = durationFormatter(extension.expiryDate) />
	</#if>

	<#if isExtended>
		<div>${extension_time_remaining} <span class="label label-info">Extended</span></div>
		Extension granted until <@fmt.date date=extension.expiryDate />
		<#if showIconsAndButtons><@extensionButton extensionRequested isExtended /></#if>
	<#elseif assignment.closed>
		<div class="alert alert-info">
			<#if hasActiveExtension>
				<#assign latenesstooltip><#if isSelf>"<@sd.lateness submission assignment user />"<#else>"<@sd.lateness submission assignment student />"</#if></#assign>
				<div>${extension_time_remaining} <span class="label label-warning use-tooltip" title=${latenesstooltip} data-container="body">Late</span></div>
				Extension deadline was <@fmt.date date=extension.expiryDate />
				</div>
			<#else>
				<#assign latenesstooltip><#if isSelf>"<@sd.lateness submission assignment user />"<#else>"<@sd.lateness submission assignment student />"</#if></#assign>
				<div>${time_remaining} <span class="label label-warning use-tooltip" title=${latenesstooltip} data-container="body">Late</span></div>
				Deadline was <@fmt.date date=assignment.closeDate />
			</#if>
		</div>
		<#if showIconsAndButtons><@extensionButton extensionRequested isExtended /></#if>
	<#else>
		<div class="alert alert-info">
			<div>${time_remaining}</div>
			Deadline <@fmt.date date=assignment.closeDate />
			<#if showIconsAndButtons><@extensionButton extensionRequested isExtended /></#if>
		</div>
	</#if>
</#if>