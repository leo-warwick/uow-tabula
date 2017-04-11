<#import "*/permissions_macros.ftl" as pm />
<#import "/WEB-INF/freemarker/modal_macros.ftl" as modal />

<#escape x as x?html>
	<h1>${customRoleDefinition.description}</h1>

	<div class="overrides">
		<h2>Role overrides</h2>

		<p><a class="btn btn-default" href="<@routes.admin.addcustomroleoverride customRoleDefinition />">Create</a></p>

		<#if roleInfo.overrides?size gt 0>
			<table class="table table-striped permission-list">
				<thead>
					<tr>
						<th>Permission</th>
						<th>Allowed?</th>
						<th>&nbsp;</th>
					</tr>
				</thead>
				<tbody>
					<#list roleInfo.overrides as override>
						<#assign permissionName><#compress>
							<#if override.permission.selector??>
								${override.permission.name}(${override.permission.selector.id})
							<#else>
								${override.permission.name}
							</#if>
						</#compress></#assign>
						<tr>
							<td><abbr title="${override.permission.description}" class="initialism">${permissionName}</abbr></td>
							<td>
								<#if override.overrideType>
									<i class="fa fa-check" title="Allowed"></i>
								<#else>
									<i class="fa fa-times" title="Not allowed"></i>
								</#if>
							</td>
							<td class="actions">
								<a class="btn btn-xs btn-danger" data-remote="false" href="<@routes.admin.deletecustomroleoverride override />" data-toggle="modal" data-target="#custom-roles-modal">Delete</a>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
		<#else>
			<p>There are no overrides for ${customRoleDefinition.name}.</p>
		</#if>
	</div>

	<h2>All permissions</h2>

	<@pm.debugRole role=roleInfo.role showScopes=false />

	<div id="custom-roles-modal" class="modal fade">
		<@modal.wrapper>
			<@modal.header>
				<h3 class="modal-title">Delete role override</h3>
			</@modal.header>
			<@modal.body></@modal.body>
		</@modal.wrapper>
	</div>

	<script type="text/javascript">
		jQuery(function($){
			$('.permission-list').on('click', 'a[data-toggle=modal]', function(e){
				var $this = $(this);
				var $modal = $($this.data('target'));
				var $body = $modal.find('.modal-body').empty();
				$body.load($this.attr('href'), function() {
					$body.find('.btn').each(function() {
						if ($(this).text() == 'Cancel') {
							$(this).attr('data-dismiss', 'modal');
						}
					});
				});
			});

			$("a.disabled").on('click', function(e){e.preventDefault(e); return false;})
		});
	</script>
</#escape>