<#escape x as x?html>
	<h1>Delete location</h1>

	<form method="post">
		<p>
			The location <strong>${location.upstreamName}</strong> will be deleted.
		</p>

		<p>
			<button class="btn btn-danger">Delete</button>
			<a href="<@routes.admin.locations />" class="btn btn-default">Cancel</a>
		</p>
	</form>
</#escape>