<#import "../admin/assignments/submissionsandfeedback/_submission_details.ftl" as sd />

<#assign has_assignments = enrolledAssignments?has_content />
<#assign has_historical_items = historicAssignments?has_content />

<#if has_assignments || has_historical_items || user.student || user.PGR>
	<div class="header-with-tooltip" id="your-assignments">
		<#if ajax><h4><#else><h2 class="section"></#if>
		<#if isSelf>My<#else>${student.firstName}'s</#if> assignments
		<#if ajax></h4><#else></h2></#if>
		<#if isSelf>
			<span class="use-tooltip" data-toggle="tooltip" data-html="true" data-placement="bottom" data-title="Talk to your module convenor if you think an assignment is missing - maybe it isn't set up yet, or they aren't using Tabula.">Missing an assignment?</span>
		</#if>
	</div>
</#if>

<#if has_assignments>
	<div class="striped-section collapsible expanded" data-name="pending">
		<div class="clearfix">
			<h3 class="section-title">Pending</h3>
		</div>

		<div class="striped-section-contents">
			<#macro enrolled_assignment info>
				<#local assignment = info.assignment />
				<#local extension = info.extension!false />
				<#local hasExtension = info.hasExtension />
				<#local hasActiveExtension = info.hasActiveExtension />
				<#local isExtended = info.isExtended!false />
				<#local extensionRequested = info.extensionRequested!false />

				<div class="item-info clearfix<#if !info.isExtended && info.closed> late<#elseif info.isExtended!false> extended</#if>">
					<div class="row-fluid">
						<div class="span5">
							<div class="module-title"><@fmt.module_name assignment.module /></div>
							<h4 class="name">
								<#if isSelf><a href="<@url context='/coursework' page='/module/${assignment.module.code}/${assignment.id}/' />"></#if>
									<span class="ass-name">${assignment.name}</span>
								<#if isSelf></a></#if>
							</h4>
						</div>
						<div class="span4">
							<#if !assignment.opened>
								<div class="not-open deadline">
									Opens <@fmt.date date=assignment.openDate /><br>
									<#if assignment.openEnded>
										No close date
									<#else>
										Deadline <@fmt.date date=assignment.closeDate />
									</#if>
								</div>
							<#else>
								<#if assignment.openEnded>
									<#-- Open ended assignment. Can't display a deadline, but display whether it's open yet -->
									<div class="deadline open-ended">
										<#if info.submittable>
											<div class="deadline">No close date</div>
										</#if>
									</div>
								<#elseif info.submittable>
									<#local textOnly = true />
									<#include "/WEB-INF/freemarker/coursework/submit/_assignment_deadline.ftl" />
								</#if>
							</#if>
						</div>
						<div class="span3 button-list">
							<#if isSelf>
								<#if info.submittable>
									<a class="btn btn-block btn-primary" href="<@url context='/coursework' page='/module/${assignment.module.code}/${assignment.id}/' />">
										<i class="icon-folder-close icon-white"></i> Submit
									</a>

									<#if assignment.extensionsPossible>
										<#if extensionRequested>
											<a href="<@routes.coursework.extensionRequest assignment=assignment />?returnTo=/coursework" class="btn btn-block">
												<i class="icon-calendar"></i> Review extension request
											</a>
										<#elseif !isExtended && assignment.newExtensionsCanBeRequested>
											<a href="<@routes.coursework.extensionRequest assignment=assignment />?returnTo=/coursework" class="btn btn-block">
												<i class="icon-calendar"></i> Request extension
											</a>
										</#if>
									</#if>
								</#if>
							</#if>
						</div>
					</div>
				</div>
			</#macro>

			<#list enrolledAssignments as info>
				<@enrolled_assignment info />
			</#list>
		</div>
	</div>
<#elseif user.student || user.PGR>
	<div class="alert alert-block alert-info">
		<#if !ajax><h3>Pending</h3></#if>
		There are no pending assignments to show <#if isSelf>you </#if>in Tabula right now
	</div>
</#if>

<#if has_historical_items>
	<div class="striped-section collapsible" data-name="past">
		<div class="clearfix">
			<h3 class="section-title">Past</h3>
		</div>

		<div class="striped-section-contents">
			<#macro marked_assignment info>
				<#local assignment = info.assignment />
				<#local submission = info.submission!false />
				<#local hasSubmission = info.hasSubmission!false />
				<#local feedback = info.feedback!false />
				<#local hasFeedback = info.hasFeedback!false />
				<#local extension = info.extension!false />
				<#local isExtended = info.isExtended!false />
				<#local extensionRequested = info.extensionRequested!false />
				<#local isFormative = !info.summative!false />
				<#local assignmentLink><#compress>
					<#if isSelf>
						<@routes.coursework.assignment assignment />
					<#else>
						<@routes.coursework.assignment_in_profile assignment student/>
					</#if>
				</#compress></#local>

				<div class="item-info clearfix marked">
					<div class="row-fluid">
						<div class="span4">
							<div class="module-title"><@fmt.module_name assignment.module /></div>
							<h4 class="name">
								<a href="${assignmentLink}">
									<span class="ass-name">${assignment.name}</span>
								</a>
							</h4>
						</div>
						<div class="span5">
							<#if hasSubmission>
								Submitted <@fmt.date date=submission.submittedDate />
								<#if submission.late>
									<span class="label label-important use-tooltip" title="<@sd.lateness submission />" data-container="body">Late</span>
								<#elseif submission.authorisedLate>
									<span class="label label-info use-tooltip" title="<@sd.lateness submission />" data-container="body">Within Extension</span>
								</#if>
							<#elseif isFormative>
								<span class="label use-tooltip" title="Formative assignments do not contribute to <#if isSelf>your<#else>a student's</#if> module grade or mark. They provide an opportunity to feedback and/or evaluate <#if isSelf>your<#else>a student's</#if> learning.">Formative, no submission</span>
							</#if>
						</div>
						<div class="span3 button-list">
							<#if hasFeedback>
								<#-- View feedback -->
								<a class="btn btn-block btn-success" href="${assignmentLink}">
									<i class="icon-check icon-white"></i> View feedback
								</a>
							<#elseif info.resubmittable && isSelf>
								<#-- Resubmission allowed -->
								<a class="btn btn-block btn-primary" href="${assignmentLink}">
									<i class="icon-folder-close icon-white"></i> Resubmit
								</a>
							<#elseif hasSubmission>
								<#-- View receipt -->
								<a class="btn btn-block" href="${assignmentLink}">
									<i class="icon-list-alt"></i> View <#if isSelf>receipt<#else>submission</#if>
								</a>
							<#elseif info.submittable>
								<#if isSelf>
									<#-- First submission still allowed -->
									<a class="btn btn-block btn-primary" href="${assignmentLink}">
										<i class="icon-folder-close icon-white"></i> Submit
									</a>
								</#if>
							<#else>
								<#-- Assume formative, so just show info -->
								<a class="btn btn-block" href="${assignmentLink}">
									<i class="icon-list-alt"></i> View details
								</a>
							</#if>
						</div>
					</div>
				</div>
			</#macro>

			<#list historicAssignments as info>
				<@marked_assignment info />
			</#list>
		</div>
	</div>
<#elseif user.student || user.PGR>
	<div class="alert alert-block alert-info">
		<#if !ajax><h3>Past</h3></#if>
		There are no past assignments to show <#if isSelf>you </#if>in Tabula right now
	</div>
</#if>