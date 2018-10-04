<#escape x as x?html>
<h1>Mapped Syllabus+ rooms</h1>
<p>This is a list of all Syllabus+ locations in Tabula.</p>

<p>
	<#if canManage>
		<a href="<@routes.admin.addLocation />" class="btn btn-primary">Add location</a>
	<#else>
		<span class="btn btn-primary disabled use-tooltip" data-title="You do not have permission to manage Syllabus+ locations">Add location</span>
	</#if>
</p>

<#if locations?size == 0>
	<form action="/admin/scientia-rooms/populate" method="post">
		<p>
			<button class="btn btn-primary">Populate from hard-coded data</button>
		</p>
	</form>
</#if>

<table class="table table-bordered table-striped table-condensed sortable">
	<thead>
		<tr>
			<th class="sortable">Syllabus+ name</th>
			<th class="sortable">Tabula name</th>
			<th class="sortable">Location ID</th>
			<th></th>
		</tr>
	</thead>
	<tbody>
		<#list locations as location>
			<tr>
				<td>${location.upstreamName!""}</td>
				<td>${location.name!""}</td>
				<td>
					<#if location.mapLocationId??>
						<span class="map" data-lid="${location.mapLocationId}">${location.mapLocationId}</span>
					</#if>
				</td>
				<td class="text-right">
					<#if canManage>
						<a href="<@routes.admin.editLocation location />" class="btn btn-default btn-xs">Edit</a>
						<a href="<@routes.admin.deleteLocation location />" class="btn btn-danger btn-xs">Delete</a>
					<#else>
						<span class="btn btn-default btn-xs disabled use-tooltip" data-title="You do not have permission to manage Syllabus+ locations">Edit</span>
						<span class="btn btn-danger btn-xs disabled use-tooltip" data-title="You do not have permission to manage Syllabus+ locations">Delete</span>
					</#if>
				</td>
			</tr>
		</#list>
	</tbody>
</table>
<script type="text/javascript">
	jQuery(function($) {
		$('.map[data-lid]').mapPopups({
			placement: 'left',
			expandClickTarget: true,
			template: '<div class="popover extra-wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>',
			content: _.template(['<iframe width="800" height="600" frameborder="0" src="<%- mapUrl %>"></iframe>'])
		});

		$('table.sortable').sortableTable();
	});
</script>
</#escape>