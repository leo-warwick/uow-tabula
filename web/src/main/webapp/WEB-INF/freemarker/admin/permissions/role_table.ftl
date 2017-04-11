<#compress><#escape x as x?html>
	<style type="text/css">
		.roles-table {
			width: auto !important;
		}

		.roles-table td {
			text-align: center !important;
		}

		.roles-table td:first-child {
			text-align: right !important;
			white-space: nowrap;
		}

		.roles-table abbr {
			font-size: 0.8em;
		}

		.roles-table th.rotated .rotate {
			float: left;
			position: absolute;

			-webkit-transform-origin: 0% 100%;
			-moz-transform-origin: 0% 100%;
			-ms-transform-origin: 0% 100%;
			-o-transform-origin: 0% 100%;
			transform-origin: 0% 100%;

			-webkit-transform: rotateZ(90deg);
			-moz-transform: rotateZ(90deg);
			-ms-transform: rotateZ(90deg);
			-o-transform: rotateZ(90deg);
			transform: rotateZ(90deg);

			white-space: nowrap;
		}

		.dropdown-menu > li > label {
			display: block;
			padding: 3px 20px;
			clear: both;
			font-weight: normal;
			line-height: 20px;
			color: #3a3a3c;
			white-space: nowrap;
			margin-left: 10px;
		}

		.transparent { color: transparent; }
	</style>

	<#function roleDescription definition>
		<#if definition.baseRoleDefinition??>
			<#if definition.replacesBaseDefinition>
				<#return roleDescription(definition.baseRoleDefinition) />
			<#else>
				<#local result><span title="Derived from ${definition.baseRoleDefinition.description}">${definition.name}</span></#local>
				<#return result />
			</#if>
		<#else>
			<#return definition.description />
		</#if>
	</#function>

	<#function roleId definition>
		<#if definition.id??>
			<#return definition.id />
		<#elseif definition.selector??>
			<#local result>${definition.name}(${definition.selector.id})</#local>
			<#return result />
		<#else>
			<#return definition.name />
		</#if>
	</#function>

	<div class="btn-toolbar dept-toolbar">
		<form action="" class="dropdown show-hide-form clearfix">
			<a class="btn btn-default" href="#" data-toggle="dropdown" data-target=".show-hide-form">
				Show/hide roles <b class="caret"></b>
			</a>
			<ul class="dropdown-menu">
				<#list (rolesTable?first)._2() as roles>
					<li><label class="checkbox">
						<input type="checkbox" name="${roleId(roles._1())}" value="true" checked> <#noescape>${roleDescription(roles._1())}</#noescape>
					</label></li>
				</#list>
			</ul>
		</form>
	</div>

	<#if department??>
		<#function route_function dept>
			<#local result><@routes.admin.rolesDepartment dept /></#local>
			<#return result />
		</#function>
		<@fmt.id7_deptheader "Roles and capabilities" route_function "for" />
	<#else>
		<h1 class="with-settings">Roles and capabilities</h1>
	</#if>

	<table class="table table-striped table-condensed roles-table">
		<thead>
			<th></th>
			<#list (rolesTable?first)._2() as roles>
				<th class="rotated" data-name="${roleId(roles._1())}"><div class="rotate"><#noescape>${roleDescription(roles._1())}</#noescape></div></th>
			</#list>
		</thead>
		<tbody>
			<#list rolesTable as permission>
				<tr>
					<#assign permissionName>${permission._1().name}</#assign>

					<td><abbr class="use-tooltip" title="${permission._1().description}">${permissionName}</abbr></td>
					<#list permission._2() as roles>
						<td data-name="${roleId(roles._1())}">
							<#if roles._2()?has_content>
								<#if roles._2()>
									<#assign icon="icon-ok fa fa-check attended has-role" />
									<#assign title><#noescape>${roleDescription(roles._1())}</#noescape> <strong>can</strong> ${permission._1().description?uncap_first}</#assign>
								<#else>
									<#assign icon="icon-sign-blank fa fa-square transparent" />
									<#assign title><#noescape>${roleDescription(roles._1())}</#noescape> <strong>cannot</strong> ${permission._1().description?uncap_first}</#assign>
								</#if>
							<#else>
								<#assign icon="icon-ok-circle fa fa-check-circle-o authorised has-role" />
								<#assign title><#noescape>${roleDescription(roles._1())}</#noescape> <strong>can</strong> ${permission._1().description?uncap_first} <strong>for students with the same relationship</strong></#assign>
							</#if>

							<i class="${icon} icon-fixed-width fa fa-fw use-tooltip" title="${title}" data-html="true"></i>
						</td>
					</#list>
				</tr>
			</#list>
		</tbody>
	</table>

	<script type="text/javascript">
		jQuery(function($) {
			$('.roles-table th.rotated').each(function() {
				var width = $(this).find('.rotate').width();
				$(this).css('height', width + 20).css('width', 20);
				$(this).find('.rotate').css('margin-top', -(width + 25));
			});

			$('.show-hide-form').on('submit', function(e) {
				e.preventDefault();
				e.stopPropagation();
				return false;
			});

			$('.show-hide-form input[type="checkbox"]').on('change', function() {
				console.log('change');
				var name = $(this).attr('name');
				if ($(this).is(':checked')) {
					console.log('showing ' + name);
					$('.roles-table [data-name="' + name + '"]').show();
				} else {
					console.log('hiding ' + name);
					$('.roles-table [data-name="' + name + '"]').hide();
				}

				// Hide any empty rows
				var hasData = function() { return $(this).find('.has-role:visible').length > 0; };

				$('.roles-table tbody tr').show().not(hasData).hide();
			});

			$('.show-hide-form label').on('click', function(e) {e.stopPropagation();});
		});
	</script>
</#escape></#compress>
