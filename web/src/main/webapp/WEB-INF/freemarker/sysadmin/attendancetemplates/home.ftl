<#escape x as x?html>

<h1>Attendance monitoring templates</h1>

<#if templates?size == 0>
	<div class="striped-section">
		<div class="pull-right">
			<a href="<@url page="/sysadmin/attendancetemplates/add" />" class="btn btn-primary btn-medium pull-right">
				Add a new template
			</a>
		</div>
		<h2 class="section-title">Templates</h2>
		<div class="striped-section-contents">
			<div class="item-info">
				<em>No templates exist</em>
			</div>
		</div>
	</div>
<#else>
	<form id="reorderTemplates" action="<@url page="/sysadmin/attendancetemplates/reorder"/>" method="POST">
		<p style="color: #818285;"><em>Drag and drop to reorder templates.</em></p>
		<div class="striped-section">
			<div class="pull-right">
				<a href="<@url page="/sysadmin/attendancetemplates/add" />" class="btn btn-primary btn-medium pull-right">
					Add a new template
				</a>
			</div>
			<h2 class="section-title">Templates</h2>
			<div class="striped-section-contents">
				<#assign templateCount = 0 />
				<#list templates as template>
					<div class="item-info row point" style="cursor: move;">
						<div class="col-md-12">
							<div class="pull-right">
								<a href="<@url page="/sysadmin/attendancetemplates/${template.id}/edit" />" class="btn btn-primary btn-sm">Edit</a>
								<a href="<@url page="/sysadmin/attendancetemplates/${template.id}/delete" />" class="btn btn-danger btn-sm">Delete</a>
							</div>
							<input type="hidden" name="templates[${templateCount}]" value="${template.id}" />
							<span class="count">${templateCount + 1}</span>. ${template.templateName} (${template.points?size} points)
						</div>
					</div>
					<#assign templateCount = templateCount + 1 />
				</#list>
			</div>
		</div>
		<input type="submit" value="Reorder" class="btn btn-primary"/>
	</form>

	<script>
		jQuery(function($){
			$('.striped-section-contents').sortable({
				axis: 'y',
				cancel: 'a',
				cursor: 'move',
				items: 'div.item-info',
				update: function() {
					$('.striped-section-contents div.item-info').each(function(i){
						$(this).find('span.count').html(i + 1)
								.end().find('input').attr('name', 'templates[' + i + ']');
					});
				}
			});

		});
	</script>
</#if>

</#escape>