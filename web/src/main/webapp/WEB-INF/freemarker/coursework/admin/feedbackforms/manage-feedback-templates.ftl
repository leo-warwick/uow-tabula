<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#escape x as x?html>
<h1>Feedback forms for ${department.name}</h1>

<@f.form enctype="multipart/form-data"
		 method="post"
		 class="form-horizontal"
		 action="${url('/coursework/admin/department/${department.code}/settings/feedback-templates')}"
		 commandName="bulkFeedbackTemplateCommand">
	<@form.labelled_row "file.upload" "Upload feedback forms">
		<input type="file" id="file.upload" name="file.upload" multiple />
		<div id="multifile-column-description" class="help-block">
			<#include "/WEB-INF/freemarker/multiple_upload_help.ftl" />
		</div>
	</@form.labelled_row>
	<script>
		var frameLoad = function(frame){
			if(jQuery(frame).contents().find("form").length == 0){
				jQuery("#feedback-template-model").modal('hide');
				document.location.reload(true);
			}
		}

		jQuery(function($){

			// modals use ajax to retrieve their contents
			$('#feedback-template-list').on('click', 'a[data-toggle=modal]', function(e){
				$this = $(this);
				target = $this.attr('data-url');
				$("#feedback-template-model .modal-body").html('<iframe src="'+target+'" onLoad="frameLoad(this)" frameBorder="0" scrolling="no"></iframe>')
			});

			$('#feedback-template-model').on('click', 'input[type=submit]', function(e){
				e.preventDefault();
				$('#feedback-template-model iframe').contents().find('form').submit();
			});
		});
	</script>
	<button type="submit" class="btn btn-primary">
		<i class="icon-upload icon-white"></i> Upload
	</button>
	<div class="submit-buttons">
		<#if bulkFeedbackTemplateCommand.department.feedbackTemplates?has_content>
			<table id="feedback-template-list" class="table table-striped table-bordered">
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
								<span class="label label-info">${template.countLinkedAssignments}</span>&nbsp;
								<a id="tool-tip-${template.id}" class="btn btn-mini" data-toggle="button" href="#">
									<i class="icon-list"></i>
									List
								</a>
								<div id="tip-content-${template.id}" class="hide">
									<ul><#list template.assignments as assignment>
										<li>
											<a href="<@routes.coursework.depthome module=assignment.module />">
												${assignment.module.code} - ${assignment.name}
											</a>
										</li>
									</#list></ul>
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
								<span class="label">None</span>
							</#if>
						</td>
						<td>
							<#if template.attachment??>
							<a class="btn btn-mini" href="<@routes.coursework.feedbacktemplatedownload department=department feedbacktemplate=template />">
								<i class="icon-download"></i> Download
							</a>
							</#if>
							<a class="btn btn-mini" href="#feedback-template-model" data-toggle="modal" data-url="<@routes.coursework.feedbacktemplateedit department=department feedbacktemplate=template />">
								<i class="icon-pencil"></i> Edit
							</a>
							<#if !template.hasAssignments>
								<a class="btn btn-mini btn-danger" href="#feedback-template-model" data-toggle="modal" data-url="<@routes.coursework.feedbacktemplatedelete department=department feedbacktemplate=template />">
									<i class="icon-white icon-trash"></i> Delete
								</a>
							<#else>
								<a class="btn btn-mini btn-danger disabled" href="#" title="You cannot delete a feedback template with linked assignments">
									<i class="icon-white icon-trash"></i> Delete
								</a>
							</#if>
						</td>
					</tr>
				</#list>
				</tbody>
			</table>
			<div id="feedback-template-model" class="modal fade">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
					<h3>Update feedback template</h3>
				</div>
				<div class="modal-body"></div>
				<div class="modal-footer">
					<input type="submit" value="Confirm" class="btn btn-primary">
					<a data-dismiss="modal" class="close-model btn" href="#">Cancel</a>
				</div>
			</div>
		</#if>
	</div>
</@f.form>
</#escape>