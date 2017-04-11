<#escape x as x?html>

<#if !isSelf>
	<details class="indent">
		<summary>${member.officialName}</summary>
		<#if member.userId??>
			${member.userId}<br/>
		</#if>
		<#if member.email??>
			<a href="mailto:${member.email}">${member.email}</a><br/>
		</#if>
		<#if member.phoneNumber??>
			${phoneNumberFormatter(member.phoneNumber)}<br/>
		</#if>
		<#if member.mobileNumber??>
			${phoneNumberFormatter(member.mobileNumber)}<br/>
		</#if>
	</details>
</#if>

<h1>Assignments</h1>

<#if hasPermission>

	<div class="striped-section collapsible upcoming">
		<h3 class="section-title">Upcoming assignments</h3>
		<div class="striped-section-contents">
			<#if result.upcoming?has_content>
				<#list result.upcoming as enhancedAssignment>
					<div class="row item-info">
						<div class="col-md-<#if enhancedAssignment.submissionDeadline?has_content>5<#else>10</#if>">
							<h4><@fmt.module_name enhancedAssignment.assignment.module /></h4>
							<h4>${enhancedAssignment.assignment.name!}</h4>
						</div>
						<div class="col-md-5">
							<#if enhancedAssignment.assignment.openDate?has_content>
								Opens <@fmt.date date=enhancedAssignment.assignment.openDate relative=false />
							</#if>
							<#if enhancedAssignment.submissionDeadline?has_content>
								<br />
								Deadline <@fmt.date date=enhancedAssignment.submissionDeadline relative=false />
							</#if>
						</div>
					</div>
				</#list>
			<#else>
				<div class="row item-info">
					<div class="col-md-12">
						There are no upcoming assignments in Tabula.
					</div>
				</div>
			</#if>
		</div>
	</div>

	<div class="striped-section collapsible expanded todo">
		<h3 class="section-title">To do</h3>
		<div class="striped-section-contents">
			<#if result.todo?has_content>

				<#list result.todo as enhancedAssignment>
					<div class="row item-info">
						<div class="col-md-<#if enhancedAssignment.submissionDeadline?has_content>5<#else>10</#if>">
							<h4><@fmt.module_name enhancedAssignment.assignment.module /></h4>
							<h4>${enhancedAssignment.assignment.name!}</h4>
						</div>
						<#if enhancedAssignment.submissionDeadline?has_content>
							<div class="col-md-5">
								<#if enhancedAssignment.submissionDeadline.beforeNow>
									<#assign context>
										<#if enhancedAssignment.extension?? && enhancedAssignment.extension.approved>
											extended deadline
										<#else>
											deadline
										</#if>
									</#assign>
									<#assign lateness>
										<@fmt.p enhancedAssignment.assignment.workingDaysLateIfSubmittedNow(member.userId) "working day" /> overdue,
										${durationFormatter(enhancedAssignment.submissionDeadline)} after ${context}
										(<@fmt.date date=enhancedAssignment.submissionDeadline capitalise=false shortMonth=true stripHtml=true />)
									</#assign>

									<span class="label label-danger use-tooltip" title="${lateness}" data-html="true">Late</span>
								<#else>
									Due in <strong>${durationFormatter(enhancedAssignment.submissionDeadline)}</strong>
								</#if>
								<br />
								Deadline <@fmt.date date=enhancedAssignment.submissionDeadline relative=false />
							</div>
						</#if>
						<div class="col-md-2">
							<#if isSelf>
							<a href="<@routes.coursework.assignment enhancedAssignment.assignment />?returnTo=${info.requestedUri}" class="btn btn-primary btn-block">Submit</a>
								<#if enhancedAssignment.assignment.extensionsPossible>
									<#if enhancedAssignment.extensionRequested>
										<a href="<@routes.coursework.extensionRequest assignment=enhancedAssignment.assignment />?returnTo=${info.requestedUri}" class="btn btn-default btn-block">
											Review extension request
										</a>
									<#elseif !enhancedAssignment.withinExtension && enhancedAssignment.assignment.newExtensionsCanBeRequested>
										<a href="<@routes.coursework.extensionRequest assignment=enhancedAssignment.assignment />?returnTo=${info.requestedUri}" class="btn btn-default btn-block">
											Request an extension
										</a>
									</#if>
								</#if>
							</#if>
						</div>
					</div>
				</#list>

			<#else>

				<div class="row item-info">
					<div class="col-md-12">
						There are no assignments in Tabula that need submissions at this time.
					</div>
				</div>

			</#if>
		</div>
	</div>

	<#if result.doing?has_content>

		<div class="striped-section collapsible expanded doing">
			<h3 class="section-title">Doing</h3>
			<div class="striped-section-contents">
				<#list result.doing as enhancedAssignment>
					<div class="row item-info">
						<div class="col-md-5">
							<h4><@fmt.module_name enhancedAssignment.assignment.module /></h4>
							<h4>${enhancedAssignment.assignment.name!}</h4>
						</div>
						<div class="col-md-5">
							<#if enhancedAssignment.feedbackDeadlineWorkingDaysAway?has_content>
								<#if (enhancedAssignment.feedbackDeadlineWorkingDaysAway > 0)>
									Feedback due in <strong><@fmt.p enhancedAssignment.feedbackDeadlineWorkingDaysAway "working day" /></strong><br />
								<#elseif enhancedAssignment.feedbackDeadlineWorkingDaysAway == 0>
									Feedback due <strong>today</strong><br />
								<#else>
									Feedback <strong>overdue</strong><br />
								</#if>
							</#if>
							Submitted <@fmt.date date=enhancedAssignment.submission.submittedDate relative=false />
							<#if enhancedAssignment.submission.late>
								<#assign context>
									<#if enhancedAssignment.extension?? && enhancedAssignment.extension.approved>
										extended deadline
									<#else>
										deadline
									</#if>
								</#assign>
								<#assign lateness>
									<@fmt.p enhancedAssignment.submission.workingDaysLate "working day" /> overdue,
									${durationFormatter(enhancedAssignment.submissionDeadline)} after ${context}
									(<@fmt.date date=enhancedAssignment.submissionDeadline capitalise=false shortMonth=true stripHtml=true />)
								</#assign>

								<span class="label label-danger use-tooltip" title="${lateness}" data-html="true">Late</span>
							</#if>
						</div>
						<div class="col-md-2">
							<#if isSelf>
								<a href="<@routes.coursework.assignment enhancedAssignment.assignment />?returnTo=${info.requestedUri}" class="btn btn-primary btn-block">View receipt</a>
								<#if enhancedAssignment.assignment.extensionsPossible>
									<#if enhancedAssignment.extensionRequested>
										<a href="<@routes.coursework.extensionRequest assignment=enhancedAssignment.assignment />?returnTo=${info.requestedUri}" class="btn btn-default btn-block">
											Review extension request
										</a>
									<#elseif !enhancedAssignment.withinExtension && enhancedAssignment.assignment.newExtensionsCanBeRequested>
										<a href="<@routes.coursework.extensionRequest assignment=enhancedAssignment.assignment />?returnTo=${info.requestedUri}" class="btn btn-default btn-block">
											Request an extension
										</a>
									</#if>
								</#if>
							</#if>
						</div>
					</div>
				</#list>
			</div>
		</div>

	</#if>

	<#if result.done?has_content>

		<div class="striped-section collapsible done">
			<h3 class="section-title">Done</h3>
			<div class="striped-section-contents">
				<#list result.done as enhancedAssignment>
					<div class="row item-info">
						<div class="col-md-5">
							<h4><@fmt.module_name enhancedAssignment.assignment.module /></h4>
							<h4>${enhancedAssignment.assignment.name!}</h4>
						</div>
						<div class="col-md-5">
							<#if enhancedAssignment.submissionDeadline?has_content>
								Closed <@fmt.date date=enhancedAssignment.submissionDeadline relative=false />
							</#if>
							<#if enhancedAssignment.submission?? && enhancedAssignment.submission.late>
								<#assign context>
									<#if enhancedAssignment.extension?? && enhancedAssignment.extension.approved>
										extended deadline
									<#else>
										deadline
									</#if>
								</#assign>
								<#assign lateness>
									<@fmt.p enhancedAssignment.submission.workingDaysLate "working day" /> overdue,
									${durationFormatter(enhancedAssignment.submissionDeadline)} after ${context}
									(<@fmt.date date=enhancedAssignment.submissionDeadline capitalise=false shortMonth=true stripHtml=true />)
								</#assign>

								<span class="label label-danger use-tooltip" title="${lateness}" data-html="true">Late</span>
							</#if>
						</div>
						<div class="col-md-2">
							<#assign assignmentLink><#compress>
								<#if isSelf>
									<@routes.coursework.assignment enhancedAssignment.assignment />
								<#else>
									<@routes.coursework.assignment_in_profile enhancedAssignment.assignment member />
								</#if>
							</#compress></#assign>
							<a href="${assignmentLink}?returnTo=${info.requestedUri}" class="btn btn-primary btn-block">View feedback</a>
						</div>
					</div>
				</#list>
			</div>
		</div>

	</#if>

<#else>

	<div class="alert alert-info">
		You do not have permission to see the assignments for this course.
	</div>

</#if>



</#escape>