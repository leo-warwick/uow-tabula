<#escape x as x?html>
	<h1>Delete reusable small groups</h1>

	<#assign submitAction><@routes.groups.deletecrossmodulegroups smallGroupSet /></#assign>
	<@f.form method="post" action="${submitAction}" commandName="command">
		<h2>${smallGroupSet.name} (${smallGroupSet.academicYear.label})</h2>

		<!-- global errors -->
		<@f.errors cssClass="error" />

		<p>
			You can delete a set of reusable small groups if they've been created in error. You can't delete reusable groups if they've been
			linked to groups which have been released to tutors or students.
		</p>

		<@f.errors path="confirm" cssClass="error" />
		<@bs3form.checkbox path="confirm">
			<@f.checkbox path="confirm" id="confirmCheck" />
			<strong>I definitely will not need these groups again and wish to delete them entirely.</strong>
		</@bs3form.checkbox>

		<@bs3form.form_group>
			<input type="submit" value="Delete" class="btn btn-danger">
			<a href="<@routes.groups.crossmodulegroups department academicYear />" class="btn btn-default">Cancel</a>
		</@bs3form.form_group>
	</@f.form>

	<script type="text/javascript">
		jQuery(function($){
			$('#confirmCheck').change(function(){
				$('input[type=submit].btn-danger').attr('disabled', !this.checked).toggleClass('disabled', !this.checked);
			});
			$('input[type=submit].btn-danger').attr('disabled',true).addClass('disabled');
		})
	</script>

</#escape>