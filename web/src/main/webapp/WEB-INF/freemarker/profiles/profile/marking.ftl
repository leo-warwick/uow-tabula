<#escape x as x?html>

<h1>Marking</h1>

<#if hasPermission>

	<div class="striped-section collapsible expanded">
		<h3 class="section-title">To do</h3>
		<div class="striped-section-contents">
			<#if result.todo?has_content>

				<#list result.todo as assignmentInfo>
					<div class="row item-info">
						<#assign assignmentDeadlineExists = assignmentInfo.assignment.feedbackDeadline?has_content>
						<#if assignmentDeadlineExists>
							<#assign assignmentDeadline = assignmentInfo.assignment.feedbackDeadline />
						</#if>
						<div class="col-md-<#if assignmentDeadlineExists>5<#else>10</#if>">
							<h4><@fmt.module_name assignmentInfo.assignment.module /></h4>
							<h4>${assignmentInfo.assignment.name!}</h4>
						</div>
						<#if assignmentDeadlineExists>
							<div class="col-md-5">
								Deadline <@fmt.date date=assignmentDeadline relative=false includeTime=false />
							</div>
						</#if>
						<div class="col-md-2">
							<#if isSelf>
								<a href="<@routes.coursework.listmarkersubmissions assignmentInfo.assignment user.apparentUser />?returnTo=${info.requestedUri}" class="btn btn-primary btn-block">Mark</a>
							</#if>
						</div>
					</div>
				</#list>

			<#else>

				<div class="row item-info">
					<div class="col-md-12">
						There are no assignments in Tabula that need marking at this time.
					</div>
				</div>

			</#if>
		</div>
	</div>

	<#if result.doing?has_content>

		<div class="striped-section collapsible expanded">
			<h3 class="section-title">Doing</h3>
			<div class="striped-section-contents">
				<#list result.doing as assignmentInfo>
					<div class="row item-info">
						<#assign assignmentDeadlineExists = assignmentInfo.assignment.feedbackDeadline?has_content>
						<#if assignmentDeadlineExists>
							<#assign assignmentDeadline = assignmentInfo.assignment.feedbackDeadline />
						</#if>
						<div class="col-md-<#if assignmentDeadlineExists>5<#else>10</#if>">
							<h4><@fmt.module_name assignmentInfo.assignment.module /></h4>
							<h4>${assignmentInfo.assignment.name!}</h4>
						</div>
						<#if assignmentDeadlineExists>
							<div class="col-md-5">
								Deadline <@fmt.date date=assignmentDeadline relative=false includeTime=false />
							</div>
						</#if>
						<div class="col-md-2">
							${assignmentInfo.status}
						</div>
					</div>
				</#list>
			</div>
		</div>

	</#if>

	<#if result.done?has_content>

		<div class="striped-section collapsible">
			<h3 class="section-title">Done</h3>
			<div class="striped-section-contents">
				<#list result.done as assignmentInfo>
					<div class="row item-info">
						<#assign assignmentDeadlineExists = assignmentInfo.assignment.feedbackDeadline?has_content>
						<#if assignmentDeadlineExists>
							<#assign assignmentDeadline = assignmentInfo.assignment.feedbackDeadline />
						</#if>
						<div class="col-md-<#if assignmentDeadlineExists>5<#else>10</#if>">
							<h4><@fmt.module_name assignmentInfo.assignment.module /></h4>
							<h4>${assignmentInfo.assignment.name!}</h4>
						</div>
						<#if assignmentDeadlineExists>
							<div class="col-md-5">
								Closed <@fmt.date date=assignmentDeadline relative=false includeTime=false />
							</div>
						</#if>
					</div>
				</#list>
			</div>
		</div>

	</#if>

<#else>

	<div class="alert alert-info">
		You do not have permission to see the marking for this person.
	</div>

</#if>

</#escape>