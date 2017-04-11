<#escape x as x?html>

	<#macro previousExtensions extensionId warwickId fullName acceptedExtensions rejectedExtensions previousExtensions>

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
							<h5>${fullName} - ${warwickId}</h5>
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
										<td>${e.assignment.module.code}</td>
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

	<#macro previousSubmissions extensionId warwickId fullName previousSubmissions>
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
							<h5>${fullName} - ${warwickId}</h5>
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
									<td>${submission.assignment.module.code}</td>
									<td>${submission.assignment.name}</td>
									<td>
									<#if submission.isAuthorisedLate()>
										Within extension
									<#elseif submission.isLate()>
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
</#escape>