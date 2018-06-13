<#escape x as x?html>

	<#macro previousExtensions extensionId studentIdentifier fullName acceptedExtensions rejectedExtensions previousExtensions>

		<#if previousExtensions?has_content>
			<div id="prev-extensions-${extensionId}" class="modal fade" role="dialog">
				<div class="modal-dialog" role="document">
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal" aria-label="Close">
								<span aria-hidden="true">&times;</span>
							</button>
							<h4 class="modal-title">Previous extension requests</h4>
						</div>
						<div class="modal-body">
							<h5>${fullName} - ${studentIdentifier}</h5>
							<div><strong>Accepted requests: </strong> ${acceptedExtensions}</div>
							<div><strong>Denied requests: </strong> ${rejectedExtensions}</div>
							<table class="table table-striped">
								<thead>
								<tr>
									<th>Module</th>
									<th>Assignment</th>
									<th>Status</th>
									<th>Made</th>
								</tr>
								</thead>
								<tbody>
									<#list previousExtensions as e>
									<tr>
										<td><@fmt.module_name e.assignment.module false /></td>
										<td>${e.assignment.name}</td>
										<td>${e.state.description}</td>
										<td>
											<#if e.requestedOn?has_content>
												<@fmt.date date=e.requestedOn />
											<#else>
												<@fmt.date date=e.reviewedOn />
											</#if>
										</td>
									</tr>
									</#list>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</#if>
	</#macro>

	<#macro pagination currentPage totalPages>
		<#if totalPages gt 1>
			<ul class="pagination pagination-sm pull-right" style="cursor: pointer;">
			<#if currentPage lte 1>
				<li class="disabled"><span>&laquo;</span></li>
			<#else>
				<li><a data-page="${currentPage - 1}">&laquo;</a></li>
			</#if>
			<#list 1..totalPages as page>
				<#if page == currentPage>
					<li class="active"><span>${page}</span></li>
				<#else>
					<li><a data-page="${page}">${page}</a></li>
				</#if>
			</#list>
			<#if currentPage gte totalPages>
				<li class="disabled"><span>&raquo;</span></li>
			<#else>
				<li><a data-page="${currentPage + 1}">&raquo;</a></li>
			</#if>
			</ul>
		</#if>
	</#macro>

	<#macro previousSubmissions extensionId studentIdentifier fullName previousSubmissions>
		<#if previousSubmissions?has_content>
			<div id="prev-submissions-${extensionId}" class="modal fade" role="dialog">
				<div class="modal-dialog" role="document">
					<div class="modal-content">
						<div class="modal-header">
							<button type="button" class="close" data-dismiss="modal" aria-label="Close">
								<span aria-hidden="true">&times;</span>
							</button>
							<h4 class="modal-title">Previous submissions</h4>
						</div>
						<div class="modal-body">
							<h5>${fullName} - ${studentIdentifier}</h5>
							<table class="table table-striped">
								<thead>
								<tr>
									<th>Module</th>
									<th>Assignment</th>
									<th>Status</th>
								</tr>
								</thead>
								<tbody>
								<#list previousSubmissions as submission>
								<tr>
									<td><@fmt.module_name submission.assignment.module false /></td>
									<td>${submission.assignment.name}</td>
									<td>
									<#if submission.authorisedLate>
										Within extension
									<#elseif submission.late>
										Late
									<#else>
										On time
									</#if>
									</td>
								</tr>
								</#list>
								</tbody>
							</table>
						</div>
					</div>
				</div>
			</div>
		</#if>
	</#macro>

	<#macro moduleHeader title module preposition="for">
		<#local two_line = module?has_content />
		<div class="deptheader">
			<h1 <#if !two_line>class="with-related"</#if>>${title}</h1>
			<#if two_line>
				<h4 class="with-related">${preposition} <@fmt.module_name module /></h4>
			</#if>
		</div>
	</#macro>

	<#macro assignmentHeader title assignment preposition="for" admin=true>
		<#if can.do('Submission.Read', assignment)>
			<div class="btn-toolbar dept-toolbar">
				<div class="btn-group">
					<#if assignment.extensionsPossible>
						<#if can.do('Extension.Update', assignment)>
							<#local ext_caption="Manage assignment's extensions" />
							<#local ext_link="Manage extensions" />
						<#else>
							<#local ext_caption="View assignment's extensions" />
							<#local ext_link="View extensions" />
						</#if>
						<#local ext_url><@routes.cm2.assignmentextensions assignment /></#local>
						<@fmt.permission_button
							permission='Extension.Read'
							scope=assignment
							action_descr=ext_caption?lower_case
							href=ext_url
							classes='btn btn-default'>
								${ext_link}
						</@fmt.permission_button>
					</#if>

					<#if assignment.cm2Assignment>
						<#local edit_url><@routes.cm2.editassignmentdetails assignment /></#local>
					<#else>
						<#local edit_url><@routes.coursework.assignmentedit assignment /></#local>
					</#if>
					<@fmt.permission_button
						permission='Assignment.Update'
						scope=assignment
						action_descr='edit assignment properties'
						href=edit_url
						classes='btn btn-default'>
							Edit assignment
					</@fmt.permission_button>
				</div>
			</div>
		</#if>

		<#local two_line = assignment?has_content />
		<div class="deptheader">
			<h1 <#if !two_line>class="with-related"</#if>>${title}</h1>
			<#if two_line>
				<h4 class="with-related">${preposition} ${assignment.name} (${assignment.module.code?upper_case}, ${assignment.academicYear.toString})</h4>
			</#if>
		</div>
	</#macro>

	<#macro workflowHeader title workflow preposition="for">
		<#local two_line = workflow?has_content />
		<div class="deptheader">
			<h1 <#if !two_line>class="with-related"</#if>>${title}</h1>
			<#if two_line>
				<h4 class="with-related">${preposition} ${workflow.name}</h4>
			</#if>
		</div>
	</#macro>

	<#macro departmentHeader title department routeFunction academicYear="" preposition="for">
		<div class="btn-toolbar dept-toolbar">
			<div class="btn-group">
				<div class="btn-group">
					<a class="btn btn-default dropdown-toggle" data-toggle="dropdown">
						Assignments
						<span class="caret"></span>
					</a>
					<ul class="dropdown-menu">
						<li>
							<#assign setup_Url><@routes.cm2.create_sitsassignments department academicYear /></#assign>
							<@fmt.permission_button
								permission='Assignment.ImportFromExternalSystem'
								scope=department
								action_descr='setup assignments from SITS'
								href=setup_Url>
									Create assignments from SITS
							</@fmt.permission_button>
						</li>
						<li>
							<#assign copy_url><@routes.cm2.copy_assignments_previous department academicYear /></#assign>
							<@fmt.permission_button
								permission='Assignment.Create'
								scope=department
								action_descr='copy existing assignments'
								href=copy_url>
									Create assignments from previous
							</@fmt.permission_button>
						</li>
					</ul>
				</div>
				<#assign extensions_url><@routes.cm2.filterExtensions academicYear />?departments=${department.code}</#assign>
				<@fmt.permission_button
					permission='Extension.Read'
					scope=department
					action_descr='manage extensions'
					href=extensions_url
					classes='btn btn-default'>
						Extension requests
				</@fmt.permission_button>
				<#if features.markingWorkflows>
					<#assign markingflow_url><@routes.cm2.reusableWorkflowsHome department academicYear /></#assign>
					<@fmt.permission_button
						permission='MarkingWorkflow.Read'
						scope=department
						action_descr='manage marking workflows'
						href=markingflow_url
						classes='btn btn-default'>
							Marking workflows
					</@fmt.permission_button>
				</#if>
				<div class="btn-group">
					<a class="btn btn-default dropdown-toggle" data-toggle="dropdown">
						Feedback
						<span class="caret"></span>
					</a>
					<ul class="dropdown-menu">
						<#if features.feedbackTemplates>
							<li>
								<#assign feedback_url><@routes.cm2.feedbacktemplates department /></#assign>
								<@fmt.permission_button
									permission='FeedbackTemplate.Manage'
									scope=department
									action_descr='create feedback template'
									href=feedback_url>
										Feedback templates
								</@fmt.permission_button>
							</li>
						</#if>
						<li>
							<#assign feedbackrep_url><@routes.cm2.feedbackreport department /></#assign>
							<@fmt.permission_button
								permission='Department.DownloadFeedbackReport'
								scope=department
								action_descr='generate a feedback report'
								href=feedbackrep_url>
									Feedback reports
							</@fmt.permission_button>
						</li>
					</ul>
				</div>
				<div class="btn-group">
					<a class="btn btn-default dropdown-toggle" data-toggle="dropdown">
						Settings
						<span class="caret"></span>
					</a>
					<ul class="dropdown-menu pull-right">
						<li>
							<#assign settings_url><@routes.admin.displaysettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
							<@fmt.permission_button
								permission='Department.ManageDisplaySettings'
								scope=department
								action_descr='manage department settings'
								href=settings_url>
									Department settings
							</@fmt.permission_button>
						</li>
						<li>
							<#assign settings_url><@routes.admin.notificationsettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
							<@fmt.permission_button
								permission='Department.ManageNotificationSettings'
								scope=department
								action_descr='manage department notification settings'
								href=settings_url>
									Notification settings
							</@fmt.permission_button>
						</li>
						<li>
							<#assign extensions_url><@routes.cm2.extensionSettings department /></#assign>
							<@fmt.permission_button
								permission='Department.ManageExtensionSettings'
								scope=department
								action_descr='manage extension settings'
								href=extensions_url>
									Extension settings
							</@fmt.permission_button>
						</li>
					</ul>
				</div>
			</div>
		</div>

		<@fmt.id7_deptheader title routeFunction preposition />
	</#macro>
</#escape>