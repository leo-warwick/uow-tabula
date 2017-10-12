<#import "*/modal_macros.ftl" as modal />
<#import "*/cm2_macros.ftl" as cm2 />
<#escape x as x?html>
	<#function route_function dept>
		<#local result><@routes.cm2.feedbacktemplates dept /></#local>
		<#return result />
	</#function>
	<@cm2.departmentHeader "Feedback templates" department route_function />

	<#assign actionUrl><@routes.cm2.feedbacktemplates department /></#assign>
	<@f.form enctype="multipart/form-data"
			 method="post"
			 action="${actionUrl}"
			 commandName="bulkFeedbackTemplateCommand">

		<@bs3form.filewidget
			basename="file"
			labelText="Upload feedback forms"
			types=[]
			multiple=true
		/>

		<script>
			var frameLoad = function(frame){
				if(jQuery(frame).contents().find("form").length == 0){
					jQuery("#feedback-template-model").modal('hide');
					document.location.reload(true);
				}
			};

			jQuery(function($){

				// modals use ajax to retrieve their contents
				$('#feedback-template-list').on('click', 'a[data-toggle=modal]', function(e){
					$this = $(this);
					target = $this.prop('data-url');
					$("#feedback-template-model .modal-body").html('<iframe src="'+target+'" onLoad="frameLoad(this)" frameBorder="0" scrolling="no" style="width: 100%;"></iframe>')
				});

				$('#feedback-template-model').on('click', 'input[type=submit]', function(e){
					e.preventDefault();
					$('#feedback-template-model iframe').contents().find('form').submit();
				});
			});
		</script>
		<button type="submit" class="btn btn-primary">
			Upload
		</button>

		<hr />

		<div class="submit-buttons">
			<#if bulkFeedbackTemplateCommand.department.feedbackTemplates?has_content>
				<table id="feedback-template-list" class="table table-striped">
					<thead>
						<tr>
							<th>Name</th>
							<th>Description</th>
							<th>Assignments</th>
							<th><!-- Actions column--></th>
						</tr>
					</thead>
					<tbody>
					<#list bulkFeedbackTemplateCommand.department.feedbackTemplates as template>
						<tr>
							<td>${template.name}</td>
							<td>${template.description!""}</td>
							<td>
								<#if template.hasAssignments>
									${template.countLinkedAssignments}&nbsp;
									<a id="tool-tip-${template.id}" class="btn btn-xs btn-default" data-toggle="button" href="#">
										List
									</a>
									<div id="tip-content-${template.id}" class="hide">
										<ul>
											<#list template.assignments as assignment>
												<li>
													<a href="<@routes.cm2.depthome module=assignment.module academicYear=assignment.academicYear />">
													<#if assignment.name?has_content>
														${assignment.module.code} - ${assignment.name}
													<#else>
														${assignment.module.code} - N/A
													</#if>
													</a>
												</li>
											</#list>
										</ul>
									</div>
									<script type="text/javascript">
										jQuery(function($){
											var markup = $('#tip-content-${template.id}').html();
											$("#tool-tip-${template.id}").popover({
												placement: 'right',
												html: true,
												content: markup,
												title: 'Assignments linked to ${template.name}'
											});
										});
									</script>
								<#else>
									None
								</#if>
							</td>
							<td>
								<#if template.attachment??>
								<a class="btn btn-xs btn-primary" href="<@routes.cm2.feedbacktemplatedownload department=department feedbacktemplate=template />">
									 Download
								</a>
								</#if>
								<a class="btn btn-xs btn-default" href="#feedback-template-model" data-toggle="modal" data-url="<@routes.cm2.editfeedbacktemplate department=department template=template />">
									 Edit
								</a>
								<#if !template.hasAssignments>
									<a class="btn btn-xs btn-default" href="#feedback-template-model" data-toggle="modal" data-url="<@routes.cm2.deletefeedbacktemplate department=department template=template />">
									Delete
									</a>
								<#else>
									<a class="btn btn-xs btn-default disabled" href="#" title="You cannot delete a feedback template with linked assignments">
									Delete
									</a>
								</#if>
							</td>
						</tr>
					</#list>
					</tbody>
				</table>

				<div id="feedback-template-model" class="modal fade">
				<@modal.wrapper>
					<@modal.header>
						<h3 class="modal-title">Update feedback template</h3>
					</@modal.header>
					<@modal.body></@modal.body>
					<@modal.footer>
						<input type="submit" value="Confirm" class="btn btn-primary">
						<a data-dismiss="modal" class="close-model btn btn-default" href="#">Cancel</a>
					</@modal.footer>
				</@modal.wrapper>
			</div>
			</#if>
		</div>
	</@f.form>
</#escape>