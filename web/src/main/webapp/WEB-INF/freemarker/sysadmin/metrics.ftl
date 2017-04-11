<#escape x as x?html>

<form action="" class="form-inline pull-right">
	<div class="input-group">
		<span class="input-group-addon">Start date</span>
		<input id="startDate" name="startDate" value="${startDate}" class="form-control"/>
		<span class="input-group-btn">
			<button type="submit" class="btn btn-primary">Update</button>
		</span>
	</div>
</form>

<h1 class="with-settings">Metrics for ${period}</h1>

<table class="table table-striped table-hover">
	<tbody>
		<tr>
			<th>Number of coursework submissions</th>
			<td>${submissions}</td>
		</tr>
		<tr>
			<th>Number of individual assignments submitted to</th>
			<td>${assignments}</td>
		</tr>
		<tr>
			<th>Number of feedback returned</th>
			<td>${feedback}</td>
		</tr>
		<tr>
			<th>Number of meeting records</th>
			<td>${meetings}</td>
		</tr>
		<tr>
			<th>Number of small group signups</th>
			<td>${groups}</td>
		</tr>
		<tr>
			<th>Number of attendance points recorded</th>
			<td>${checkpoints}</td>
		</tr>
	</tbody>
</table>

<script>
	jQuery('#startDate').datetimepicker({
		format: "dd-M-yyyy",
		weekStart: 1,
		minView: 'year',
		startView: 'year',
		autoclose: true,
		fontAwesome: true,
		bootcssVer: 3
	});
</script>

</#escape>